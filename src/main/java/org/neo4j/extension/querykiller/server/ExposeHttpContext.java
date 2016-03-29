package org.neo4j.extension.querykiller.server;

import com.google.common.eventbus.EventBus;
import org.neo4j.extension.querykiller.events.transport.HttpContext;
import org.neo4j.extension.querykiller.events.transport.ResetHttpContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author Stefan Armbruster
 */
public class ExposeHttpContext implements Filter {

    private final EventBus eventBus;

    public ExposeHttpContext(EventBus eventBus) {

        this.eventBus = eventBus;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            String user = httpServletRequest.getRemoteUser();
            String host = httpServletRequest.getRemoteHost();
            String path = httpServletRequest.getRequestURI();
            String method = httpServletRequest.getMethod();
            eventBus.post(new HttpContext(path, user, host, method));
            chain.doFilter(request, response);
        } finally {
            eventBus.post(new ResetHttpContext());
        }
    }

    @Override
    public void destroy() {

    }
}
