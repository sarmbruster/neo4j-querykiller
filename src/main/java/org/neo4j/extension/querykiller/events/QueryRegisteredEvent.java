package org.neo4j.extension.querykiller.events;

import org.neo4j.extension.querykiller.QueryRegistryEntry;

public class QueryRegisteredEvent extends QueryEvent {

    public QueryRegisteredEvent(QueryRegistryEntry queryRegistryEntry) {
        super(queryRegistryEntry);
    }
}
