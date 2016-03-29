package org.neo4j.extension.querykiller

import com.google.common.eventbus.EventBus
import org.junit.ClassRule
import org.junit.rules.RuleChain
import org.neo4j.extension.querykiller.agent.WrapNeo4jComponentsAgent
import org.neo4j.extension.querykiller.events.bind.BindTransactionEvent
import org.neo4j.extension.querykiller.events.bind.UnbindTransactionEvent
import org.neo4j.extension.querykiller.helper.AgentRule
import org.neo4j.extension.querykiller.helper.EventCounters
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.jmx.JmxUtils
import spock.lang.Shared
import spock.lang.Specification

import javax.management.ObjectName

import static org.neo4j.extension.querykiller.helper.SpecHelper.*

class JMXSpec extends Specification {

    public static final String MOUNTPOINT = "statistics"

    @Shared
    Neo4jServerResource neo4j = new Neo4jServerResource(
            thirdPartyJaxRsPackages: [
                    "org.neo4j.extension.querykiller.server": "/notrelevant",
                    "org.neo4j.extension.querykiller.statistics": "/$MOUNTPOINT"
            ]
    )

    @Shared
    @ClassRule
    RuleChain ruleChain = RuleChain.outerRule(new AgentRule(WrapNeo4jComponentsAgent)).around(neo4j)

    EventBus eventBus
    def eventCounters

    def setup() {
        eventBus = neo4j.server.getDatabase().getGraph().getDependencyResolver().resolveDependency(EventBusLifecycle.class)
        eventCounters = new EventCounters()
        eventBus.register(eventCounters)
    }

    def cleanup() {
        eventBus.unregister(eventCounters)
    }

    def "query killer is available via JMX"() {
        when:

        Thread.start {
            def cypher = "MATCH (n) RETURN count(n) AS c"
            neo4j.http.POST("db/data/cypher", [query: cypher])
            neo4j.http.GET(MOUNTPOINT)
        }

        sleepUntil { eventCounters.counters[BindTransactionEvent.class] == 1}

        ObjectName objectName = JmxUtils.getObjectName( neo4j.graphDatabaseService, "Queries" );
        int count = JmxUtils.getAttribute( objectName, "RunningQueriesCount" );

        then:
        count > 0

        cleanup:
        sleepUntil { eventCounters.counters[UnbindTransactionEvent.class] == 1}
    }
}
