package org.neo4j.extension.querykiller;

import java.util.Observable;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.neo4j.extension.querykiller.events.QueryAbortedEvent;
import org.neo4j.extension.querykiller.events.QueryEvent;
import org.neo4j.extension.querykiller.events.QueryRegisteredEvent;
import org.neo4j.extension.querykiller.events.QueryUnregisteredEvent;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.server.logging.Logger;

public class QueryRegistryExtension extends Observable implements Lifecycle
{
    public final Logger log = Logger.getLogger( QueryRegistryExtension.class );

    protected final ConcurrentSkipListSet<QueryRegistryEntry> runningQueries = new ConcurrentSkipListSet<>();
    protected final Guard guard;

    public QueryRegistryExtension( Guard guard )
    {
        this.guard = guard;
    }

    public QueryRegistryEntry registerQuery( String cypher, String endPoint, String remoteHost, String remoteUser ) {

        VetoGuard vetoGuard = new VetoGuard();
        guard.start(vetoGuard);

        QueryRegistryEntry queryRegistryEntry = new QueryRegistryEntry(vetoGuard, cypher, endPoint, remoteHost, remoteUser);
        runningQueries.add( queryRegistryEntry );
        log.warn("registered query for key " + queryRegistryEntry);
        forceNotifyObservers( new QueryRegisteredEvent( queryRegistryEntry ) );
        return queryRegistryEntry;
    }

    public void unregisterQuery( QueryRegistryEntry queryRegistryEntry) {
        guard.stop();
        log.warn("unregistered query for key " + queryRegistryEntry);
        forceNotifyObservers(new QueryUnregisteredEvent(queryRegistryEntry));
        runningQueries.remove(queryRegistryEntry);
    }

    public QueryRegistryEntry abortQuery(String key) {
        QueryRegistryEntry entry = findQueryRegistryEntryForKey( key );
        entry.getVetoGuard().setAbort(true);
        log.warn("aborted query for key " + key);
        forceNotifyObservers(new QueryAbortedEvent(entry));
        return entry;
    }

    private void forceNotifyObservers( Object event )
    {
        setChanged();
        notifyObservers( event );
    }

    private QueryRegistryEntry findQueryRegistryEntryForKey( String key )
    {
        for (QueryRegistryEntry entry: runningQueries) {
            if (key.equals( entry.getKey() )) {
                return entry;
            }
        }
        throw new IllegalArgumentException("no query running with key " + key);
    }

    public SortedSet<QueryRegistryEntry> getRunningQueries() {
        return runningQueries;
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

    public String formatAsTable()
    {
        StringBuilder sb = new StringBuilder(  );
        sb.append(      "+---------+------------+--------------------------------------------------------------+-----------------+-----------------+\n")
                .append("| time ms | key        | query                                                        | source          | endPoint        |\n");
        for (QueryRegistryEntry queryRegistryEntry : runningQueries) {
            sb.append(queryRegistryEntry.formatAsTable()).append("\n");
        }

        sb.append("+---------+------------+--------------------------------------------------------------+-----------------+-----------------+\n");
        return sb.toString();
    }
}
