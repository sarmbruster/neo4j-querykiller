package org.neo4j.extension.querykiller.events;

import org.neo4j.extension.querykiller.QueryRegistryEntry;

public abstract class QueryEvent {

    private QueryRegistryEntry queryRegistryEntry;

    public QueryEvent(QueryRegistryEntry queryRegisteredEvent) {
        this.queryRegistryEntry = queryRegisteredEvent;
    }

    public QueryRegistryEntry getQueryRegistryEntry() {
        return queryRegistryEntry;
    }

}
