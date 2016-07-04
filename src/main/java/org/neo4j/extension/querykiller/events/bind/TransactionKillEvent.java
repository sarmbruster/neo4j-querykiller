package org.neo4j.extension.querykiller.events.bind;

import org.neo4j.kernel.api.KernelTransaction;

/**
 * @author Stefan Armbruster
 */
public class TransactionKillEvent extends TransactionEvent {

    public TransactionKillEvent(KernelTransaction kernelTransaction) {
        super(kernelTransaction);
    }

}
