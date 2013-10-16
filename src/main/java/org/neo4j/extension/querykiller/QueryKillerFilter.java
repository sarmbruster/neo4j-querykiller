package org.neo4j.extension.querykiller;

import javax.servlet.*;
import java.io.IOException;

public class QueryKillerFilter implements Filter {

    private QueryRegistry queryRegistry;

    public QueryKillerFilter(QueryRegistry queryRegistry) {
        this.queryRegistry = queryRegistry;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // intentionally empty
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        String queryKey = queryRegistry.registerQuery(request.toString());
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
