package org.neo4j.extension.querykiller

import org.junit.ClassRule
import org.neo4j.extension.querykiller.events.QueryRegisteredEvent
import org.neo4j.extension.querykiller.helper.CounterObserver
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.jmx.JmxUtils
import spock.lang.Shared
import spock.lang.Specification

import javax.management.ObjectName

import static org.neo4j.extension.querykiller.helper.SpecHelper.*

class JMXSpec extends Specification {

    public static final String MOUNTPOINT = "statistics"

    @Shared
    @ClassRule Neo4jServerResource neo4j = new Neo4jServerResource(
            config: [ execution_guard_enabled: "true" ],
            thirdPartyJaxRsPackages: [
                    "org.neo4j.extension.querykiller.server": "/notrelevant",
                    "org.neo4j.extension.querykiller.statistics": "/$MOUNTPOINT"
            ]
    )

    Observable observable
    CounterObserver countObserver

    def setup() {
        observable = neo4j.server.getDatabase().getGraph().getDependencyResolver().resolveDependency(QueryRegistryExtension.class)
        countObserver = new CounterObserver()
        observable.addObserver(countObserver)
    }

    def cleanup() {
        observable.deleteObserver(countObserver)
    }


    def "query killer is available via JMX"() {
        when:

        Thread.start {
            def cypher = "MATCH (n) RETURN count(n) AS c"
            neo4j.http.POST("db/data/cypher", [query: cypher])
            neo4j.http.GET(MOUNTPOINT)
        }

        sleepUntil { countObserver.counters[QueryRegisteredEvent.class] == 1}

        ObjectName objectName = JmxUtils.getObjectName( neo4j.graphDatabaseService, "Queries" );
        int count = JmxUtils.getAttribute( objectName, "RunningQueriesCount" );

        then:
        count > 0
    }

}
