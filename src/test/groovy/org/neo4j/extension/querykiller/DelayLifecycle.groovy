package org.neo4j.extension.querykiller

import org.apache.commons.configuration.Configuration
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.server.AbstractNeoServer
import org.neo4j.server.NeoServer
import org.neo4j.server.plugins.Injectable
import org.neo4j.server.plugins.SPIPluginLifecycle

class DelayLifecycle implements SPIPluginLifecycle {
    @Override
    Collection<Injectable<?>> start(NeoServer neoServer) {
        AbstractNeoServer abstractNeoServer = (AbstractNeoServer)neoServer
        abstractNeoServer.getWebServer().addFilter(new DelayFilter(), "/cypher")
        return Collections.emptyList()
    }

    @Override
    Collection<Injectable<?>> start(GraphDatabaseService graphDatabaseService, Configuration config) {
        return Collections.emptyList()
    }

    @Override
    void stop() {
    }
}
