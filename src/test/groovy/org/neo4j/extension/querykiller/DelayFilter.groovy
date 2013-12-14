package org.neo4j.extension.querykiller

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.NotFoundException
import org.neo4j.server.logging.Logger

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

/**
 * simple filter initialized by {@link DelayLifecycle} to delay a request based on HTTP header X-Delay.
 * During delay a getNodeById(0) is called to give the guard a chance to fire
 * this is useful to fake long running queries
 */

class DelayFilter implements Filter {

    public static final Logger log = Logger.getLogger( DelayFilter.class );

    private GraphDatabaseService graphDatabaseService

    public DelayFilter(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService
    }

    @Override
    void init(FilterConfig filterConfig) throws ServletException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            def delay = ((HttpServletRequest)request).getHeader("X-Delay")
            def tx = graphDatabaseService.beginTx()
            try {
                if (delay != null) {
                    def finishTime = System.currentTimeMillis() + (delay as long)
                    log.warn "${Thread.currentThread()} sleeping for $delay ms"

                    while (System.currentTimeMillis() < finishTime) {
                        try {
                            graphDatabaseService.getNodeById(0)
                        } catch (NotFoundException e) {
                            // pass
                        }
                        sleep 1
                    }
                }
            } finally {
                tx.close()
            }
        }
        chain.doFilter(request, response)
    }

    @Override
    void destroy() {
    }
}
