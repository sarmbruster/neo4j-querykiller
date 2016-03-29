package org.neo4j.extension.querykiller.events.bind;

import org.neo4j.kernel.api.KernelTransaction;

/**
 * @author Stefan Armbruster
 */
public class UnbindTransactionEvent extends TransactionEvent {

    public UnbindTransactionEvent(KernelTransaction kernelTransaction) {
        super(kernelTransaction);
    }

}
