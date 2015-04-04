package org.neo4j.extension.querykiller.server

import com.sun.jersey.api.client.UniformInterfaceException
import groovy.util.logging.Slf4j
import org.junit.ClassRule
import org.neo4j.extension.querykiller.QueryRegistryExtension
import org.neo4j.extension.querykiller.events.QueryAbortedEvent
import org.neo4j.extension.querykiller.events.QueryRegisteredEvent
import org.neo4j.extension.querykiller.events.QueryUnregisteredEvent
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.test.Mute
import org.neo4j.test.server.HTTP
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.MediaType
import java.util.concurrent.TimeoutException

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

    // TODO: refactor to Neo4jServerResponse.http when 0.2 is released
    def getHttp() {
        HTTP.withBaseUri(neo4j.baseUrl)
    }

    def "send cypher query"() {
        when:
        def json = [
                query: "MATCH (n) RETURN count(n) AS c",
        ]
        def response = http.POST("db/data/cypher", json)

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
        def response = http
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
        def response = http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() >= 0
        response.content().size() <= numberOfQueries

        when:
        sleepUntil { countObserver.counters[QueryUnregisteredEvent.class] == numberOfQueries}

        response = http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

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
        def response = http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        countObserver.counters[QueryUnregisteredEvent.class] == 0

        and:
        response.content().size() == 1
        response.content()[0].cypher == "MATCH (n) RETURN count(n) AS c"

        when:
        def key = response.content()[0].key
        http.DELETE("$MOUNTPOINT/$key")

        then: "delete operation returned 204"
        def e = thrown(UniformInterfaceException) // it's weird but a 204 is wrapped into an exception
        e.response.status == 204

        when: "check query list again"
        sleepUntil { (countObserver.counters[QueryAbortedEvent.class] == 1) &&
             (countObserver.counters[QueryUnregisteredEvent.class] == 1)
        }
        response = http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() == 0

        and: "delay did not time out since we've aborted the query"
        System.currentTimeMillis() - now < delay

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
        def response = http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() == 1
        response.content()[0].cypher == '["CREATE (n) return n", "MATCH (n) RETURN count(n) AS c"]'

        cleanup:
        threads.each { it.join() }
    }

    Closure runCypherQueryViaLegacyEndpoint = { delay ->
         http.withHeaders("X-Delay", delay as String).POST("db/data/cypher", [
                 query: "MATCH (n) RETURN count(n) AS c",
         ])
     }

     Closure runCypherQueryViaTransactionalEndpoint = { delay, statements, params=null ->
         http.withHeaders("X-Delay", delay as String).POST("db/data/transaction/commit", createJsonForTransactionalEndpoint(statements, params))
     }

     /**
      * create a collection structure fitting being suitable for json format used for
      * transactional endpoint
      * @param statements array holding cypher statements
      * @param params array holding parameter for statements
      * @return
      */
     def createJsonForTransactionalEndpoint( statements,  params) {
         if (!params) {
             params = statements.collect {[:]}
         }
         def transposed = [statements, params].transpose()
         [
                 statements: transposed.collect { [statement: it[0], params: it[1]] }
         ]
     }

    private void sleepUntil(Closure closure) {
        long started = System.currentTimeMillis()
        while (closure.call() == false) {
            sleep 5;
            if ((System.currentTimeMillis()-started) > 10*1000) {
                log.error("timeout in sleepUntil")
                throw new TimeoutException("timeout in sleepUntil")
            }
        }
    }

    @Slf4j
    class CounterObserver implements Observer {
        def counters = [:].withDefault { 0 }
        @Override
        void update( Observable obs, Object o )
        {
            synchronized (o.class) {  // this get concurrently called from different threads -> synchronized is crucial
                counters[o.class]++
                log.info("incrementing for ${o.class}: ${counters[o.class]}")
            }
        }
    }
}