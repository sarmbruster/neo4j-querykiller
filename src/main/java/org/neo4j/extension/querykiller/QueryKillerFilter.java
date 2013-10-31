package org.neo4j.extension.querykiller;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import org.neo4j.extension.querykiller.http.CopyHttpServletRequest;

/**
 * QueryKillerFilter registers a cypher statement with a {@QueryRegistry}
 * N.B.: it is crucial to have a {@TeeFilter} in the filterchain before since we're inspecting the payload of the request here.
 */
public class QueryKillerFilter implements Filter {

    private QueryRegistryExtension queryRegistryExtension;
    private ObjectMapper objectMapper;


    public QueryKillerFilter( QueryRegistryExtension queryRegistryExtension) {
        this.queryRegistryExtension = queryRegistryExtension;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        objectMapper = new ObjectMapper();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest copyRequest = new CopyHttpServletRequest((HttpServletRequest)request);

        JsonNode query = objectMapper.readTree( copyRequest.getReader() ).get( "query" );
        String queryKey = queryRegistryExtension.registerQuery(query.getTextValue());
        try {
            chain.doFilter(copyRequest, response);
        } finally {
            queryRegistryExtension.unregisterQuery(queryKey);
        }
    }

    @Override
    public void destroy() {
        // intentionally empty
    }
}
