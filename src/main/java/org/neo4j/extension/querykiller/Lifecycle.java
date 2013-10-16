package org.neo4j.extension.querykiller;

import org.apache.commons.configuration.Configuration;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.server.AbstractNeoServer;
import org.neo4j.server.NeoServer;
import org.neo4j.server.plugins.Injectable;
import org.neo4j.server.plugins.SPIPluginLifecycle;
import org.neo4j.server.plugins.TypedInjectable;

import java.util.Collection;

import static java.util.Collections.*;

public class Lifecycle implements SPIPluginLifecycle {
    @Override
    public Collection<Injectable<?>> start(NeoServer neoServer) {


        Guard guard = neoServer.getDatabase().getGraph().getDependencyResolver().resolveDependency(Guard.class);
        QueryRegistry queryRegistry = new QueryRegistry(guard);

        AbstractNeoServer abstractNeoServer = (AbstractNeoServer)neoServer;
        abstractNeoServer.getWebServer().addFilter(new QueryKillerFilter(queryRegistry), "/cypher"); // "/*" for catch all


        Collection result = singletonList(TypedInjectable.injectable(queryRegistry));
        return result;
    }

    @Override
    public Collection<Injectable<?>> start(GraphDatabaseService graphDatabaseService, Configuration config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        // intentionally empty
    }
}
