package org.neo4j.extension.querykiller.statistics;

import org.neo4j.extension.querykiller.EventBusLifecycle;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.helpers.Service;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;

@Service.Implementation(KernelExtensionFactory.class)
public class QueryStatisticsExtensionFactory extends KernelExtensionFactory<QueryStatisticsExtensionFactory.Dependencies>
{

    public interface Dependencies
    {
        Config getConfig();
        EventBusLifecycle getEventBusLifecylce();
        QueryRegistryExtension getQueryRegistryExtension();
    }

    public QueryStatisticsExtensionFactory() {
        super("queryStatistics");
    }

    @Override
    public Lifecycle newInstance(KernelContext context, final Dependencies dependencies) throws Throwable {
//        KernelExtensions kernelExtensions = dependencies.getKernelExtensions();
//        EventBusLifecylce eventBusLifecylce = kernelExtensions.resolveDependency(EventBusLifecylce.class);
//        return new QueryStatisticsExtension(eventBusLifecylce.getEventBus(), dependencies.getConfig());
        return new QueryStatisticsExtension(dependencies);

        // old version for Neo4j <= 2.1.x
//        return new QueryStatisticsExtension(dependencies.getQueryRegistryExtension());
    }

}

