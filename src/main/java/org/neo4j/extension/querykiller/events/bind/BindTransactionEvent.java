package org.neo4j.extension.querykiller.events.bind;

import org.neo4j.kernel.api.KernelTransaction;

/**
 * @author Stefan Armbruster
 */
public class BindTransactionEvent extends TransactionEvent {

    public BindTransactionEvent(KernelTransaction kernelTransaction) {
        super(kernelTransaction);
    }

}
