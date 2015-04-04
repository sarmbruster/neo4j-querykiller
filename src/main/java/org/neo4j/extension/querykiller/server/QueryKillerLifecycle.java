package org.neo4j.extension.querykiller.server;

import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.extension.querykiller.filter.LegacyCypherQueryKillerFilter;
import org.neo4j.extension.querykiller.filter.TransactionalCypherQueryKillerFilter;
import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.NeoServer;
import org.neo4j.server.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class QueryKillerLifecycle extends DepenceyResolverAwareLifecycle {

    public final Logger log = LoggerFactory.getLogger(QueryKillerLifecycle.class);

    @Override
    protected Iterable<? extends Class> dependenciesToInject() {
        return Collections.singleton(QueryRegistryExtension.class);
    }

    @Override
    protected void start(NeoServer neoServer, DependencyResolver dependencyResolver, WebServer webServer) {
        log.info("registering filters");
        final QueryRegistryExtension queryRegistryExtension = dependencyResolver.resolveDependency(QueryRegistryExtension.class);
        final GraphDatabaseService graphDatabaseService = neoServer.getDatabase().getGraph();

        webServer.addFilter(new LegacyCypherQueryKillerFilter(queryRegistryExtension, graphDatabaseService), "/cypher"); // "/*" for catch all
        TransactionalCypherQueryKillerFilter transactionalCypherQueryKillerFilter = new TransactionalCypherQueryKillerFilter(queryRegistryExtension, graphDatabaseService);
        webServer.addFilter(transactionalCypherQueryKillerFilter, "/transaction/*"); // "/*" for catch all
        webServer.addFilter(transactionalCypherQueryKillerFilter, "/transaction"); // "/*" for catch all
    }

}