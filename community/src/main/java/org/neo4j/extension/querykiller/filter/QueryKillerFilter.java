package org.neo4j.extension.querykiller.filter;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.extension.querykiller.QueryRegistryEntry;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.extension.querykiller.http.CopyHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * QueryKillerFilter registers a cypher statement with a {@QueryRegistryExtension}
 * N.B.: a {@link CopyHttpServletRequest} is used for processing the filterChain
 * base class for filtering requests dealing with cypher statements.
 * Parsing the cypher query from the request is delegated to a derived class.
 */
public abstract class QueryKillerFilter implements Filter {

    public final Logger log = LoggerFactory.getLogger(QueryKillerFilter.class);

    protected QueryRegistryExtension queryRegistryExtension;

    private ObjectMapper objectMapper;

    public QueryKillerFilter(QueryRegistryExtension queryRegistryExtension) {
        this.queryRegistryExtension = queryRegistryExtension;
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

            log.debug( "intercepting request");
            HttpServletRequest copyRequest = new CopyHttpServletRequest((HttpServletRequest)request);

            String cypher = extractCypherFromRequest( copyRequest );
            QueryRegistryEntry queryMapEntry = queryRegistryExtension.registerQuery(
                    cypher,
                    copyRequest.getPathInfo(),
                    copyRequest.getRemoteHost(),
                    copyRequest.getRemoteUser() );
            try {
                chain.doFilter(copyRequest, response);
            } finally {
                queryRegistryExtension.unregisterQuery(queryMapEntry);
                log.debug( "intercepting request DONE");
            }
        } else {
            chain.doFilter(request, response);
        }

    }

    private boolean shouldInterceptThisRequest(ServletRequest request) {
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

    protected abstract String extractCypherFromRequest(HttpServletRequest copyRequest) throws IOException;

    @Override
    public void destroy() {
        // intentionally empty
    }
}
