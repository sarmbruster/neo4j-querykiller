package org.neo4j.extension.querykiller;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * QueryKillerFilter registers a cypher statement with a {@QueryRegistry}
 * N.B.: it is crucial to have a {@TeeFilter} in the filterchain before since we're inspecting the payload of the request here.
 */
public class QueryKillerFilter implements Filter {

    private QueryRegistry queryRegistry;
    private ObjectMapper objectMapper;


    public QueryKillerFilter(QueryRegistry queryRegistry) {
        this.queryRegistry = queryRegistry;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        objectMapper = new ObjectMapper();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        JsonNode query = objectMapper.readTree( request.getReader() ).get( "query" );
        String queryKey = queryRegistry.registerQuery(query.getTextValue());
        try {
            chain.doFilter(request, response);
        } finally {
            queryRegistry.unregisterQuery(queryKey);
        }
    }

    @Override
    public void destroy() {
        // intentionally empty
    }
}
