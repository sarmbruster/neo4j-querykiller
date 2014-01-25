package org.neo4j.extension.querykiller.helper

import groovy.util.logging.Slf4j
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.NotFoundException

import javax.servlet.*
import javax.servlet.http.HttpServletRequest

/**
 * simple filter initialized by {@link DelayLifecycle} to delay a request based on HTTP header X-Delay.
 * During delay a getNodeById(0) is called to give the guard a chance to fire
 * this is useful to fake long running queries
 */


@Slf4j
class DelayFilter implements Filter {

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
            if (delay != null) {

                def tx = graphDatabaseService.beginTx()
                try {
                    def finishTime = System.currentTimeMillis() + (delay as long)
                    log.debug "sleeping for $delay ms"

                    while (System.currentTimeMillis() < finishTime) {
                        try {
                            graphDatabaseService.getNodeById(0)
                        } catch (NotFoundException e) {
                            // pass
                        }
                        sleep 1
                        log.trace "waited 1ms"
                    }
                    log.debug "sleeping for $delay ms DONE"
                } finally {
                    tx.close()
                }
            }
        }
        chain.doFilter(request, response)
    }

    @Override
    void destroy() {
    }
}
