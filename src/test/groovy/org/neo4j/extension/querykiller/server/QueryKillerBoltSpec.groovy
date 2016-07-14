package org.neo4j.extension.querykiller.server

import com.google.common.eventbus.EventBus
import groovy.util.logging.Slf4j
import org.junit.ClassRule
import org.junit.rules.RuleChain
import org.neo4j.driver.v1.StatementResult
import org.neo4j.extension.querykiller.EventBusLifecycle
import org.neo4j.extension.querykiller.QueryRegistryExtension
import org.neo4j.extension.querykiller.agent.WrapNeo4jComponentsAgent
import org.neo4j.extension.querykiller.events.cypher.CypherContext
import org.neo4j.extension.querykiller.events.cypher.ResetCypherContext
import org.neo4j.extension.querykiller.events.query.QueryUnregisteredEvent
import org.neo4j.extension.querykiller.events.transport.BoltContext
import org.neo4j.extension.querykiller.events.transport.HttpContext
import org.neo4j.extension.querykiller.events.transport.ResetHttpContext
import org.neo4j.extension.querykiller.helper.AgentRule
import org.neo4j.extension.querykiller.helper.EventCounters
import org.neo4j.extension.spock.Neo4jBoltResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.graphdb.DependencyResolver
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.TimeoutException

import static org.neo4j.extension.querykiller.helper.SpecHelper.sleepUntil

@Slf4j
class QueryKillerBoltSpec extends Specification {

    @Shared
    Neo4jBoltResource neo4j = new Neo4jBoltResource(
//            config: [
//                    "dbms.pagecache.memory": "1M"
//            ],
//            thirdPartyJaxRsPackages: [
//                    "org.neo4j.extension.querykiller.server": "/$MOUNTPOINT",
//                    "org.neo4j.extension.querykiller.helper": "/$MOUNTPOINT",
//            ]
    )

    @Shared
    @ClassRule
    RuleChain ruleChain = RuleChain.outerRule(new AgentRule(WrapNeo4jComponentsAgent))
//                .around(SuppressOutput.suppressAll())   // comment this out for debugging
                .around(neo4j)

    @Shared
    EventBus eventBus

    @Shared
    DependencyResolver dependencyResolver

    EventCounters eventCounters

    def setupSpec() {
        dependencyResolver = neo4j.graphDatabaseService.getDependencyResolver()
        eventBus = dependencyResolver.resolveDependency(EventBusLifecycle)
    }

    def setup() {
        log.error "setup: $specificationContext.currentFeature.name"
        eventCounters = new EventCounters()
        eventBus.register(eventCounters)

        QueryRegistryExtension queryRegistryExtension = dependencyResolver.resolveDependency(QueryRegistryExtension)
        assert queryRegistryExtension.transactionEntries.size() == 0
    }

    def cleanup() {
        neo4j.closeCypher()
        eventBus.unregister(eventCounters)
        try {
            sleepUntil { eventCounters.counters[HttpContext] == eventCounters.counters[ResetHttpContext]}
            sleepUntil { eventCounters.counters[CypherContext] == eventCounters.counters[ResetCypherContext]}

        } catch (TimeoutException e) {
            log.error("timeoutException: $eventCounters.counters")
            throw e
        }
        log.info "cleanup for $specificationContext.currentFeature.name "
        Neo4jUtils.assertNoOpenTransaction(neo4j.graphDatabaseService)
        log.error "done: $specificationContext.currentFeature.name"
    }

    def "send bolt query"() {
        when:
        StatementResult result = neo4j.session.run("MATCH (n) RETURN count(n) AS c");

        then:
        result.single().get("c").asInt() == 0

        when:
        sleepUntil { eventCounters.counters[QueryUnregisteredEvent] == 2}

        then:
        eventCounters.counters[BoltContext] == 1

        cleanup:
        log.error "events: $eventCounters.counters"
    }
}
