package org.neo4j.extension.querykiller.server;

import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.extension.querykiller.filter.LegacyCypherQueryKillerFilter;
import org.neo4j.extension.querykiller.filter.TransactionalCypherQueryKillerFilter;
import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.server.NeoServer;
import org.neo4j.server.web.WebServer;

import java.util.Collections;

public class QueryKillerLifecycle extends DepenceyResolverAwareLifecycle {

    @Override
    protected Iterable<? extends Class> dependenciesToInject() {
        return Collections.singleton(QueryRegistryExtension.class);
    }

    @Override
    protected void start(NeoServer neoServer, DependencyResolver dependencyResolver, WebServer webServer) {
        final QueryRegistryExtension queryRegistryExtension = dependencyResolver.resolveDependency(QueryRegistryExtension.class);
        webServer.addFilter(new LegacyCypherQueryKillerFilter(queryRegistryExtension), "/cypher"); // "/*" for catch all
        TransactionalCypherQueryKillerFilter transactionalCypherQueryKillerFilter = new TransactionalCypherQueryKillerFilter(queryRegistryExtension);
        webServer.addFilter(transactionalCypherQueryKillerFilter, "/transaction/*"); // "/*" for catch all
        webServer.addFilter(transactionalCypherQueryKillerFilter, "/transaction"); // "/*" for catch all
    }

}
