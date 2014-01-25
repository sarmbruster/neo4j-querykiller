package org.neo4j.extension.querykiller.helper

import org.apache.commons.configuration.Configuration
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.server.AbstractNeoServer
import org.neo4j.server.NeoServer
import org.neo4j.server.plugins.Injectable
import org.neo4j.server.plugins.SPIPluginLifecycle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DelayLifecycle implements SPIPluginLifecycle {

    public final Logger log = LoggerFactory.getLogger(DelayLifecycle.class);

    @Override
    Collection<Injectable<?>> start(NeoServer neoServer) {
        log.info("registering filters");
        AbstractNeoServer abstractNeoServer = (AbstractNeoServer)neoServer

        def delayFilter = new DelayFilter(abstractNeoServer.database.graph)

        abstractNeoServer.getWebServer().addFilter(delayFilter, "/cypher")
        abstractNeoServer.getWebServer().addFilter(delayFilter, "/transaction/*")
        abstractNeoServer.getWebServer().addFilter(delayFilter, "/transaction")
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
