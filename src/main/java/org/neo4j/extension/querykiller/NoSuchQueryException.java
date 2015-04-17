package org.neo4j.extension.querykiller;

/**
 * @author Stefan Armbruster
 */
public class NoSuchQueryException extends RuntimeException {

    private String key;

    public NoSuchQueryException( String key) {
        super("no query registered for key " + key);
    }

    public String getKey() {
        return key;
    }
}
