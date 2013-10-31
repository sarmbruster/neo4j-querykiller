package org.neo4j.extension.querykiller;

import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.server.logging.Logger;

public class QueryRegistryExtension implements Lifecycle
{
    public static final Logger log = Logger.getLogger( QueryRegistryExtension.class );

    protected final ConcurrentSkipListSet<QueryRegistryEntry> runningQueries = new ConcurrentSkipListSet<>();
    protected final Guard guard;

    public QueryRegistryExtension( Guard guard )
    {
        this.guard = guard;
    }

    public QueryRegistryEntry registerQuery( String cypher, String endPoint, String remoteHost, String remoteUser ) {

        VetoGuard vetoGuard = new VetoGuard();
        guard.start(vetoGuard);

        QueryRegistryEntry queryMapEntry = new QueryRegistryEntry(vetoGuard, cypher, endPoint, remoteHost, remoteUser);
        runningQueries.add( queryMapEntry );
        log.warn("registered query for key " + queryMapEntry);
        return queryMapEntry;
    }

    public void unregisterQuery( QueryRegistryEntry queryMapEntry) {
        guard.stop();
        log.warn("unregistered query for key " + queryMapEntry);
        runningQueries.remove(queryMapEntry);
    }

    QueryRegistryEntry abortQuery(String key) {
        QueryRegistryEntry entry = findQueryRegistryEntryForKey( key );
        entry.getVetoGuard().setAbort(true);
        log.warn("aborted query for key " + key);

        return entry;
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
}
