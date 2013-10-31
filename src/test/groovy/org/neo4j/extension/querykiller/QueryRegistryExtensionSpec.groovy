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
        notThrown Exception

        when:
        queryRegistryExtension.unregisterQuery(q1)

        then:
        queryRegistryExtension.runningQueries.size() == 1

        when:
        queryRegistryExtension.unregisterQuery(q1)

        then:
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
        def q1 = queryRegistryExtension.registerQuery("cypher1", "endPoint", "127.0.0.1", null)
        def q2 = queryRegistryExtension.registerQuery("cypher2", "endPoint", "127.0.0.1", null)

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
}
