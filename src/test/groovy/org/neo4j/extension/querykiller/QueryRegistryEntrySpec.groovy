package org.neo4j.extension.querykiller

import com.sun.jersey.api.json.JSONJAXBContext
import spock.lang.Specification

import javax.xml.bind.JAXBContext

class QueryRegistryEntrySpec extends Specification {

    def "test json marshalling"() {
        setup:
        def context = JAXBContext.newInstance(QueryRegistryEntry)
        def m = context.createMarshaller()
        def jsonMarshaller = JSONJAXBContext.getJSONMarshaller(m)
        def sw = new StringWriter()

        when:
        QueryRegistryEntry entry = new QueryRegistryEntry("abc", new VetoGuard())
        jsonMarshaller.marshallToJSON(entry, sw)
        def s = sw.toString()
        def dateString = entry.started.format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")


        then: "vetoguard is not exposed"
        !(s=~/veto/)

        and:
        s == """{"cypher":"abc","key":"${entry.key}","started":"${dateString}","thread":"main"}"""

    }
}
