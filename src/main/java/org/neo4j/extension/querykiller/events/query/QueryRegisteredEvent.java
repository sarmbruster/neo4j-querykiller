package org.neo4j.extension.querykiller.events.query;

import org.neo4j.extension.querykiller.TransactionEntry;

public class QueryRegisteredEvent extends QueryEvent {

    public QueryRegisteredEvent(TransactionEntry transactionEntry) {
        super(transactionEntry);
    }
}
