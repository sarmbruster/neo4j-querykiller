package org.neo4j.extension.querykiller.shell;

import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.helpers.Service;
import org.neo4j.shell.App;
import org.neo4j.shell.AppCommandParser;
import org.neo4j.shell.Continuation;
import org.neo4j.shell.Output;
import org.neo4j.shell.Session;
import org.neo4j.shell.kernel.apps.ReadOnlyGraphDatabaseApp;

/**
 * shell extension for querykiller
 */
@Service.Implementation( App.class )
public class QueryKillerApp extends ReadOnlyGraphDatabaseApp
{
    @Override
    protected Continuation exec( AppCommandParser parser, Session session, Output out ) throws Exception
    {
        QueryRegistryExtension queryRegistryExtension = getQueryRegistryExtension();
        out.println( queryRegistryExtension.formatAsTable() );
        return Continuation.INPUT_COMPLETE;
    }

    private QueryRegistryExtension getQueryRegistryExtension()
    {
        QueryRegistryExtension queryRegistryExtension = getServer().getDb().getDependencyResolver().resolveDependency( QueryRegistryExtension.class );
        return queryRegistryExtension;
    }

    @Override
    public String getName()
    {
        return "query";
    }

    @Override
    public String getDescription()
    {
        return "description";
    }
}
