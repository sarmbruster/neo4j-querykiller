package org.neo4j.extension.querykiller.server

import com.google.common.eventbus.EventBus
import com.sun.jersey.api.client.UniformInterfaceException
import groovy.util.logging.Slf4j
import org.junit.ClassRule
import org.junit.rules.RuleChain
import org.neo4j.extension.querykiller.EventBusLifecycle
import org.neo4j.extension.querykiller.QueryRegistryExtension
import org.neo4j.extension.querykiller.agent.WrapNeo4jComponentsAgent
import org.neo4j.extension.querykiller.events.cypher.CypherContext
import org.neo4j.extension.querykiller.events.cypher.ResetCypherContext
import org.neo4j.extension.querykiller.events.query.QueryAbortedEvent
import org.neo4j.extension.querykiller.events.query.QueryRegisteredEvent
import org.neo4j.extension.querykiller.events.query.QueryUnregisteredEvent
import org.neo4j.extension.querykiller.events.transport.HttpContext
import org.neo4j.extension.querykiller.events.transport.ResetHttpContext
import org.neo4j.extension.querykiller.helper.AgentRule
import org.neo4j.extension.querykiller.helper.EventCounters
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.test.SuppressOutput
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.MediaType
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException

import static org.neo4j.extension.querykiller.helper.SpecHelper.*

@Slf4j
class QueryKillerRestSpec extends Specification {

    public static final String MOUNTPOINT = "querykiller"

    @Shared
    Neo4jServerResource neo4j = new Neo4jServerResource(
            config: [
                    "dbms.pagecache.memory": "1M"
            ],
            thirdPartyJaxRsPackages: [
                    "org.neo4j.extension.querykiller.server": "/$MOUNTPOINT",
            ]
    )

    @Shared
    @ClassRule
    RuleChain ruleChain = RuleChain.outerRule(new AgentRule(WrapNeo4jComponentsAgent))
            .around(SuppressOutput.suppressAll())   // comment this out for debugging
            .around(neo4j)

    @Shared
    EventBus eventBus
    EventCounters eventCounters

