package org.neo4j.extension.querykiller;

import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.lifecycle.Lifecycle;

/**
 * trivial Neo4j kernel extension exposing a {@link QueryRegistry}
 */
public class QueryKillerExtension implements Lifecycle
{

    private QueryRegistry queryRegistry;

    public QueryKillerExtension( Guard guard )
    {
        this.queryRegistry = new QueryRegistry( guard );
    }

    public QueryRegistry getQueryRegistry()
    {
        return queryRegistry;
    }

    @Override
    public void init() throws Throwable
    {
    }

    @Override
    public void start() throws Throwable
    {
    }

    @Override
    public void stop() throws Throwable
    {
    }

    @Override
    public void shutdown() throws Throwable
    {
    }
}
