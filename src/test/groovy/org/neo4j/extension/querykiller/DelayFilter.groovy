package org.neo4j.extension.querykiller

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

/**
 * simple filter initialized by {@link DelayLifecycle} to delay a request based on HTTP header X-Delay.
 * this is useful to fake long running queries
 */

class DelayFilter implements Filter {
    @Override
    void init(FilterConfig filterConfig) throws ServletException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response)
        if (request instanceof HttpServletRequest) {
            def delay = ((HttpServletRequest)request).getHeader("X-Delay")
            if (delay != null) {
                sleep delay as long
            }
        }
    }

    @Override
    void destroy() {
    }
}
