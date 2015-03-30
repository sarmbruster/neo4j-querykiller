package org.neo4j.extension.querykiller.http

import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

class CopyHttpServletRequestSpec extends Specification
{

    public static final String BODY = "body of message"

    def "CopyHttpServletRequest allow access to request body multiple times"() {

        setup:
        def origRequest = createMockRequest()
        def cut = new CopyHttpServletRequest(origRequest)

        expect: "call getReader multiple times"
        cut.getReader().text == BODY
        cut.getReader().text == BODY

        and: "call getInputStream multiple times"
        cut.getInputStream().text == BODY
        cut.getInputStream().text == BODY

    }

    def "mockRequest does not support multiple invocations of getReader"() {

        setup:
        def origRequest = createMockRequest()

        expect: "call getReader first time"
        origRequest.getReader().text == BODY

        when: "call getReader second time"
        origRequest.getReader().text == BODY

        then:
        thrown IOException

    }


    private HttpServletRequest createMockRequest() {
        def inputStream = new ByteArrayInputStream( BODY.bytes )
        def servletInputStream = new ServletInputStreamWrapper( inputStream )
        def reader = new BufferedReader( new InputStreamReader( inputStream ) )

        [
                getReader: {-> reader },
                getInputStream: {-> servletInputStream }
        ] as HttpServletRequest
    }

}
