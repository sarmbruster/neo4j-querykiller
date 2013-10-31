package org.neo4j.extension.querykiller;

import java.util.Collection;

import org.apache.commons.configuration.Configuration;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.server.AbstractNeoServer;
import org.neo4j.server.NeoServer;
import org.neo4j.server.plugins.Injectable;
import org.neo4j.server.plugins.SPIPluginLifecycle;

import static java.util.Collections.singletonList;

public class Lifecycle implements SPIPluginLifecycle {
    @Override
    public Collection<Injectable<?>> start(NeoServer neoServer) {

        Guard guard = neoServer.getDatabase().getGraph().getDependencyResolver().resolveDependency(Guard.class);
        final QueryRegistry queryRegistry = new QueryRegistry(guard);

        AbstractNeoServer abstractNeoServer = (AbstractNeoServer)neoServer;
        abstractNeoServer.getWebServer().addFilter(new QueryKillerFilter(queryRegistry), "/cypher"); // "/*" for catch all

        Collection result = singletonList(new Injectable<QueryRegistry>() {

            @Override
            public QueryRegistry getValue() {
                return queryRegistry;
            }

            @Override
            public Class<QueryRegistry> getType() {
                return QueryRegistry.class;
            }
        });
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
