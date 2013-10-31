package org.neo4j.extension.querykiller;

import java.util.Collection;

import org.apache.commons.configuration.Configuration;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.AbstractNeoServer;
import org.neo4j.server.NeoServer;
import org.neo4j.server.plugins.Injectable;
import org.neo4j.server.plugins.SPIPluginLifecycle;

import static java.util.Collections.singletonList;

public class Lifecycle implements SPIPluginLifecycle {
    @Override
    public Collection<Injectable<?>> start(NeoServer neoServer) {

        final QueryRegistryExtension queryRegistryExtension = neoServer.getDatabase().getGraph().getDependencyResolver().resolveDependency(QueryRegistryExtension.class);

        AbstractNeoServer abstractNeoServer = (AbstractNeoServer)neoServer;
        abstractNeoServer.getWebServer().addFilter(new QueryKillerFilter(queryRegistryExtension), "/cypher"); // "/*" for catch all

        Collection result = singletonList(new Injectable<QueryRegistryExtension>() {

            @Override
            public QueryRegistryExtension getValue() {
                return queryRegistryExtension;
            }

            @Override
            public Class<QueryRegistryExtension> getType() {
                return QueryRegistryExtension.class;
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
