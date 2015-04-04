package org.neo4j.extension.querykiller.statistics

import org.junit.ClassRule
import org.neo4j.extension.spock.Neo4jServerResource
import spock.lang.Shared
import spock.lang.Specification

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

    def "statistics get updated"() {
        when:
        def response = neo4j.http.GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == 0

        when: "submit a cypher query"
        def cypher = "MATCH (n) RETURN count(n) AS c"
        neo4j.http.POST("db/data/cypher", [query: cypher])
        response = neo4j.http.GET(MOUNTPOINT)

        then:
        response.status() == 200
        response.content().size() == 1
        response.content()[cypher].total > 0
        response.content()[cypher].durations.size() == 1

        when: "submit another cypher query"
        neo4j.http.POST("db/data/cypher", [query: cypher])
        response = neo4j.http.GET(MOUNTPOINT)
        println response.content()

        then:
        response.status() == 200
        response.content().size() == 1
        response.content()[cypher].total > 0
        response.content()[cypher].durations.size() == 2
    }

}
