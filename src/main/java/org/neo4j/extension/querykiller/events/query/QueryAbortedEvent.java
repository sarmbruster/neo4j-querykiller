package org.neo4j.extension.querykiller.events.query;

import org.neo4j.extension.querykiller.TransactionEntry;

public class QueryAbortedEvent extends QueryEvent {
    public QueryAbortedEvent(TransactionEntry transactionEntry) {
        super(transactionEntry);
    }
}
