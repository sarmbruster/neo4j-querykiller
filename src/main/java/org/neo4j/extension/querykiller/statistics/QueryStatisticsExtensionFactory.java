package org.neo4j.extension.querykiller.statistics;

import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.helpers.Service;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.extension.KernelExtensions;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.lifecycle.Lifecycle;

@Service.Implementation(KernelExtensionFactory.class)
public class QueryStatisticsExtensionFactory extends KernelExtensionFactory<QueryStatisticsExtensionFactory.Dependencies>
{

    public interface Dependencies
    {
        KernelExtensions getKernelExtensions();

        // for a unknown reason it seems that dependencies on other kernel extension do no longer work
//        QueryRegistryExtension getQueryRegistryExtension();
    }

    public QueryStatisticsExtensionFactory() {
        super("queryStatistics");
    }

    @Override
    public Lifecycle newKernelExtension(final Dependencies dependencies) throws Throwable {
        KernelExtensions kernelExtensions = dependencies.getKernelExtensions();
        return new QueryStatisticsExtension(kernelExtensions.resolveDependency(QueryRegistryExtension.class));

        // old version for Neo4j <= 2.1.x
//        return new QueryStatisticsExtension(dependencies.getQueryRegistryExtension());
    }

}

