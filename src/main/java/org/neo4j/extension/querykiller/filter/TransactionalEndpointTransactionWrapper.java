package org.neo4j.extension.querykiller.filter;

import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.rest.transactional.TransactionRegistry;
import org.neo4j.server.rest.transactional.error.TransactionLifecycleException;

/**
 * @author Stefan Armbruster
 */
public class TransactionalEndpointTransactionWrapper implements Transaction {


    private final TransactionRegistry transactionRegistry;
    private final long txId;

    public TransactionalEndpointTransactionWrapper(TransactionRegistry transactionRegistry, long txId) {

        this.transactionRegistry = transactionRegistry;
        this.txId = txId;
    }

    @Override
    public void terminate() {
        try {
            transactionRegistry.terminate(txId);
        } catch (TransactionLifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void failure() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void success() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void finish() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock acquireWriteLock(PropertyContainer entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock acquireReadLock(PropertyContainer entity) {
        throw new UnsupportedOperationException();
    }
}
