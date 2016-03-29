package org.neo4j.extension.querykiller.events.transport;

/**
 * @author Stefan Armbruster
 */
public class BoltContext extends TransportContext {

    public BoltContext(String connectionDescriptor) {
        super("bolt", null, connectionDescriptor);
    }

    @Override
    public String toString() {
        return "BoltContext{" +
                ", endPoint='" + getEndPoint() + '\'' +
                ", remoteUser='" + getRemoteUser() + '\'' +
                ", remoteHost='" + getRemoteHost() + '\'' +
                '}';
    }
}
