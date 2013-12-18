package org.neo4j.extension.querykiller.events;

import org.neo4j.extension.querykiller.QueryRegistryEntry;

public class QueryAbortedEvent extends QueryEvent {
    public QueryAbortedEvent(QueryRegistryEntry queryRegistryEntry) {
        super(queryRegistryEntry);
    }
}