    def setupSpec() {
        def resolver = neo4j.server.getDatabase().getGraph().getDependencyResolver()

        eventBus = resolver.resolveDependency(EventBusLifecycle.class)
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
        log.error "setup: $specificationContext.currentFeature.name"
        eventCounters = new EventCounters()
        eventBus.register(eventCounters)

        def resolver = neo4j.server.getDatabase().getGraph().getDependencyResolver()
        QueryRegistryExtension queryRegistryExtension = resolver.resolveDependency(QueryRegistryExtension)
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

    def "cypher query is registered and unregistered"() {
        when:
        def json = [
                query: "MATCH (n) RETURN count(n) AS c",
        ]
        def response = neo4j.http.POST("db/data/cypher", json)

        then:
        response.status() == 200

        and:
        response.content().columns[0] == "c"
        response.content().data[0][0] == 0

        and:
        eventCounters.counters[QueryRegisteredEvent] == 1

        cleanup:
        sleepUntil { eventCounters.counters[QueryUnregisteredEvent] == 1}
    }

    def "procedure 'sleep' works as expected"() {

        when:
        def delay = 100
        def json = [
                query: "CALL org.neo4j.extension.querykiller.helper.sleep($delay)".toString(),
        ]

        def now = System.currentTimeMillis()
        def response = neo4j.http.POST("db/data/cypher", json)
        def duration = System.currentTimeMillis() - now

        then:
        response.status() == 200

        and:
        response.content().data.size() == 0

        and:
        duration > delay

        cleanup:
        sleepUntil { eventCounters.counters[ResetHttpContext] == 1}
    }

    @Unroll
    def "send #numberOfQueries queries in parallel with delay #delay [ms] and check if registry handles this correctly"() {

        setup:
        assert eventCounters.counters.every { it.value == 0 }
        def procedureStatement = "CALL org.neo4j.extension.querykiller.helper.sleep($delay)".toString()
        def threads =  (0..<numberOfQueries).collect {
            Thread.start {
                neo4j.http.POST("db/data/transaction/commit",
                        createJsonForTransactionalEndpoint([procedureStatement]))
                }
        }
        sleepUntil { eventCounters.counters[QueryRegisteredEvent.class] == numberOfQueries}

        when: "check query list"
        def response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() >= 0
        response.content().size() <= numberOfQueries

        when:
        sleepUntil { eventCounters.counters[QueryUnregisteredEvent.class] == numberOfQueries}

        response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size()==0

        cleanup:
        threads.each { it.join() }

        where:
        numberOfQueries | delay
        0               | 50
        1               | 50
        2               | 50
        4               | 50
        8               | 50
        100             | 50
        1000            | 50
    }

    /*
     * take a baseline measurement to be used for the query to interrupt
     * this should balance out different hardware speed
     */
    private long measureBaseline(def samples) {
        eventBus.unregister(eventCounters)
        "unwind range(0,1000) as x create (n:Person{id:x})".cypher()
        "unwind range(0,{samples}) as x match (n:Person {id:(x % 1000)}) return count(n) as c".cypher(samples: samples)[0].c
        def now = System.currentTimeMillis()
        def c = "unwind range(0,{samples}) as x match (n:Person {id:(x % 1000)}) return count(n) as c".cypher(samples: samples)[0].c
        neo4j.closeCypher()  // crucial! otherwise open tx will pollute query list
        eventBus.register(eventCounters)
        return System.currentTimeMillis()-now
    }

    def "send query with delay and terminate it"() {
        setup:
        long expectedRuntime = 3000 // if query would *not* be terminated it should run approx. this time duration
        long duration = measureBaseline(300)
        long samples = expectedRuntime * 300 / duration

        def procedureStatement = "unwind range(0,$samples) as x match (n:Person {id:(x % 1000)}) return count(n) as c".toString()

        Future future = Executors.newSingleThreadExecutor().submit({
            log.info("starting long running query")
            def now = System.currentTimeMillis()
            def r = neo4j.http.POST("db/data/transaction/commit",
                    createJsonForTransactionalEndpoint([procedureStatement]))
            return r.content()
        } as Callable )

        sleepUntil { eventCounters.counters[QueryRegisteredEvent] == 1 && eventCounters.counters[CypherContext] == 1 }
        sleep(200) // await query building

        when: "check query list"
        def response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)
        log.info("status called, got ${eventCounters.counters[QueryUnregisteredEvent.class]} ${response.status()}  ${response.content()}")

        then:
        response.status() == 200

        and: "query is still running in separate thread"
        eventCounters.counters[QueryUnregisteredEvent.class] == 0

        and:
        response.content().size() == 1
        response.content()[0].query == procedureStatement

        when:
        def key = response.content()[0].key
        log.info("before sending kill")
        response = neo4j.http.DELETE("$MOUNTPOINT/$key")
        log.info("sending kill $key ${response.status()}")

        then: "delete operation gives 200"
        response.status() == 200
        response.content().deleted == key

        when: "check query list again"
        log.info("query terminated")
        sleepUntil { (eventCounters.counters[QueryAbortedEvent.class] == 1) &&
             (eventCounters.counters[QueryUnregisteredEvent.class] == 1)
        }
        response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() == 0

//        and: "delay did not time out since we've aborted the query"
//        System.currentTimeMillis() - now < delay

        /*when: "deleting the same query again"
        neo4j.http.DELETE("$MOUNTPOINT/$key")

        then: "gives a 204"
        def e = thrown(UniformInterfaceException) // it's weird but a 204 is wrapped into an exception
        e.response.status == 204*/

        when:
        def fResponse = future.get()

        then:
        fResponse.errors.size() == 1
        fResponse.errors[0].code == "Neo.DatabaseError.Statement.ExecutionFailed"
        fResponse.errors[0].message == "The transaction has been terminated, no new operations in it are allowed. This normally happens because a client explicitly asks to terminate the transaction, for instance to stop a long-running operation. It may also happen because an operator has asked the database to be shut down, or because the current instance is about to perform a cluster role change. Simply retry your operation in a new transaction, and you should see a successful result."


        cleanup:

        sleepUntil {
            try {
                Neo4jUtils.assertNoOpenTransaction(neo4j.graphDatabaseService)
                true
            } catch (IllegalStateException ex) {
                false
            }
        }
    }

    def "queries on transactional endpoint are monitored"() {
        setup:

        def numberOfQueries = 1
        def threads =  (0..<numberOfQueries).collect {
            Thread.start {
                neo4j.http.POST("db/data/transaction/commit",
                        createJsonForTransactionalEndpoint([
                                "CREATE (n) return n",
                                "MATCH (n) RETURN count(n) AS c",
                                "CALL org.neo4j.extension.querykiller.helper.transactionAwareSleep(200)"
                        ]))
                }
        }

        try {
            sleepUntil { eventCounters.counters[CypherContext.class] == 3}
        } catch (TimeoutException e) {
            log.error "counters: $eventCounters.counters"
            throw e
        }

        when: "check query list"
        def response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() == 1
        response.content()[0].query == "CALL org.neo4j.extension.querykiller.helper.transactionAwareSleep(200)"
        response.content()[0].endPoint == "/db/data/transaction/commit"

        cleanup:
        threads.each { it.join() }
    }

