package org.neo4j.extension.querykiller.statistics;

import org.neo4j.extension.querykiller.server.DepenceyResolverAwareLifecycle;
import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.server.NeoServer;
import org.neo4j.server.web.WebServer;

import java.util.Collections;

public class StatisticsLifecylce extends DepenceyResolverAwareLifecycle {

    @Override
    protected Iterable<? extends Class> dependenciesToInject() {
        return Collections.singleton(QueryStatisticsExtension.class);
    }

    @Override
    protected void start(NeoServer neoServer, DependencyResolver dependencyResolver, WebServer webServer) {
        // intentionally empty
    }
}
