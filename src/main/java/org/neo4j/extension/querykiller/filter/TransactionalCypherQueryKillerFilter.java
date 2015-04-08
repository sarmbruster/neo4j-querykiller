package org.neo4j.extension.querykiller.filter;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.extension.querykiller.QueryRegistryEntry;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.extension.querykiller.http.CopyHttpServletRequest;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.rest.transactional.ExecutionResultSerializer;
import org.neo4j.server.rest.transactional.TransactionFacade;
import org.neo4j.server.rest.transactional.TransactionHandle;
import org.neo4j.server.rest.transactional.TransactionRegistry;
import org.neo4j.server.rest.transactional.error.Neo4jError;
import org.neo4j.server.rest.transactional.error.TransactionLifecycleException;
import org.neo4j.server.rest.web.TransactionUriScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static org.neo4j.extension.querykiller.filter.RequestType.*;

/**
 * parse cypher from request when transactional endpoint is used
 */
public class TransactionalCypherQueryKillerFilter implements Filter {

    public static final String URI_ONE_SHOT = "/db/data/transaction/commit";
    public static final Pattern URI_BEGIN = Pattern.compile("^/db/data/transaction/?");
    public static final Pattern URI_AMEND = Pattern.compile("^/db/data/transaction/(\\d+)/?$");
    public static final Pattern URI_COMMIT = Pattern.compile("^/db/data/transaction/(\\d+)/commit/?$");
    public final Logger log = LoggerFactory.getLogger(TransactionalCypherQueryKillerFilter.class);

    final protected QueryRegistryExtension queryRegistryExtension;
    final protected GraphDatabaseService graphDatabaseService;
    final protected TransactionRegistry transactionRegistry;
    final protected TransactionFacade transactionFacade;
    protected Field transactionHandleIdField;

    private ObjectMapper objectMapper;

