package org.neo4j.extension.querykiller.events.transport;

/**
 * @author Stefan Armbruster
 */
public class BoltContext extends TransportContext {

    /**
     * build a boltcontext from a 6 element string array {@link org.neo4j.bolt.v1.runtime.internal.SessionStateMachine#currentQuerySource}
     * @param splittedConnectionDescriptor
     */
    public BoltContext(String[] splittedConnectionDescriptor) {
        super(splittedConnectionDescriptor[2] + "@" + splittedConnectionDescriptor[5], splittedConnectionDescriptor[1], splittedConnectionDescriptor[4]);

        /*  example:
          0 = "bolt"
          1 = "null"
          2 = "bolt-java-driver/1.0.0-c84db27281aa0bb847e5ea2272a2f54a0ce8735d"
          3 = ""
          4 = "client/127.0.0.1:36234"
          5 = "server/127.0.0.1:7687>"
         */
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
