package org.neo4j.extension.querykiller.events.transport;

/**
 * @author Stefan Armbruster
 */
public class EmbeddedTransportContext extends TransportContext {

    private static EmbeddedTransportContext instance;

    private EmbeddedTransportContext() {
        super("embedded", "n/a", "n/a");
    }

    public static EmbeddedTransportContext getInstance() {
        if (instance==null) {
            instance = new EmbeddedTransportContext();
        }
        return instance;
    }
}
