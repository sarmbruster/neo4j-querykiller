package org.neo4j.extension.querykiller

import com.sun.jersey.api.client.Client
import groovy.json.JsonSlurper
import org.neo4j.server.rest.RestRequest
import org.neo4j.server.rest.domain.GraphDbHelper
import spock.lang.Shared
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

class QueryKillerRestSpec extends NeoServerSpecification {

    public static final String MOUNTPOINT = "/querykiller"
    @Shared Client client = Client.create()
    @Shared GraphDbHelper helper
    private RestRequest request

    def setupSpec() {
        assert graphDB
        this.helper = new GraphDbHelper( server.getDatabase() )
        //client.addFilter(new LoggingFilter())
    }

    @Override
    Map thirdPartyJaxRsPackages() {
        ["org.neo4j.extension.querykiller": MOUNTPOINT]
    }

    def setup() {
        request = new RestRequest(server.baseUri() /*.resolve(MOUNTPOINT+"/")*/, client)
    }

    def "send cypher query"() {

        when:
        def jsonIn = """{
          "query" : "start n=node(*) return count(n) as c",
          "params" : {
          }
        }"""
        def response = request.post("db/data/cypher", jsonIn)
        def jsonOut = new JsonSlurper().parseText(response.entity)

        then:
        response.status == 200

        and:
        jsonOut.columns[0] == "c"
        jsonOut.data[0][0] == 0

    }

    def "send cypher query with delay"() {

        when:
        def jsonIn = """{
          "query" : "start n=node(*) return count(n) as c",
          "params" : {
          }
        }"""
        def delay = 100
        request.header("X-Delay", delay as String)
        def now = System.currentTimeMillis()
        def response = request.post("db/data/cypher", jsonIn)
        def jsonOut = new JsonSlurper().parseText(response.entity)
        def duration = System.currentTimeMillis() - now

        then:
        response.status == 200

        and:
        jsonOut.columns[0] == "c"
        jsonOut.data[0][0] == 0

        and:
        duration > delay

    }

    @Unroll
    def "send query with delay and check if registry handles this correctly"() {

        setup:
        def threads =  (0..<numberOfQueries).collect { Thread.start runCypherQuery.curry(delay) }
        sleep 10  // otherwise cypher requests have not yet arrived

        when: "check query list"
        request.accept(MediaType.APPLICATION_JSON_TYPE)
        def response = request.get("querykiller")
        def json = new JsonSlurper().parseText(response.entity)

        then:
        response.status == 200

        and:
        json.size() >= resultRows -1 // 1 off, otherwise test are flacky

        when:
        threads.each { it.join() }
        sleep 100
        response = request.get("querykiller")
        json = new JsonSlurper().parseText(response.entity)

        then:
        response.status == 200

        and:
        json.size()==0

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
        request.accept(MediaType.APPLICATION_JSON_TYPE)
        def response = request.get("querykiller")
        def json = new JsonSlurper().parseText(response.entity)

        then:
        response.status == 200

        and:

        json.size() == 1
        json[0].cypher == "start n=node(*) return count(n) as c"


        when:
        def key = json[0].key
        response = request.delete("querykiller/$key")


        //json = new JsonSlurper().parseText(response.entity)

        then:
        response.status == 204

        when: "check query list again"
        sleep 100
        response = request.get("querykiller")
        json = new JsonSlurper().parseText(response.entity)

        then:
        response.status == 200

        and:
        json.size() == 0

        cleanup:
        threads.each { it.join() }

    }

    Closure runCypherQuery = { delay ->
        def req = new RestRequest(server.baseUri(), client)
        def jsonIn = """{
                      "query" : "start n=node(*) return count(n) as c",
                      "params" : {
                      }
                    }"""
        req.header("X-Delay", delay as String)
        req.post("db/data/cypher", jsonIn)
    }

}
