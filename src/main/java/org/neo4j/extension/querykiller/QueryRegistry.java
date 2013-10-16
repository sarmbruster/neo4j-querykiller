package org.neo4j.extension.querykiller;

import org.neo4j.kernel.guard.Guard;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

class QueryRegistry {

    protected final ConcurrentHashMap<String, QueryMapEntry> runningQueries = new ConcurrentHashMap<String, QueryMapEntry>();
    protected final Guard guard;

    public QueryRegistry(Guard guard) {
        //this.sessionIdGenerator = sessionIdGenerator
        this.guard = guard;
    }

    public String registerQuery(String cypher) {
        VetoGuard vetoGuard = new VetoGuard();
        guard.start(vetoGuard);
        QueryMapEntry queryMapEntry = new QueryMapEntry(cypher, vetoGuard);
        String key = queryMapEntry.calculateKey();
        runningQueries.put(key, queryMapEntry);
        return key;
    }

    public QueryMapEntry unregisterQuery(String key) {
        guard.stop();
        return runningQueries.remove(key);
    }

    QueryMapEntry abortQuery(String key) {
        QueryMapEntry entry = runningQueries.get(key);
        if (entry==null) {
            throw new IllegalArgumentException("no query running with key key " + key);
        }
        entry.getVetoGuard().setAbort(true);
        //log.info "aborted query ($key)"

        return entry;
    }

    static class QueryMapEntry {
        String cypher;
        Date started = new Date();
        Thread thread = Thread.currentThread();
        VetoGuard vetoGuard;

        QueryMapEntry(String cypher, VetoGuard vetoGuard) {
            this.cypher = cypher;
            this.vetoGuard = vetoGuard;
        }

        String getCypher() {
            return cypher;
        }

        void setCypher(String cypher) {
            this.cypher = cypher;
        }

        Date getStarted() {
            return started;
        }

        void setStarted(Date started) {
            this.started = started;
        }

        Thread getThread() {
            return thread;
        }

        void setThread(Thread thread) {
            this.thread = thread;
        }

        VetoGuard getVetoGuard() {
            return vetoGuard;
        }

        void setVetoGuard(VetoGuard vetoGuard) {
            this.vetoGuard = vetoGuard;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof QueryMapEntry)) return false;

            QueryMapEntry that = (QueryMapEntry) o;

            if (!cypher.equals(that.cypher)) return false;
            if (!started.equals(that.started)) return false;
            if (!thread.equals(that.thread)) return false;
            if (!vetoGuard.equals(that.vetoGuard)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = cypher.hashCode();
            result = 31 * result + started.hashCode();
            result = 31 * result + thread.hashCode();
            result = 31 * result + vetoGuard.hashCode();
            return result;
        }

        public String calculateKey() {
            // TODO: find better key, e.g. md5
            StringBuilder sb = new StringBuilder(thread.getName()).append("_").append(started.getTime());
            return sb.toString();
        }
    }
}
