package org.neo4j.extension.querykiller;

/**
 * @author Stefan Armbruster
 */
public class NoSuchQueryException extends RuntimeException {

//    private Object key;

    public NoSuchQueryException( Object key) {
        super("no query registered for key " + key);
    }

//    public String getKey() {
//        return key;
//    }
}
