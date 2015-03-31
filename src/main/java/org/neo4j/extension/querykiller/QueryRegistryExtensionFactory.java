package org.neo4j.extension.querykiller;

import org.neo4j.helpers.Service;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.lifecycle.Lifecycle;

@Service.Implementation(KernelExtensionFactory.class)
public class QueryRegistryExtensionFactory extends KernelExtensionFactory<QueryRegistryExtensionFactory.Dependencies>
{

    public interface Dependencies
    {
        // intentionally empty
    }

    public QueryRegistryExtensionFactory() {
        super("queryRegistry");
    }

    @Override
    public Lifecycle newKernelExtension(final Dependencies dependencies) throws Throwable {
        return new QueryRegistryExtension();
    }

}

