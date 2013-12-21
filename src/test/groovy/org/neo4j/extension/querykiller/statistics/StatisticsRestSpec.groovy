package org.neo4j.extension.querykiller.statistics

import com.sun.jersey.api.client.UniformInterfaceException
import org.junit.ClassRule
import org.neo4j.extension.querykiller.QueryRegistryExtension
import org.neo4j.extension.querykiller.events.QueryRegisteredEvent
import org.neo4j.extension.querykiller.events.QueryUnregisteredEvent
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.test.server.HTTP
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

class StatisticsRestSpec extends Specification {

    public static final String MOUNTPOINT = "statistics"

    @Shared
    @ClassRule Neo4jServerResource neo4j = new Neo4jServerResource(
            config: [ execution_guard_enabled: "true" ],
            thirdPartyJaxRsPackages: [
                    "org.neo4j.extension.querykiller.server": "/notrelevant",
                    "org.neo4j.extension.querykiller.statistics": "/$MOUNTPOINT"
            ]
    )

    // TODO: refactor to Neo4jServerResponse.http when 0.2 is released
    def getHttp() {
        HTTP.withBaseUri(neo4j.baseUrl)
    }

    def "statistics get updated"() {
        when:
        def response = http.GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == 0

        when: "submit a cypher query"
        def cypher = "MATCH (n) RETURN count(n) AS c"
        http.POST("db/data/cypher", [query: cypher])
        response = http.GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == 1
        response.content()[cypher].total > 0
        response.content()[cypher].durations.size() == 1

        when: "submit another cypher query"
        http.POST("db/data/cypher", [query: cypher])
        response = http.GET(MOUNTPOINT)
        println response.content()

        then:
        response.status() == 200
        response.content().size() == 1
        response.content()[cypher].total > 0
        response.content()[cypher].durations.size() == 2
    }

}
