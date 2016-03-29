package org.neo4j.extension.querykiller.events.query;

import org.neo4j.extension.querykiller.TransactionEntry;

public class QueryUnregisteredEvent extends QueryEvent {

    private final String query;

    public QueryUnregisteredEvent(TransactionEntry transactionEntry, String query) {
        super(transactionEntry);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
