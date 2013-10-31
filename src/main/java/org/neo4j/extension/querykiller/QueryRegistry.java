package org.neo4j.extension.querykiller;

import org.apache.commons.collections.list.TreeList;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.server.logging.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class QueryRegistry {

    public static final Logger log = Logger.getLogger( QueryRegistry.class );

    protected final ConcurrentHashMap<String, QueryRegistryEntry> runningQueries = new ConcurrentHashMap<String, QueryRegistryEntry>();
    protected final Guard guard;

    public QueryRegistry(Guard guard) {
        //this.sessionIdGenerator = sessionIdGenerator
        this.guard = guard;
    }

    public String registerQuery(String cypher) {
        VetoGuard vetoGuard = new VetoGuard();
        guard.start(vetoGuard);
        QueryRegistryEntry queryMapEntry = new QueryRegistryEntry(cypher, vetoGuard);
        String key = queryMapEntry.getKey();
        runningQueries.put(key, queryMapEntry);
        log.warn("registered query for key " + queryMapEntry.getKey());
        return key;
    }

    public QueryRegistryEntry unregisterQuery(String key) {
        guard.stop();
        log.warn("unregistered query for key " + key);
        return runningQueries.remove(key);
    }

    QueryRegistryEntry abortQuery(String key) {
        QueryRegistryEntry entry = runningQueries.get(key);
        if (entry==null) {
            throw new IllegalArgumentException("no query running with key " + key);
        }
        entry.getVetoGuard().setAbort(true);
        log.warn("aborted query for key " + key);

        return entry;
    }

    public List<QueryRegistryEntry> getRunningQueries() {
        return new TreeList(runningQueries.values());
    }

}
