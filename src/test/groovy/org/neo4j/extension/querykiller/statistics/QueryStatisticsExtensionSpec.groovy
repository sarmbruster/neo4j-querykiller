package org.neo4j.extension.querykiller.statistics

import org.neo4j.extension.querykiller.EventBusLifecycle
import org.neo4j.extension.querykiller.QueryRegistryExtension
import org.neo4j.extension.querykiller.TransactionEntry
import org.neo4j.extension.querykiller.events.query.QueryUnregisteredEvent
import org.neo4j.helpers.collection.MapUtil
import org.neo4j.kernel.api.KernelTransaction
import org.neo4j.kernel.configuration.Config
import spock.lang.Specification

class QueryStatisticsExtensionSpec extends Specification {

    def "should query statistics be empty on fresh instance"() {

        setup:
        def qse = new QueryStatisticsExtension(
                [getConfig: { new Config()},
                 getEventBusLifecylce: {null} ]
                        as QueryStatisticsExtensionFactory.Dependencies)
        expect:
        qse.statistics.size() == 0
    }

    def "sending a QueryUnregisteredEvent should update statistics"() {

        setup:
        def eventBusLifecycle = new EventBusLifecycle(null)
        def queryRegistryExtension = new QueryRegistryExtension()
        def qse = new QueryStatisticsExtension(
                [
                        getConfig: { new Config()},
                        getEventBusLifecylce: { eventBusLifecycle },
                        getQueryRegistryExtension: { queryRegistryExtension }
                ] as QueryStatisticsExtensionFactory.Dependencies)
        qse.init()

        when:
        def cypher = "MATCH (n) RETURN n"
        def entry = new TransactionEntry(Mock(KernelTransaction))
        eventBusLifecycle.post(new QueryUnregisteredEvent(entry, cypher))

        then:
        qse.statistics.size() == 1
        qse.statistics[cypher].durations.size() == 1
        qse.statistics[cypher].durations.keySet()[0] == entry.started
        qse.statistics[cypher].total == qse.statistics[cypher].durations.values().sum()

        when: "adding same query again"
        cypher = "MATCH (n) RETURN n"
        entry = new TransactionEntry(Mock(KernelTransaction))
        eventBusLifecycle.post(new QueryUnregisteredEvent(entry, cypher))

        then:
        qse.statistics.size() == 1
        qse.statistics[cypher].durations.size() == 2
        qse.statistics[cypher].durations.keySet()[1] == entry.started
        qse.statistics[cypher].total == qse.statistics[cypher].durations.values().sum()

        when: "adding another query"
        cypher = "MATCH (n) RETURN count(n)"
        entry = new TransactionEntry(Mock(KernelTransaction))
        eventBusLifecycle.post(new QueryUnregisteredEvent(entry, cypher))

        then:
        qse.statistics.size() == 2
        qse.statistics[cypher].durations.size() == 1
        qse.statistics[cypher].durations.keySet()[0] == entry.started
        qse.statistics[cypher].total == qse.statistics[cypher].durations.values().sum()

    }

    def "should statistics be disabled by config"() {
        setup:
        def eventBusLifecycle = new EventBusLifecycle(null)
        def queryRegistryExtension = new QueryRegistryExtension()

        def qse = new QueryStatisticsExtension([
            getConfig: { new Config(MapUtil.stringMap("extension.statistics.enabled", "false"))},
            getEventBusLifecylce: {  eventBusLifecycle },
            getQueryRegistryExtension: { queryRegistryExtension }
        ] as QueryStatisticsExtensionFactory.Dependencies)
        qse.init()

        when:
        def cypher = "MATCH (n) RETURN n"
        def entry = new TransactionEntry(Mock(KernelTransaction))
        eventBusLifecycle.post(new QueryUnregisteredEvent(entry, cypher))

        then:
        qse.statistics.size() == 0
    }

}
