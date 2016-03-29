package org.neo4j.extension.querykiller.events.query;

import org.neo4j.extension.querykiller.TransactionEntry;

public abstract class QueryEvent {

    private TransactionEntry transactionEntry;

    public QueryEvent(TransactionEntry queryRegisteredEvent) {
        this.transactionEntry = queryRegisteredEvent;
    }

    public TransactionEntry getTransactionEntry() {
        return transactionEntry;
    }

}
