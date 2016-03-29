package org.neo4j.extension.querykiller.events.bind;

import org.neo4j.kernel.api.KernelTransaction;

/**
 * @author Stefan Armbruster
 */
public abstract class TransactionEvent {

    private final KernelTransaction kernelTransaction;

    public TransactionEvent(KernelTransaction kernelTransaction) {
        this.kernelTransaction = kernelTransaction;
    }

    public KernelTransaction getKernelTransaction() {
        return kernelTransaction;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' +
                "kernelTransaction=" + kernelTransaction +
                '}';
    }
}
