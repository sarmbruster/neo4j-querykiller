package org.neo4j.extension.querykiller.events;

import org.neo4j.extension.querykiller.QueryRegistryEntry;

public class QueryUnregisteredEvent extends QueryEvent {
    public QueryUnregisteredEvent(QueryRegistryEntry queryRegistryEntry) {
        super(queryRegistryEntry);
    }
}
