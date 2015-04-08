package org.neo4j.extension.querykiller

import org.neo4j.graphdb.Transaction
import spock.lang.Specification

class QueryRegistryExtensionSpec extends Specification
{

    def transactionMock = [ terminate: {->}] as Transaction

    def "registering and unregistering of queries"() {
        setup:
        QueryRegistryExtension queryRegistryExtension = new QueryRegistryExtension()

        when:
        def q1 = queryRegistryExtension.registerQuery(transactionMock, "cypher1", "endPoint", "127.0.0.1", null)

        then:
        queryRegistryExtension.runningQueries.size() == 1

        when:
        def q2 = queryRegistryExtension.registerQuery(transactionMock, "cypher2", "endPoint", "127.0.0.1", null)

        then:
        queryRegistryExtension.runningQueries.size() == 2

        when:
        queryRegistryExtension.unregisterQuery(new QueryRegistryEntry())

        then:
        notThrown()

        when:
        queryRegistryExtension.unregisterQuery(q1)

        then:
        queryRegistryExtension.runningQueries.size() == 1

        when:
        queryRegistryExtension.unregisterQuery(q1)

        then:
        notThrown()

        and:
        queryRegistryExtension.runningQueries.size() == 1

        when:
        queryRegistryExtension.unregisterQuery(q2)

        then:
        queryRegistryExtension.runningQueries.size() == 0

    }

    def "termination of a queries"() {
        setup:
        QueryRegistryExtension queryRegistryExtension = new QueryRegistryExtension()

        when:
        def q1 = queryRegistryExtension.registerQuery(transactionMock, "cypher1-t", "endPoint", "127.0.0.1", null)
        def q2 = queryRegistryExtension.registerQuery(transactionMock, "cypher2-t", "endPoint", "127.0.0.1", null)

        then: "guards are not triggered"
        q1.killed == false
        q2.killed == false

        when:
        queryRegistryExtension.abortQuery(q1.key)

        then:
        then: "q1 guard has been triggered"
        q1.killed == true
        q2.killed == false

        when: "try to abort a unregistered query"
        queryRegistryExtension.unregisterQuery(q2)
        queryRegistryExtension.abortQuery(q2.key)

        then:
        thrown IllegalArgumentException
    }

    def "tabular output matches table structure"() {
        setup:
        QueryRegistryExtension queryRegistryExtension = new QueryRegistryExtension()
        assert queryRegistryExtension.runningQueries.size() == 0
        queryRegistryExtension.registerQuery(transactionMock, "cypher1", "endPoint", "127.0.0.1", null)
        queryRegistryExtension.registerQuery(transactionMock, "cypher2", "endPoint", "127.0.0.1", null)

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
