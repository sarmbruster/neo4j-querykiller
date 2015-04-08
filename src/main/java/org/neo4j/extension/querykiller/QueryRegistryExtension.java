package org.neo4j.extension.querykiller;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import org.neo4j.extension.querykiller.events.QueryAbortedEvent;
import org.neo4j.extension.querykiller.events.QueryRegisteredEvent;
import org.neo4j.extension.querykiller.events.QueryUnregisteredEvent;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryRegistryExtension extends Observable implements Lifecycle
{
    public final Logger log = LoggerFactory.getLogger(QueryRegistryExtension.class);

    protected final SortedSet<QueryRegistryEntry> runningQueries = new ConcurrentSkipListSet<>();

    synchronized public QueryRegistryEntry registerQuery( Transaction tx, String cypher, String endPoint, String remoteHost, String remoteUser ) {
        QueryRegistryEntry queryRegistryEntry = new QueryRegistryEntry(tx, cypher, endPoint, remoteHost, remoteUser);
        runningQueries.add( queryRegistryEntry );
        log.debug("registered query for key " + queryRegistryEntry);
        forceNotifyObservers( new QueryRegisteredEvent( queryRegistryEntry ) );
        return queryRegistryEntry;
    }

    synchronized public void unregisterQuery( QueryRegistryEntry queryRegistryEntry) {
        log.debug("unregistered query for key " + queryRegistryEntry);
        if (runningQueries.remove(queryRegistryEntry)) {
            forceNotifyObservers(new QueryUnregisteredEvent(queryRegistryEntry));
        }
    }

    synchronized public QueryRegistryEntry abortQuery(String key) {
        QueryRegistryEntry entry = findQueryRegistryEntryForKey( key );
        entry.kill();
        log.warn("aborted query for key " + key);
        forceNotifyObservers(new QueryAbortedEvent(entry));
        unregisterQuery(entry);
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
        sb.append(      "+---------+----------+--------------------------------------------------------------+-----------------+-----------------+\n")
                .append("| time ms | key      | query                                                        | source          | endPoint        |\n");
        for (QueryRegistryEntry queryRegistryEntry : runningQueries) {
            sb.append(queryRegistryEntry.formatAsTable()).append("\n");
        }

        sb.append("+---------+----------+--------------------------------------------------------------+-----------------+-----------------+\n");
        return sb.toString();
    }
}
