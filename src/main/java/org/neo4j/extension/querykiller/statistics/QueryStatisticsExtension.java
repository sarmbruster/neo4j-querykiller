package org.neo4j.extension.querykiller.statistics;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.extension.querykiller.TransactionEntry;
import org.neo4j.extension.querykiller.events.query.QueryUnregisteredEvent;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class QueryStatisticsExtension extends LifecycleAdapter
{
    public final Logger log = LoggerFactory.getLogger(QueryStatisticsExtension.class);

    protected final Map<String, QueryStat> statistics = new HashMap<>();

    private Config config;
    private EventBus eventBus;
    private QueryRegistryExtension queryRegistryExtension;
    private final QueryStatisticsExtensionFactory.Dependencies dependencies;
    public static final Setting<Boolean> STATISTICS_ENABLED_SETTING = Settings.setting("extension.statistics.enabled", Settings.BOOLEAN, Settings.FALSE);

    public QueryStatisticsExtension(QueryStatisticsExtensionFactory.Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, QueryStat> getStatistics() {
        return statistics;
    }

    public SortedMap<String, QueryStat> getSortedStatistics() {
        SortedMap<String, QueryStat> map = new TreeMap<>(new ValueComparator(statistics));
        map.putAll(statistics);
        return map;
    }

    @Subscribe
    public void handleQueryUnregisteredEvent(QueryUnregisteredEvent event) {
        TransactionEntry qre = event.getTransactionEntry();

        String query = event.getQuery();
        QueryStat queryStat = statistics.get(query);
        if (queryStat==null) {
            queryStat = new QueryStat();
            statistics.put(query, queryStat);
        }
        queryStat.add(qre.getStarted(), System.currentTimeMillis() - qre.getStarted().getTime());

    }

    @Override
    public void init() throws Throwable {
        config = dependencies.getConfig();
        queryRegistryExtension = dependencies.getQueryRegistryExtension();
        eventBus = dependencies.getEventBusLifecylce();
        if (config.get(STATISTICS_ENABLED_SETTING)) {
            eventBus.register(this);
        }
    }

    @Override
    public void stop() throws Throwable {
        statistics.clear();
    }

    @Override
    public void shutdown() throws Throwable {
        if (dependencies.getConfig().get(STATISTICS_ENABLED_SETTING)) {
            eventBus.unregister(this);
        }
    }

    public void clear() {
        statistics.clear();
    }
}
