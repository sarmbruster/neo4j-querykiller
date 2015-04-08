package org.neo4j.extension.querykiller.filter;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.extension.querykiller.QueryRegistryEntry;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.extension.querykiller.http.CopyHttpServletRequest;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * parses the cypher statement from a call to the legacy cypher endpoint
 */
public class LegacyCypherQueryKillerFilter implements Filter {

    public final Logger log = LoggerFactory.getLogger(LegacyCypherQueryKillerFilter.class);

    final protected QueryRegistryExtension queryRegistryExtension;
    final protected GraphDatabaseService graphDatabaseService;

    private ObjectMapper objectMapper;

    public LegacyCypherQueryKillerFilter(QueryRegistryExtension queryRegistryExtension, GraphDatabaseService graphDatabaseService) {
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

        String requestURI = ((HttpServletRequest) request).getRequestURI();
        log.debug("intercepting request " + requestURI);
        HttpServletRequest copyRequest = new CopyHttpServletRequest((HttpServletRequest) request);

        QueryRegistryEntry queryMapEntry = null;
        String cypher = extractCypherFromRequest(copyRequest);
        Transaction tx = graphDatabaseService.beginTx();
        try {
            queryMapEntry = queryRegistryExtension.registerQuery(
                    tx,
                    cypher,
                    copyRequest.getPathInfo(),
                    copyRequest.getRemoteHost(),
                    copyRequest.getRemoteUser());
            chain.doFilter(copyRequest, response);
            tx.success();
        } finally {
            queryRegistryExtension.unregisterQuery(queryMapEntry);
            tx.close();
            log.debug("intercepting request DONE");
        }
    }

    @Override
    public void destroy() {
        // intentionally empty
    }

    protected String extractCypherFromRequest(HttpServletRequest copyRequest) {
        try {
            return getObjectMapper().readTree(copyRequest.getReader()).get("query").getTextValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
