package org.neo4j.extension.querykiller.statistics;

import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.helpers.Service;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.lifecycle.Lifecycle;

@Service.Implementation(KernelExtensionFactory.class)
public class QueryStatisticsExtensionFactory extends KernelExtensionFactory<QueryStatisticsExtensionFactory.Dependencies>
{

    public interface Dependencies
    {
        QueryRegistryExtension getQueryRegistryExtension();
    }

    public QueryStatisticsExtensionFactory() {
        super("queryStatistics");
    }

    @Override
    public Lifecycle newKernelExtension(final Dependencies dependencies) throws Throwable {
        return new QueryStatisticsExtension(dependencies.getQueryRegistryExtension());
    }

}

