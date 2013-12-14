package org.neo4j.extension.querykiller.shell;

import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.helpers.Service;
import org.neo4j.shell.App;
import org.neo4j.shell.AppCommandParser;
import org.neo4j.shell.Continuation;
import org.neo4j.shell.OptionDefinition;
import org.neo4j.shell.OptionValueType;
import org.neo4j.shell.Output;
import org.neo4j.shell.Session;
import org.neo4j.shell.kernel.apps.TransactionProvidingApp;

/**
 * shell extension for querykiller
 */
@Service.Implementation( App.class )
public class QueryKillerApp extends TransactionProvidingApp
{

    {
        addOptionDefinition( "k", new OptionDefinition( OptionValueType.MUST, "kills a query specified by the key" ));
    }

    @Override
    protected Continuation exec( AppCommandParser parser, Session session, Output out ) throws Exception
    {
        QueryRegistryExtension queryRegistryExtension = getQueryRegistryExtension();

        String key = parser.options().get( "k" );
        if ( (key != null) && (!key.isEmpty()) ) {

            queryRegistryExtension.abortQuery( key );
            out.println( "query for key " + key + " terminated." );
        } else {
            out.println( queryRegistryExtension.formatAsTable() );
        }
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
        return "Lists all currently running queries. Without any arguments a list of currently running queries is printed.";
    }
}