    @Unroll
    def "should transactional endpoint work with transaction spawned over multiple requests"() {

        setup:
        assert eventCounters.counters.every { it.value == 0 }
        def thread

        when:
        def response = neo4j.http.POST(initialURL) //, createJsonForTransactionalEndpoint(["MATCH (n) RETURN count(n)"] ))
        log.info "done initial"

        then:
        response.status() == 201

        when:
        def location = response.header("Location")
        def url = location - neo4j.baseUrl
        thread = Thread.start {
            log.info "start 2nd (from different thread)"
            neo4j.http.POST(url + amend2ndRequest, createJsonForTransactionalEndpoint(["foreach (x in range(0,100000) | merge (n:Person{name:'Person'+x}))"] ))
            log.info "done 2nd (from different thread)"
        }

        sleepUntil { eventCounters.counters[QueryRegisteredEvent] == 2 }

        and: "check query list"
        response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == 1

        when:
        def key = response.content()[0].key
        response = neo4j.http.DELETE("$MOUNTPOINT/$key")

        then:
        response.status() == 200
        response.content().deleted == key

        cleanup:
        sleepUntil { eventCounters.counters[HttpContext] == eventCounters.counters[ResetHttpContext]  }
        log.info "done with spec"


        where:
        initialURL             | amend2ndRequest
        "db/data/transaction"  | ""
        "db/data/transaction/" | ""
        "db/data/transaction"  | "/"
        "db/data/transaction/" | "/"
        "db/data/transaction"  | "/commit"
        "db/data/transaction/" | "/commit"
    }

    def "should transactional endpoint work with payload upon first request"() {

        setup:
        def numberOfQueries = 1
        def threads = (0..<numberOfQueries).collect {
            Thread.start {
                neo4j.http.POST("db/data/transaction", createJsonForTransactionalEndpoint(["foreach (x in range(0,100000) | merge (n:Person{name:'Person'+x}))"]))
            }
        }
        sleepUntil { eventCounters.counters[QueryRegisteredEvent.class] == numberOfQueries }

        when: "check query list"
        def response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == 1

        when:
        def key = response.content()[0].key
        response = neo4j.http.DELETE("$MOUNTPOINT/$key")

        then:
        response.status() == 200
        response.content().deleted == key

        cleanup:
        threads.each{it.join(5000)}
    }

    def "should 'millis' time be accurate"() {
        setup:
        def numberOfQueries = 10
        def waitTime = 1000 // 1 sec
        def threads = (0..<numberOfQueries).collect {
            Thread.start {
                neo4j.http.POST("db/data/transaction", createJsonForTransactionalEndpoint(["foreach (x in range(0,100000) | merge (n:Person{name:'Person'+x}))"]))
            }
        }
        sleepUntil { eventCounters.counters[QueryRegisteredEvent.class] == numberOfQueries }
        sleep waitTime

        when:
        def response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == numberOfQueries

        and: "millis time is longer than wait time"
        response.content().every { it.millis > waitTime }

        when: "killing all queries"
        response.content().each {
            def key = it.key
            try {
                neo4j.http.DELETE("$MOUNTPOINT/$key")
            } catch (UniformInterfaceException e) {
                // pass
            }
        }
        sleepUntil { eventCounters.counters[QueryUnregisteredEvent.class] == numberOfQueries }

        and:
        response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == 0

        cleanup:
        threads.each {it.join(5000)}

    }

/*    Closure runCypherQueryViaLegacyEndpoint = { delay ->
        neo4j.http.withHeaders("X-Delay", delay as String).POST("db/data/cypher", [
                query: "MATCH (n) RETURN count(n) AS c",
        ])
    }

    Closure runCypherQueryViaTransactionalEndpoint = { delay, statements, params = null ->
        neo4j.http.withHeaders("X-Delay", delay as String).POST("db/data/transaction/commit", createJsonForTransactionalEndpoint(statements, params))
    }*/

    /**
     * create a collection structure fitting being suitable for json format used for
     * transactional endpoint
     * @param statements array holding cypher statements
     * @param params array holding parameter for statements
     * @return
     */
    def createJsonForTransactionalEndpoint(statements, params = null) {
        if (!params) {
            params = statements.collect { [:] }
        }
        def transposed = [statements, params].transpose()
        [
                statements: transposed.collect { [statement: it[0], params: it[1]] }
        ]
    }
}
