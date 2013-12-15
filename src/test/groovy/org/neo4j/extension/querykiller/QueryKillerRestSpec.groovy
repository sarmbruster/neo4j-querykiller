package org.neo4j.extension.querykiller

import com.sun.jersey.api.client.UniformInterfaceException
import org.junit.ClassRule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.test.server.HTTP
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

class QueryKillerRestSpec extends Specification {

    public static final String MOUNTPOINT = "querykiller"

    @Shared
    @ClassRule Neo4jServerResource neo4j = new Neo4jServerResource(
            config: [ execution_guard_enabled: "true" ],
            thirdPartyJaxRsPackages: [ "org.neo4j.extension.querykiller": "/$MOUNTPOINT" ]
    )

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

    }

    @Unroll
    def "send query with delay and check if registry handles this correctly"() {

        setup:
        def threads =  (0..<numberOfQueries).collect { Thread.start runCypherQuery.curry(delay) }
        sleep 10  // otherwise cypher requests have not yet arrived

        when: "check query list"
        def response = http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() >= resultRows -1 // 1 off, otherwise test are flacky

        when:
        threads.each { it.join() }
        sleep 100
        response = http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size()==0

        cleanup:
        threads.each { it.join() }

        where:
        numberOfQueries | delay | resultRows
        0               | 50    | 0
        1               | 50    | 1
        2               | 50    | 2
        4               | 50    | 4
//        8               | 50    | 8
    }

    def "send query with delay and terminate it"() {
        setup:
        def threads =  (0..<1).collect { Thread.start runCypherQuery.curry(1000) }
        sleep 100  // otherwise cypher requests have not yet arrived

        when: "check query list"
        def response = http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

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
        sleep 100
        response = http.withHeaders("Accept", MediaType.APPLICATION_JSON).GET(MOUNTPOINT)

        then:
        response.status() == 200

        and:
        response.content().size() == 0

        cleanup:
        threads.each { it.join() }

    }

    Closure runCypherQuery = { delay ->
        http.withHeaders("X-Delay", delay as String).POST("db/data/cypher", [
                query: "MATCH (n) RETURN count(n) AS c",
        ])
    }

}
