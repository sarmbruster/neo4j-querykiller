package org.neo4j.extension.querykiller.server

import com.sun.jersey.api.client.UniformInterfaceException
import groovy.util.logging.Slf4j
import org.junit.ClassRule
import org.neo4j.extension.querykiller.QueryRegistryExtension
import org.neo4j.extension.querykiller.events.QueryAbortedEvent
import org.neo4j.extension.querykiller.events.QueryRegisteredEvent
import org.neo4j.extension.querykiller.events.QueryUnregisteredEvent
import org.neo4j.extension.querykiller.helper.CounterObserver
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.test.Mute
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.MediaType
import java.util.concurrent.TimeoutException

import static org.neo4j.extension.querykiller.helper.SpecHelper.*

@Slf4j
class QueryKillerRestSpec extends Specification {

    public static final String MOUNTPOINT = "querykiller"

    @Shared
    @ClassRule
    Mute mute = Mute.muteAll()

    @Shared
    @ClassRule Neo4jServerResource neo4j = new Neo4jServerResource(
            config: [
                    cache_type: "none",
                    "dbms.pagecache.memory": "1M"

            ],
            thirdPartyJaxRsPackages: [
                    "org.neo4j.extension.querykiller.server": "/$MOUNTPOINT",
                    "org.neo4j.extension.querykiller.helper": "/$MOUNTPOINT",
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

    def "send cypher query"() {
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

        cleanup:
        sleepUntil { countObserver.counters[QueryUnregisteredEvent.class] == 1}
    }

    def "send cypher query with delay"() {

        when:
        def json = [
                query: "MATCH (n) RETURN count(n) AS c",
        ]
        def delay = 100

        def now = System.currentTimeMillis()
        def response = neo4j.http
                .withHeaders("X-Delay", delay as String)
                .POST("db/data/cypher", json)
        def duration = System.currentTimeMillis() - now

        then:
        response.status() == 200

        and:
        response.content().columns[0] == "c"
        response.content().data[0][0] == 0

        and:
        duration > delay

        cleanup:
        sleepUntil { countObserver.counters[QueryUnregisteredEvent.class] == 1}

    }

    @Unroll
    def "send #numberOfQueries queries in parallel with delay #delay [ms] and check if registry handles this correctly"() {

        setup:
        log.error "running with $numberOfQueries"
        assert countObserver.counters.every { it.value == 0 }
        def threads =  (0..<numberOfQueries).collect { Thread.start runCypherQueryViaLegacyEndpoint.curry(delay) }
        sleepUntil { countObserver.counters[QueryRegisteredEvent.class] == numberOfQueries}

        when: "check query list"
        def response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() >= 0
        response.content().size() <= numberOfQueries

        when:
        sleepUntil { countObserver.counters[QueryUnregisteredEvent.class] == numberOfQueries}

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

    def "send query with delay and terminate it"() {
        setup:
        assert countObserver.counters.every { it.value == 0 }
        def now = System.currentTimeMillis()
        def delay = 5000
        def threads =  (0..<1).collect { Thread.start runCypherQueryViaLegacyEndpoint.curry(delay) }
        sleepUntil { countObserver.counters[QueryRegisteredEvent.class] == 1}

        when: "check query list"
        def response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        countObserver.counters[QueryUnregisteredEvent.class] == 0

        and:
        response.content().size() == 1
        response.content()[0].cypher == "MATCH (n) RETURN count(n) AS c"

        when:
        def key = response.content()[0].key
        response = neo4j.http.DELETE("$MOUNTPOINT/$key")

        then: "delete operation gives 200"
        response.status() == 200
        response.content().deleted == key

        when: "check query list again"
        sleepUntil { (countObserver.counters[QueryAbortedEvent.class] == 1) &&
             (countObserver.counters[QueryUnregisteredEvent.class] == 1)
        }
        response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() == 0

        and: "delay did not time out since we've aborted the query"
        System.currentTimeMillis() - now < delay

        when: "deleting the same query again"
        neo4j.http.DELETE("$MOUNTPOINT/$key")

        then: "gives a 204"
        def e = thrown(UniformInterfaceException) // it's weird but a 204 is wrapped into an exception
        e.response.status == 204

        cleanup:
        threads.each { it.join() }
    }

    def "queries on transactional endpoint are monitored"() {
        setup:

        def numberOfQueries = 1
        def threads =  (0..<numberOfQueries).collect { Thread.start runCypherQueryViaTransactionalEndpoint.curry(20, ["CREATE (n) return n", "MATCH (n) RETURN count(n) AS c"]) }

        try {
            sleepUntil { countObserver.counters[QueryRegisteredEvent.class] == numberOfQueries}
        } catch (TimeoutException e) {
            log.error "counters: $countObserver.counters"
            throw e
        }


        when: "check query list"
        def response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() == 1
        response.content()[0].cypher == '["CREATE (n) return n", "MATCH (n) RETURN count(n) AS c"]'

        cleanup:
        threads.each { it.join() }
    }

    @Unroll
    def "should transactional endpoint work with transaction spawned over multiple requests"() {

        setup:
        assert countObserver.counters.every { it.value == 0 }
        def thread

        when:
        def response = neo4j.http.POST(initialURL) //, createJsonForTransactionalEndpoint(["MATCH (n) RETURN count(n)"] ))

        then:
        response.status() == 201

        when:
        def location = response.header("Location")
        def url = location - neo4j.baseUrl
        thread = Thread.start { neo4j.http.POST(url + amend2ndRequest, createJsonForTransactionalEndpoint(["foreach (x in range(0,100000) | merge (n:Person{name:'Person'+x}))"] )) }

        sleepUntil { countObserver.counters[QueryRegisteredEvent.class] == 2 }

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
        sleepUntil { countObserver.counters[QueryRegisteredEvent.class] == numberOfQueries }

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

    def "should 'since' time be accurate"() {
        setup:
        def numberOfQueries = 10
        def waitTime = 1000 // 1 sec
        def threads = (0..<numberOfQueries).collect {
            Thread.start {
                neo4j.http.POST("db/data/transaction", createJsonForTransactionalEndpoint(["foreach (x in range(0,100000) | merge (n:Person{name:'Person'+x}))"]))
            }
        }
        sleepUntil { countObserver.counters[QueryRegisteredEvent.class] == numberOfQueries }
        sleep waitTime

        when:
        def response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == numberOfQueries

        and: "since time is longer than wait time"
        response.content().every { it.since > waitTime }

        when: "killing all queries"
        response.content().each {
            def key = it.key
            try {
                neo4j.http.DELETE("$MOUNTPOINT/$key")
            } catch (UniformInterfaceException e) {
                // pass
            }
        }
        sleepUntil { countObserver.counters[QueryUnregisteredEvent.class] == numberOfQueries }

        and:
        response = neo4j.http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == 0

        cleanup:
        threads.each {it.join()}

    }

    Closure runCypherQueryViaLegacyEndpoint = { delay ->
        neo4j.http.withHeaders("X-Delay", delay as String).POST("db/data/cypher", [
                query: "MATCH (n) RETURN count(n) AS c",
        ])
    }

    Closure runCypherQueryViaTransactionalEndpoint = { delay, statements, params = null ->
        neo4j.http.withHeaders("X-Delay", delay as String).POST("db/data/transaction/commit", createJsonForTransactionalEndpoint(statements, params))
    }

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