    public TransactionalCypherQueryKillerFilter(QueryRegistryExtension queryRegistryExtension,
                                                GraphDatabaseService graphDatabaseService, TransactionRegistry transactionRegistry, TransactionFacade transactionFacade) {
        this.queryRegistryExtension = queryRegistryExtension;
        this.graphDatabaseService = graphDatabaseService;
        this.transactionRegistry = transactionRegistry;
        this.transactionFacade = transactionFacade;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        objectMapper = new ObjectMapper();
        try {
            transactionHandleIdField = TransactionHandle.class.getDeclaredField("id");
            transactionHandleIdField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        // intentionally empty
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        String requestURI = ((HttpServletRequest) request).getRequestURI();
        log.debug("intercepting request " + requestURI);
        HttpServletRequest copyRequest = new CopyHttpServletRequest((HttpServletRequest) request);

        RequestType requestType = determineRequestType(copyRequest);

        QueryRegistryEntry queryMapEntry = null;
        String cypher = extractCypherFromRequest(copyRequest);
        Transaction tx = getTransaction(requestType, copyRequest);
        try {
            queryMapEntry = preProcess(requestType, copyRequest, cypher, tx, response);

            if (!requestType.equals(BEGIN)) {
                chain.doFilter(copyRequest, response);
            }
            if (tx != null) {
                tx.success();
            }
//            } catch (Exception e) {
//                throw e;
        } finally {
            postProcess(requestType, copyRequest, cypher, tx, queryMapEntry, (HttpServletResponse) response);
            if (tx != null) {
                tx.close();
            }
            log.debug("intercepting request DONE");
        }
    }

    protected String extractCypherFromRequest(HttpServletRequest copyRequest) {
        try {
            JsonNode tree = getObjectMapper().readTree(copyRequest.getReader());
            JsonNode statements = tree.get("statements");
            List<JsonNode> vals = statements.findValues("statement");
            return vals.toString();
        } catch (EOFException e) {
            return "";  // in case body is empty
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected RequestType determineRequestType(HttpServletRequest copyRequest) {
        String requestURI = copyRequest.getRequestURI();
        if (requestURI.equals(URI_ONE_SHOT)) {
            return ONE_SHOT;
        } else if (URI_BEGIN.matcher(requestURI).matches()) {
            return BEGIN;
        } else if (URI_AMEND.matcher(requestURI).matches()) {
            return AMEND;
        } else if (URI_COMMIT.matcher(requestURI).matches()) {
            return COMMIT;
        } else {
            throw new IllegalStateException("SHOULD NOT HAPPEN");
        }
    }

    protected Transaction getTransaction(RequestType requestType, HttpServletRequest request) {
        return requestType == ONE_SHOT ? graphDatabaseService.beginTx() : null;
    }

    protected QueryRegistryEntry preProcess(RequestType requestType, HttpServletRequest request, String cypher, Transaction tx, ServletResponse response) {
        long txId;
        switch (requestType) {
            case ONE_SHOT:
                return queryRegistryExtension.registerQuery(tx, cypher, request.getPathInfo(), request.getRemoteHost(), request.getRemoteUser());

            case BEGIN:
                return null;

            case AMEND:
                txId = getTxIdFromRegex(URI_AMEND, request);
                return queryRegistryExtension.registerQuery(new TransactionalEndpointTransactionWrapper(transactionRegistry, txId),
                        cypher, request.getRequestURI(), request.getRemoteHost(), request.getRemoteUser());

            case COMMIT:
                txId = getTxIdFromRegex(URI_COMMIT, request);
                return queryRegistryExtension.registerQuery(new TransactionalEndpointTransactionWrapper(transactionRegistry, txId),
                        cypher, request.getRequestURI(), request.getRemoteHost(), request.getRemoteUser());

            case ROLLBACK:
                return null;

            default:
                throw new IllegalStateException("SHOULD NOT HAPPEN");
        }
    }

    protected long getTxIdFromRegex(Pattern pattern, HttpServletRequest request ) {
        Matcher matcher = pattern.matcher(request.getRequestURI());
        matcher.matches();
        return Long.parseLong(matcher.group(1));
    }

    protected void postProcess(RequestType requestType, final HttpServletRequest request, String cypher, Transaction tx, QueryRegistryEntry queryMapEntry, HttpServletResponse response) {
        switch (requestType) {
            case ONE_SHOT:
                queryRegistryExtension.unregisterQuery(queryMapEntry);
                break;

            case BEGIN:

                processBeginRequest(request, response, cypher);
                break;

            case AMEND:
                queryRegistryExtension.unregisterQuery(queryMapEntry);
                break;

            case COMMIT:
                queryRegistryExtension.unregisterQuery(queryMapEntry);
                break;

            case ROLLBACK:
                break;

            default:
                throw new IllegalStateException("SHOULD NOT HAPPEN");
        }
    }

    protected void processBeginRequest(final HttpServletRequest request, HttpServletResponse response, String cypher) {
        try {
            final URI baseUri = new URI(request.getRequestURL().toString());

            // here were basically copying the code from TransactionService#executeStatementsInNewTransaction
            // and register that one with our QueryRegistry
            QueryRegistryEntry queryRegistryEntry = null;
            try {
                TransactionHandle transactionHandle = transactionFacade.newTransactionHandle(new UriBuilder(request));

                queryRegistryEntry = queryRegistryExtension.registerQuery(new TransactionalEndpointTransactionWrapper(transactionRegistry, (Long) transactionHandleIdField.get(transactionHandle)), cypher, request.getRequestURI(), request.getRemoteHost(), request.getRemoteUser());

                StreamingOutput streamingResults = executeStatements(request.getInputStream(), transactionHandle, baseUri, request);
                streamingResults.write(response.getOutputStream());
                response.setStatus(201);
                response.setHeader("Location", transactionHandle.uri().toString());

            } catch (TransactionLifecycleException e) {
                StreamingOutput streamingResults = serializeError(e.toNeo4jError(), baseUri);
                streamingResults.write(response.getOutputStream());
                response.setStatus(404);
            } finally {
                if (queryRegistryEntry!=null) {
                    queryRegistryExtension.unregisterQuery(queryRegistryEntry);
                }
            }
        } catch (Exception e) {  // get rid of damn checked exceptions
            throw new RuntimeException(e);
        }
    }

    // copied from TransactionService

    private StreamingOutput serializeError( final Neo4jError neo4jError, final URI baseUri )
    {
        return new StreamingOutput()
        {
            @Override
            public void write( OutputStream output ) throws IOException, WebApplicationException
            {
                ExecutionResultSerializer serializer = transactionFacade.serializer( output, baseUri );
                serializer.errors( asList( neo4jError ) );
                serializer.finish();
            }
        };
    }

    private StreamingOutput executeStatements( final InputStream input, final TransactionHandle transactionHandle,
                                               final URI baseUri, final HttpServletRequest request )
    {
        return new StreamingOutput()
        {
            @Override
            public void write( OutputStream output ) throws IOException, WebApplicationException
            {
                transactionHandle.execute(transactionFacade.deserializer(input), transactionFacade.serializer(output, baseUri),
                        request);
            }
        };
    }

    private static class UriBuilder implements TransactionUriScheme {

        private final String requestURL;

        public UriBuilder(HttpServletRequest request) {
            this.requestURL = request.getRequestURL().toString();
        }

        @Override
        public URI txUri(long id) {
            return amendToURI(id);
        }

        @Override
        public URI txCommitUri(long id) {
            return amendToURI(id, "/commit");
        }

        private URI amendToURI(Object... objs) {
            try {
                StringBuilder sb = new StringBuilder(requestURL);
                if (sb.charAt(sb.length()-1) != '/') { // prevent duplication of "/"
                    sb.append("/");
                }
                for (Object o : objs) {
                    sb.append(o);
                }
                return new URI(sb.toString());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

    }
}

