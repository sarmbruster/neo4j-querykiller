package org.neo4j.extension.querykiller;

import org.neo4j.helpers.Service;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.lifecycle.Lifecycle;

@Service.Implementation(KernelExtensionFactory.class)
public class QueryKillerExtensionFactory extends KernelExtensionFactory<QueryKillerExtensionFactory.Dependencies>
{

    public interface Dependencies
    {
        Guard getGuard();
    }

    public QueryKillerExtensionFactory() {
        super("queryKiller");
    }

    @Override
    public Lifecycle newKernelExtension(final Dependencies dependencies) throws Throwable {
        return new QueryKillerExtension(dependencies.getGuard());
    }

}

