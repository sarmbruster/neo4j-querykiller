package org.neo4j.extension.querykiller.events.transport;

/**
 * @author Stefan Armbruster
 */
public class HttpContext extends TransportContext {

    private final String method;

    public HttpContext(String endPoint, String remoteUser, String remoteHost, String method) {
        super(endPoint, remoteUser, remoteHost);
        this.method = method;
    }

    @Override
    public String toString() {
        return "HttpContext{" +
                "method='" + method + '\'' +
                ", endPoint='" + getEndPoint() + '\'' +
                ", remoteUser='" + getRemoteUser() + '\'' +
                ", remoteHost='" + getRemoteHost() + '\'' +
                '}';
    }
}
