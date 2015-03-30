package org.neo4j.extension.querykiller

import org.neo4j.kernel.guard.Guard
import spock.lang.Specification

class QueryRegistryExtensionSpec extends Specification
{

    def "registering and unregistering of queries"() {
        setup:
        QueryRegistryExtension queryRegistryExtension = new QueryRegistryExtension( new Guard(null))

        when:
        def q1 = queryRegistryExtension.registerQuery("cypher1", "endPoint", "127.0.0.1", null)

        then:
        queryRegistryExtension.runningQueries.size() == 1

        when:
        def q2 = queryRegistryExtension.registerQuery("cypher2", "endPoint", "127.0.0.1", null)

        then:
        queryRegistryExtension.runningQueries.size() == 2

        when:
        queryRegistryExtension.unregisterQuery(new QueryRegistryEntry())

        then:
        thrown IllegalArgumentException

        when:
        queryRegistryExtension.unregisterQuery(q1)

        then:
        queryRegistryExtension.runningQueries.size() == 1

        when:
        queryRegistryExtension.unregisterQuery(q1)

        then:
        thrown IllegalArgumentException

        and:
        queryRegistryExtension.runningQueries.size() == 1

        when:
        queryRegistryExtension.unregisterQuery(q2)

        then:
        queryRegistryExtension.runningQueries.size() == 0

    }

    def "termination of a queries"() {
        setup:
        QueryRegistryExtension queryRegistryExtension = new QueryRegistryExtension( new Guard(null))

        when:
        def q1 = queryRegistryExtension.registerQuery("cypher1-t", "endPoint", "127.0.0.1", null)
        def q2 = queryRegistryExtension.registerQuery("cypher2-t", "endPoint", "127.0.0.1", null)

        then: "guards are not triggered"
        q1.getVetoGuard().abort == false
        q2.getVetoGuard().abort == false

        when:
        queryRegistryExtension.abortQuery(q1.key)

        then:
        then: "q1 guard has been triggered"
        q1.getVetoGuard().abort == true
        q2.getVetoGuard().abort == false

        when: "try to abort a unregistered query"
        queryRegistryExtension.unregisterQuery(q2)
        queryRegistryExtension.abortQuery(q2.key)

        then:
        thrown IllegalArgumentException
    }

    def "tabular output matches table structure"() {
        setup:
        QueryRegistryExtension queryRegistryExtension = new QueryRegistryExtension( new Guard(null))
        assert queryRegistryExtension.runningQueries.size() == 0
        queryRegistryExtension.registerQuery("cypher1", "endPoint", "127.0.0.1", null)
        queryRegistryExtension.registerQuery("cypher2", "endPoint", "127.0.0.1", null)

        when:
        def lines = queryRegistryExtension.formatAsTable().split("\n")

        then:
        queryRegistryExtension.runningQueries.size() == 2
        lines.size() == 5
        lines[0] == "+---------+----------+--------------------------------------------------------------+-----------------+-----------------+"
        lines[1] == "| time ms | key      | query                                                        | source          | endPoint        |"
        lines[2] =~ /^\| ....... \| \w{8} \| cypher1                                                      \| 127\.0\.0\.1       \| endPoint        \|$/
        lines[3] =~ /^\| ....... \| \w{8} \| cypher2                                                      \| 127\.0\.0\.1       \| endPoint        \|$/
        lines[4] == "+---------+----------+--------------------------------------------------------------+-----------------+-----------------+"
    }

}
