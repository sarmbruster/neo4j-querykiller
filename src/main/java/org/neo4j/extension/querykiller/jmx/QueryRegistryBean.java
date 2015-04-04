package org.neo4j.extension.querykiller.jmx;

import org.neo4j.extension.querykiller.QueryRegistryEntry;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.helpers.Service;
import org.neo4j.jmx.impl.ManagementBeanProvider;
import org.neo4j.jmx.impl.ManagementData;
import org.neo4j.jmx.impl.Neo4jMBean;
import org.neo4j.kernel.extension.KernelExtensions;

import javax.management.NotCompliantMBeanException;
import java.util.SortedSet;

/**
 * @author Stefan Armbruster
 */
@Service.Implementation(ManagementBeanProvider.class)
public class QueryRegistryBean extends ManagementBeanProvider {

    public QueryRegistryBean() {
        super(QueryRegistry.class);
    }

    @Override
    protected Neo4jMBean createMBean(ManagementData management) throws NotCompliantMBeanException {
        return new QueryRegistryImpl(management);
    }

    private static class QueryRegistryImpl extends Neo4jMBean implements QueryRegistry {

        private final QueryRegistryExtension queryRegistryExtension;

        protected QueryRegistryImpl(ManagementData management) throws NotCompliantMBeanException {
            super(management);
            queryRegistryExtension = management.resolveDependency(KernelExtensions.class).resolveDependency(QueryRegistryExtension.class);
        }

        @Override
        public int getRunningQueriesCount() {
            return queryRegistryExtension.getRunningQueries().size();
        }

        @Override
        public SortedSet<QueryRegistryEntry> getRunningQueries() {
            return queryRegistryExtension.getRunningQueries();
        }

        @Override
        public void terminate(String id) {
            queryRegistryExtension.abortQuery(id);
        }
    }
}
