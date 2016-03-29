package org.neo4j.extension.querykiller;

import org.neo4j.helpers.Service;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.extension.KernelExtensions;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.impl.query.QueryExecutionEngine;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;

@Service.Implementation(KernelExtensionFactory.class)
public class EventBusExtensionFactory extends KernelExtensionFactory<EventBusExtensionFactory.Dependencies>
{

    public interface Dependencies
    {
        ThreadToStatementContextBridge getThreadToStatementContextBridge();
        QueryExecutionEngine getQueryExecutionEngine();
        KernelExtensions getKernelExtensions();
    }

    public EventBusExtensionFactory() {
        super("eventBus");
    }

    @Override
    public Lifecycle newInstance(KernelContext kernelContext, final Dependencies dependencies) throws Throwable {
        return new EventBusLifecycle(dependencies);
    }

}

