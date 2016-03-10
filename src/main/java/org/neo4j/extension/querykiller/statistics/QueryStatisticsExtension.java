package org.neo4j.extension.querykiller.statistics;

import org.neo4j.extension.querykiller.QueryRegistryEntry;
import org.neo4j.extension.querykiller.events.QueryUnregisteredEvent;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class QueryStatisticsExtension implements Lifecycle, Observer
{
    public final Logger log = LoggerFactory.getLogger(QueryStatisticsExtension.class);

    protected final Map<String, QueryStat> statistics = new HashMap<>();

    private final Observable observable;
    private final Config config;
    public static final Setting<Boolean> STATISTICS_ENABLED_SETTING = Settings.setting("extension.statistics.enabled", Settings.BOOLEAN, Settings.TRUE);

    public QueryStatisticsExtension(Observable observable, Config config)
    {
        this.observable = observable;
        this.config = config;
    }

    public Map<String, QueryStat> getStatistics() {
        return statistics;
    }

    public SortedMap<String, QueryStat> getSortedStatistics() {
        SortedMap<String, QueryStat> map = new TreeMap<>(new ValueComparator(statistics));
        map.putAll(statistics);
        return map;
    }

    @Override
    public void update(Observable observable, Object o) {
        if (o instanceof QueryUnregisteredEvent) {

            QueryRegistryEntry qre = ((QueryUnregisteredEvent)o).getQueryRegistryEntry();

            QueryStat queryStat = statistics.get(qre.getCypher());
            if (queryStat==null) {
                queryStat = new QueryStat();
                statistics.put(qre.getCypher(), queryStat);
            }
            queryStat.add(qre.getStarted(), System.currentTimeMillis() - qre.getStarted().getTime());
        }
    }

    @Override
    public void init() throws Throwable {
        if (config.get(STATISTICS_ENABLED_SETTING)) {
            observable.addObserver(this);
        }
    }

    @Override
    public void start() throws Throwable {

    }

    @Override
    public void stop() throws Throwable {
        statistics.clear();
    }

    @Override
    public void shutdown() throws Throwable {
        if (config.get(STATISTICS_ENABLED_SETTING)) {
            observable.deleteObserver(this);
        }
    }

    public void clear() {
        statistics.clear();
    }
}
