package org.neo4j.extension.querykiller.filter;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.neo4j.extension.querykiller.QueryRegistryEntry;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.rest.transactional.TransactionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.neo4j.extension.querykiller.filter.RequestType.*;

/**
 * parse cypher from request when transactional endpoint is used
 */
public class TransactionalCypherQueryKillerFilter extends QueryKillerFilter {

    public static final String URI_ONE_SHOT = "/db/data/transaction/commit";
    public static final Pattern URI_BEGIN = Pattern.compile("^/db/data/transaction/?");
    public static final Pattern URI_AMEND = Pattern.compile("^/db/data/transaction/(\\d+)$");
    public static final Pattern URI_COMMIT = Pattern.compile("^/db/data/transaction/(\\d+)/commit$");
    public final Logger log = LoggerFactory.getLogger(TransactionalCypherQueryKillerFilter.class);

    protected final TransactionRegistry transactionRegistry;

    public TransactionalCypherQueryKillerFilter(QueryRegistryExtension queryRegistryExtension,
                                                GraphDatabaseService graphDatabaseService, TransactionRegistry transactionRegistry) {
        super(queryRegistryExtension, graphDatabaseService);
        this.transactionRegistry = transactionRegistry;
    }

    @Override
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

    @Override
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

    @Override
    protected Transaction getTransaction(RequestType requestType, HttpServletRequest request) {
        switch (requestType) {
            case ONE_SHOT:
                return super.getTransaction(requestType, request);

            case BEGIN:
                return null;

            case AMEND:
                return null;

            case COMMIT:
                return null;

            case ROLLBACK:
                return null;

            default:
                throw new IllegalStateException("SHOULD NOT HAPPEN");
        }
    }

    @Override
    protected QueryRegistryEntry preProcess(RequestType requestType, HttpServletRequest request, String cypher, Transaction tx) {
        long txId;
        Matcher matcher;
        switch (requestType) {
            case ONE_SHOT:
                return super.preProcess(requestType, request, cypher, tx);

            case BEGIN:
                return null;

            case AMEND:
                matcher = URI_AMEND.matcher(request.getRequestURI());
                matcher.matches();
                txId = Long.parseLong(matcher.group(1));
                return queryRegistryExtension.registerQuery(new TransactionalEndpointTransactionWrapper(transactionRegistry, txId),
                        cypher, request.getRequestURI(), request.getRemoteHost(), request.getRemoteUser());

            case COMMIT:
                matcher = URI_COMMIT.matcher(request.getRequestURI());
                matcher.matches();
                txId = Long.parseLong(matcher.group(1));
                return queryRegistryExtension.registerQuery(new TransactionalEndpointTransactionWrapper(transactionRegistry, txId),
                        cypher, request.getRequestURI(), request.getRemoteHost(), request.getRemoteUser());

            case ROLLBACK:
                return null;

            default:
                throw new IllegalStateException("SHOULD NOT HAPPEN");
        }
    }

    @Override
    protected void postProcess(RequestType requestType, HttpServletRequest request, String cypher, Transaction tx, QueryRegistryEntry queryMapEntry, HttpServletResponse response) {
        switch (requestType) {
            case ONE_SHOT:
                super.postProcess(requestType, request, cypher, tx, queryMapEntry, response);
                break;

            case BEGIN:
                break;

            case AMEND:
                super.postProcess(requestType, request, cypher, tx, queryMapEntry, response);
                break;

            case COMMIT:
                super.postProcess(requestType, request, cypher, tx, queryMapEntry, response);
                break;

            case ROLLBACK:
                break;

            default:
                throw new IllegalStateException("SHOULD NOT HAPPEN");
        }
    }
}

