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
 * QueryKillerFilter registers a cypher statement with a {@QueryRegistryExtension}
 * N.B.: a {@link CopyHttpServletRequest} is used for processing the filterChain
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
        }
    }

    private String extractCypherFromRequest( HttpServletRequest copyRequest ) throws IOException
    {
        return objectMapper.readTree( copyRequest.getReader() ).get( "query" ).getTextValue();
    }

    @Override
    public void destroy() {
        // intentionally empty
    }
}
