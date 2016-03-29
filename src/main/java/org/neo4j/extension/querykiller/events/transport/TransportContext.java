package org.neo4j.extension.querykiller.events.transport;

/**
 * marker class for eventbus for all events providing context for a query
 * @author Stefan Armbruster
 */
public abstract class TransportContext {

    final private String endPoint;
    final private String remoteUser;
    final private String remoteHost;

    public TransportContext(String endPoint, String remoteUser, String remoteHost) {
        this.endPoint = endPoint;
        this.remoteUser = remoteUser;
        this.remoteHost = remoteHost;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public String toString() {
        return "TransportContext{" +
                "endPoint='" + endPoint + '\'' +
                ", remoteUser='" + remoteUser + '\'' +
                ", remoteHost='" + remoteHost + '\'' +
                '}';
    }
}
