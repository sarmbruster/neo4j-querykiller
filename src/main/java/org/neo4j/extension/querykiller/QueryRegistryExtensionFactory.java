package org.neo4j.extension.querykiller;

import org.neo4j.helpers.Service;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;

@Service.Implementation(KernelExtensionFactory.class)
public class QueryRegistryExtensionFactory extends KernelExtensionFactory<QueryRegistryExtensionFactory.Dependencies>
{

    public interface Dependencies
    {
        EventBusLifecycle getEventBusLifecycle();
        Config getConfig();
    }

    public QueryRegistryExtensionFactory() {
        super("queryRegistry");
    }

    @Override
    public Lifecycle newInstance(KernelContext kernelContext, final Dependencies dependencies) throws Throwable {
        return new QueryRegistryExtension(dependencies);
    }

}

