package org.neo4j.extension.querykiller.server;

import com.google.common.eventbus.EventBus;
import org.neo4j.extension.querykiller.EventBusLifecycle;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.server.NeoServer;
import org.neo4j.server.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * 2 responsibilities:
 * 1) expose QueryRegistryExtension as @Context for unmanaged extensions
 * 2) register a servlet filter to expose current context (endpoint, user, etc.)
 */
public class QueryKillerLifecycle extends DependencyResolverAwareLifecycle {

    public final Logger log = LoggerFactory.getLogger(QueryKillerLifecycle.class);

    @Override
    protected Iterable<? extends Class> dependenciesToInject() {
        return Collections.singleton(QueryRegistryExtension.class);
    }

    @Override
    protected void start(NeoServer neoServer, DependencyResolver dependencyResolver, WebServer webServer) {
        log.info("registering filters");
        final EventBus eventBus = dependencyResolver.resolveDependency(EventBusLifecycle.class);
        webServer.addFilter(new ExposeHttpContext(eventBus), "/*");
    }

}