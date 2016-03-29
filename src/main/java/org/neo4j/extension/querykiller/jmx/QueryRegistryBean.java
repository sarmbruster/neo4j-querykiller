package org.neo4j.extension.querykiller.jmx;

import org.apache.commons.beanutils.BeanUtils;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.extension.querykiller.TransactionEntry;
import org.neo4j.extension.querykiller.statistics.QueryStat;
import org.neo4j.extension.querykiller.statistics.QueryStatisticsExtension;
import org.neo4j.helpers.Service;
import org.neo4j.jmx.impl.ManagementBeanProvider;
import org.neo4j.jmx.impl.ManagementData;
import org.neo4j.jmx.impl.Neo4jMBean;
import org.neo4j.kernel.extension.KernelExtensions;

import javax.management.NotCompliantMBeanException;
import java.util.*;

/**
 * @author Stefan Armbruster
 */
@Service.Implementation(ManagementBeanProvider.class)
public class QueryRegistryBean extends ManagementBeanProvider {

    public QueryRegistryBean() {
        super(QueriesMBean.class);
    }

    @Override
    protected Neo4jMBean createMBean(ManagementData management) throws NotCompliantMBeanException {
        return new QueriesMBeanImpl(management);
    }

    private static class QueriesMBeanImpl extends Neo4jMBean implements QueriesMBean {

        private QueryRegistryExtension queryRegistryExtension;
        private QueryStatisticsExtension statisticsExtension;

        protected QueriesMBeanImpl(ManagementData management) throws NotCompliantMBeanException {
            super(management);
            KernelExtensions kernelExtensions = management.resolveDependency(KernelExtensions.class);
            queryRegistryExtension = kernelExtensions.resolveDependency(QueryRegistryExtension.class);
            statisticsExtension = kernelExtensions.resolveDependency(QueryStatisticsExtension.class);
        }

        @Override
        public int getRunningQueriesCount() {
            return queryRegistryExtension.getTransactionEntryMap().size();
        }

        @Override
        public Collection<Map<String, Object>> getRunningQueries() {
            Collection<Map<String,Object>> retVal = new ArrayList<>();
            for (TransactionEntry entry: queryRegistryExtension.getTransactionEntryMap()) {
                try {
                    retVal.add(BeanUtils.describe(entry));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return retVal;
        }

        @Override
        public void terminate(String id) {
            queryRegistryExtension.abortQuery(id);
        }

        @Override
        public Map<String, Map<String, Object>> getStatistics() {
            Map<String, Map<String,Object>> retVal = new LinkedHashMap<>();

            for (Map.Entry<String, QueryStat> entry : statisticsExtension.getSortedStatistics().entrySet()) {
                try {
                    retVal.put(entry.getKey(), BeanUtils.describe(entry.getValue()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return retVal;
        }

        @Override
        public void clearStatistics() {
            statisticsExtension.clear();
        }

    }
}
