package org.neo4j.extension.querykiller

import com.sun.jersey.api.client.Client
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.server.plugins.PluginLifecycle
import org.neo4j.server.rest.RestRequest
import org.neo4j.server.rest.domain.GraphDbHelper
import spock.lang.Shared

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
        jsonOut.data[0][0] == 1

    }

/*
    def "test /relationship/{uuid}"() {
        setup:
        def node = withTransaction { graphDB.createNode()}

        when:
        def response = request.get("node/" + node.getProperty('uuid'))

        then:
        response.status==200

        and:
        response.getEntity() == node.id as String

    }

    def "test /node/{uuid}"() {
        setup:
        def rel = withTransaction {
            def startNode = graphDB.createNode()
            def endNode = graphDB.createNode()
            startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName('RELATED'))
        }

        when:
        def response = request.get("relationship/" + rel.getProperty('uuid'))

        then:
        response.status==200

        and:
        response.getEntity() == rel.id as String

    }
*/

}
