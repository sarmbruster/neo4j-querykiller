package org.neo4j.extension.querykiller

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.json.JSONJAXBContext
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.server.logging.Logger
import org.neo4j.server.plugins.PluginLifecycle
import org.neo4j.server.rest.RestRequest
import org.neo4j.server.rest.domain.GraphDbHelper
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

import javax.ws.rs.core.MediaType
import javax.xml.bind.JAXBContext

class QueryKillerRestSpec extends NeoServerSpecification {

    public static final Logger log = Logger.getLogger( QueryKillerRestSpec.class );


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
        jsonOut.data[0][0] == 1

    }

    def "send cypher query with delay"() {

        when:
        def jsonIn = """{
          "query" : "start n=node(*) return count(n) as c",
          "params" : {
          }
        }"""
        request.header("X-Delay", "1000")
        def response = request.post("db/data/cypher", jsonIn)
        def jsonOut = new JsonSlurper().parseText(response.entity)

        then:
        response.status == 200

        and:
        jsonOut.columns[0] == "c"
        jsonOut.data[0][0] == 1

    }

    @Unroll("dummy #numberOfQueries")
    def "send query with delay and check if registry handles this correctly"() {

        setup:
        def threads =  (0..<numberOfQueries).collect { Thread.start runCypherQuery }
        sleep 10  // otherwise cypher requests have not yet arrived

        when: "check query list"
        request.accept(MediaType.APPLICATION_JSON_TYPE)
        def response = request.get("querykiller")
        def json = new JsonSlurper().parseText(response.entity)
        log.warn "sent 1st request ${json.size()}"

        then:
        response.status == 200

        and:
        json.size() == resultRows

        when:
        threads.each { it.join() }
        response = request.get("querykiller")
        json = new JsonSlurper().parseText(response.entity)
        log.warn "sent 2nd request ${json.size()}"

        then:
        response.status == 200

        and:
        json.size()==0

        where:
        numberOfQueries | resultRows
        0 | 0
        1 | 1
        2 | 2
    }


    Closure runCypherQuery = {
        def req = new RestRequest(server.baseUri(), client)
        def jsonIn = """{
                      "query" : "start n=node(*) return count(n) as c",
                      "params" : {
                      }
                    }"""
        req.header("X-Delay", "500")
        req.post("db/data/cypher", jsonIn)
    }

}
