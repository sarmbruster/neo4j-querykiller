package org.neo4j.extension.querykiller.server;

import org.apache.commons.configuration.Configuration;
import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.AbstractNeoServer;
import org.neo4j.server.NeoServer;
import org.neo4j.server.plugins.Injectable;
import org.neo4j.server.plugins.SPIPluginLifecycle;
import org.neo4j.server.web.WebServer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public abstract class DepenceyResolverAwareLifecycle implements SPIPluginLifecycle {

    @Override
    public final Collection<Injectable<?>> start(NeoServer neoServer) {

        final DependencyResolver dependencyResolver = neoServer.getDatabase().getGraph().getDependencyResolver();
        final WebServer webServer = ((AbstractNeoServer)neoServer).getWebServer();

        start(neoServer, dependencyResolver, webServer);

        Collection<Injectable<?>> result = new HashSet<>();

        for (Class toBeInjected: dependenciesToInject() ) {
            result.add(new InjectableAdapter<>(dependencyResolver.resolveDependency(toBeInjected)));
        }
        result.addAll(injectables(dependencyResolver));
        return result;
    }

    /**
     * additional injectables to be exposed to jersey
     * @param dependencyResolver
     * @return
     */
    protected Collection<? extends Injectable<?>> injectables(DependencyResolver dependencyResolver) {
        return Collections.EMPTY_SET;
    }

    /**
     * provide classes to be looked up with dependecyResolver and inject them
     * @return
     */
    protected abstract Iterable<? extends Class> dependenciesToInject();

    /**
     * placeholder for e.g. adding filters
     * @param neoServer
     * @param dependencyResolver
     * @param webServer
     */
    abstract protected void start(NeoServer neoServer, DependencyResolver dependencyResolver, WebServer webServer);

    @Override
    public Collection<Injectable<?>> start(GraphDatabaseService graphDatabaseService, Configuration config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        // intentionally empty
    }
}
