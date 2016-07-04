package org.neo4j.extension.querykiller

import com.google.common.eventbus.EventBus
import groovy.util.logging.Slf4j
import org.junit.ClassRule
import org.junit.rules.RuleChain
import org.neo4j.extension.querykiller.agent.WrapNeo4jComponentsAgent
import org.neo4j.extension.querykiller.events.bind.BindTransactionEvent
import org.neo4j.extension.querykiller.events.bind.UnbindTransactionEvent
import org.neo4j.extension.querykiller.events.cypher.CypherContext
import org.neo4j.extension.querykiller.events.cypher.ResetCypherContext
import org.neo4j.extension.querykiller.events.query.QueryUnregisteredEvent
import org.neo4j.extension.querykiller.events.transport.HttpContext
import org.neo4j.extension.querykiller.events.transport.ResetHttpContext
import org.neo4j.extension.querykiller.helper.AgentRule
import org.neo4j.extension.querykiller.helper.EventCounters
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.graphdb.DependencyResolver
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.TimeoutException

import static org.neo4j.extension.querykiller.helper.SpecHelper.sleepUntil

//@Ignore
@Slf4j
class QueryTimeoutSpec extends Specification {

    public static final String MOUNTPOINT = "querykiller"

    @Shared
    Neo4jServerResource neo4j = new Neo4jServerResource(
            config: [
                    "querykiller.timeout": "500ms"
            ],
            thirdPartyJaxRsPackages: [
                    "org.neo4j.extension.querykiller.server": "/$MOUNTPOINT",
            ]
    )

    @Shared
    @ClassRule
    RuleChain ruleChain = RuleChain.outerRule(new AgentRule(WrapNeo4jComponentsAgent))
//            .around(SuppressOutput.suppressAll())   // comment this out for debugging
            .around(neo4j)

    @Shared
    EventBus eventBus

    @Shared
    DependencyResolver resolver

    @Shared
    QueryRegistryExtension queryRegistryExtension

    EventCounters eventCounters

    def setupSpec() {
        resolver = neo4j.server.getDatabase().getGraph().getDependencyResolver()
        eventBus = resolver.resolveDependency(EventBusLifecycle.class)
        queryRegistryExtension = resolver.resolveDependency(QueryRegistryExtension.class)
        // enable this for event debugging
/*
        eventBus.register(new Object() {
            @Subscribe
            public void printit(Object o) {
                log.error "event $o"
            }
        })
*/
    }

    def setup() {
        Neo4jUtils.assertNoOpenTransaction(neo4j.graphDatabaseService)
        log.error "setup: $specificationContext.currentFeature.name"
        eventCounters = new EventCounters()
        eventBus.register(eventCounters)

        assert queryRegistryExtension.transactionEntryMap.size() == 0
        queryRegistryExtension.startTerminationByTimeout(500);
    }

    def cleanup() {
//        eventBus.unregister(eventCounters)
        neo4j.closeCypher()
        try {
            sleepUntil { eventCounters.counters[HttpContext] == eventCounters.counters[ResetHttpContext]}
            sleepUntil { eventCounters.counters[CypherContext] == eventCounters.counters[ResetCypherContext]}
            sleepUntil { eventCounters.counters[BindTransactionEvent] == eventCounters.counters[UnbindTransactionEvent]}

        } catch (TimeoutException e) {
            log.error("timeoutException: $eventCounters.counters")
            throw e
        }

//        eventCounters.counters.each { k,v -> println "HURZ $k=$v"}
//        log.info "cleanup for $specificationContext.currentFeature.name "
//        Neo4jUtils.assertNoOpenTransaction(neo4j.graphDatabaseService)
        log.error "done: $specificationContext.currentFeature.name"
    }

    def "embedded query gets killed"() {
        when:

        def started = System.currentTimeMillis()
        def duration = 3000
        "CALL org.neo4j.extension.querykiller.helper.transactionAwareSleep({duration})".cypher(duration: duration)
        log.info "here we are"

        then:
        System.currentTimeMillis() - started < duration

        cleanup:
        sleepUntil { eventCounters.counters[QueryUnregisteredEvent] == 1}
    }

}
