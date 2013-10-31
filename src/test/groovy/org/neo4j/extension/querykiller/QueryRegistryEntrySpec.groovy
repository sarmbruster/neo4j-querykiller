package org.neo4j.extension.querykiller

import spock.lang.Ignore

//import com.sun.jersey.api.json.JSONJAXBContext
import spock.lang.Specification
import spock.lang.Unroll

import javax.xml.bind.JAXBContext

class QueryRegistryEntrySpec extends Specification
{

    @Ignore("right now, jaxb is not used")
    def "test json marshalling"( )
    {
        setup:
        def context = JAXBContext.newInstance( QueryRegistryEntry )
        def m = context.createMarshaller()
        def jsonMarshaller = JSONJAXBContext.getJSONMarshaller( m )
        def sw = new StringWriter()

        when:
        QueryRegistryEntry entry = new QueryRegistryEntry( "abc", new VetoGuard() )
        jsonMarshaller.marshallToJSON( entry, sw )
        def s = sw.toString()
        def dateString = entry.started.format( "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" )


        then: "vetoguard is not exposed"
        !(s =~ /veto/)

        and:
        s == """{"cypher":"abc","key":"${entry.key}","started":"${dateString}","thread":"main"}"""

    }

    @Unroll
    def "test table formatting"( )
    {
        setup:
        QueryRegistryEntry queryRegistryEntry = new QueryRegistryEntry( null, cypher, endPoint, remoteHost, null )

        when: "we need to split the formatted string, first part contains duration in msec"
        def result = queryRegistryEntry.formatAsTable()
        def timePart = result.substring( 0, 23 )
        def rest = result.substring( 23 )

        then:
        timePart =~ /|    \d | +\d+ +|/
        rest == resultTail

        where:
        cypher                                                                                 | endPoint  | remoteHost  | resultTail
        "start n=node(*) return count(n) as c"                                                 | "/cypher" | "127.0.0.1" | "| start n=node(*) return count(n) as c                         | 127.0.0.1       | /cypher         |"
        "start n=node(*) \nreturn count(n) as c"                                               | "/cypher" | "127.0.0.1" | "| start n=node(*) return count(n) as c                         | 127.0.0.1       | /cypher         |"
        "    start n=node(*)  return count(n) as c   "                                         | "/cypher" | "127.0.0.1" | "| start n=node(*)  return count(n) as c                        | 127.0.0.1       | /cypher         |"
        "start n=node(*) match n--(aVeryLongNameGoesHereToExceedLimit)--(next2)--abc return c" | "/cypher" | "127.0.0.1" | "| start n=node(*) match n--(aVeryLongNameGoesHereToExceedLimit | 127.0.0.1       | /cypher         |"

    }
}
