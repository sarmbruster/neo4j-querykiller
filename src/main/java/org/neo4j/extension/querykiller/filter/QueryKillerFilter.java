package org.neo4j.extension.querykiller.filter;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.extension.querykiller.QueryRegistryEntry;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.extension.querykiller.http.CopyHttpServletRequest;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * QueryKillerFilter registers a cypher statement with a {@QueryRegistryExtension}
 * N.B.: a {@link CopyHttpServletRequest} is used for processing the filterChain
 * base class for filtering requests dealing with cypher statements.
 * Parsing the cypher query from the request is delegated to a derived class.
 */
public abstract class QueryKillerFilter implements Filter {

    public final Logger log = LoggerFactory.getLogger(QueryKillerFilter.class);

    final protected QueryRegistryExtension queryRegistryExtension;
    final protected GraphDatabaseService graphDatabaseService;

    private ObjectMapper objectMapper;

    public QueryKillerFilter(QueryRegistryExtension queryRegistryExtension, GraphDatabaseService graphDatabaseService) {
        this.queryRegistryExtension = queryRegistryExtension;
        this.graphDatabaseService = graphDatabaseService;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        objectMapper = new ObjectMapper();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (shouldInterceptThisRequest(request)) {

            String requestURI = ((HttpServletRequest) request).getRequestURI();
            log.debug("intercepting request " + requestURI);
            HttpServletRequest copyRequest = new CopyHttpServletRequest((HttpServletRequest)request);

            RequestType requestType = determineRequestType(copyRequest);

            QueryRegistryEntry queryMapEntry = null;
            String cypher = extractCypherFromRequest( copyRequest );
            Transaction tx = getTransaction(requestType, copyRequest);
            try {
                queryMapEntry = preProcess(requestType, copyRequest, cypher, tx);
                chain.doFilter(copyRequest, response);
                if (tx!=null) {
                    tx.success();
                }
            } catch (Exception e) {
                throw e;
            }

            finally {
                postProcess(requestType, copyRequest, cypher, tx, queryMapEntry, (HttpServletResponse) response);
                if (tx!=null) {
                    tx.close();
                }
                log.debug("intercepting request DONE");
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    protected RequestType determineRequestType(HttpServletRequest copyRequest) {
        return RequestType.ONE_SHOT;
    }

    protected Transaction getTransaction(RequestType requestType, HttpServletRequest request) {
        return graphDatabaseService.beginTx();
    }

    protected QueryRegistryEntry preProcess(RequestType requestType, HttpServletRequest request, String cypher, Transaction tx) {
        QueryRegistryEntry queryMapEntry = queryRegistryExtension.registerQuery(
                tx,
                cypher,
                request.getPathInfo(),
                request.getRemoteHost(),
                request.getRemoteUser());
        return queryMapEntry;
    }

    protected void postProcess(RequestType requestType, HttpServletRequest request, String cypher, Transaction tx, QueryRegistryEntry queryMapEntry, HttpServletResponse response) {
        queryRegistryExtension.unregisterQuery(queryMapEntry);
    }

    protected boolean shouldInterceptThisRequest(ServletRequest request) {
        return true;
/*
        HttpServletRequest hsr = (HttpServletRequest) request;
        String origin = hsr.getHeader("Origin");
        String referer = hsr.getHeader("Referer");
        if ((origin==null) || (referer==null)) {
            return false;
        } else {
            return referer.equals(origin+ "/browser/");
        }
*/
    }

    protected abstract String extractCypherFromRequest(HttpServletRequest copyRequest);

    @Override
    public void destroy() {
        // intentionally empty
    }

}
