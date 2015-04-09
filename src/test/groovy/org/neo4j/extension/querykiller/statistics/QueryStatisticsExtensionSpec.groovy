package org.neo4j.extension.querykiller.statistics

import org.neo4j.extension.querykiller.events.QueryUnregisteredEvent
import org.neo4j.helpers.collection.MapUtil
import org.neo4j.kernel.configuration.Config
import spock.lang.Specification

class QueryStatisticsExtensionSpec extends Specification {

    def "should query statistics be empty on fresh instance"() {

        setup:
        def qse = new QueryStatisticsExtension(null, new Config())

        expect:
        qse.statistics.size() == 0
    }

    def "sending a QueryUnregisteredEvent should update statistics"() {

        setup:
        Observable observable = new Observable();
        def qse = new QueryStatisticsExtension(observable, new Config())
        qse.init()

        when:
        def cypher = "MATCH (n) RETURN n"
        def entry = new org.neo4j.extension.querykiller.QueryRegistryEntry(null, cypher, null, null,  null)
        observable.setChanged()
        observable.notifyObservers(new QueryUnregisteredEvent(entry))

        then:
        qse.statistics.size() == 1
        qse.statistics[cypher].durations.size() == 1
        qse.statistics[cypher].durations.keySet()[0] == entry.started
        qse.statistics[cypher].total == qse.statistics[cypher].durations.values().sum()

        when: "adding same query again"
        cypher = "MATCH (n) RETURN n"
        entry = new org.neo4j.extension.querykiller.QueryRegistryEntry(null, cypher, null, null,  null)
        observable.setChanged()
        observable.notifyObservers(new QueryUnregisteredEvent(entry))

        then:
        qse.statistics.size() == 1
        qse.statistics[cypher].durations.size() == 2
        qse.statistics[cypher].durations.keySet()[1] == entry.started
        qse.statistics[cypher].total == qse.statistics[cypher].durations.values().sum()

        when: "adding another query"
        cypher = "MATCH (n) RETURN count(n)"
        entry = new org.neo4j.extension.querykiller.QueryRegistryEntry(null, cypher, null, null,  null)
        observable.setChanged()
        observable.notifyObservers(new QueryUnregisteredEvent(entry))

        then:
        qse.statistics.size() == 2
        qse.statistics[cypher].durations.size() == 1
        qse.statistics[cypher].durations.keySet()[0] == entry.started
        qse.statistics[cypher].total == qse.statistics[cypher].durations.values().sum()

    }

    def "should statistics be disabled by config"() {
        setup:
        Observable observable = new Observable();
        def qse = new QueryStatisticsExtension(observable, new Config(MapUtil.stringMap("extension.statistics.enabled", "false")))
        qse.init()

        when:
        def cypher = "MATCH (n) RETURN n"
        def entry = new org.neo4j.extension.querykiller.QueryRegistryEntry(null, cypher, null, null,  null)
        observable.setChanged()
        observable.notifyObservers(new QueryUnregisteredEvent(entry))

        then:
        qse.statistics.size() == 0
    }

}
