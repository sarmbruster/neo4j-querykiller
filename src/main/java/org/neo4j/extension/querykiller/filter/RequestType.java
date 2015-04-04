package org.neo4j.extension.querykiller.filter;

/**
 * @author Stefan Armbruster
 */
public enum RequestType {
    ONE_SHOT,
    BEGIN,
    AMEND,
    COMMIT,
    ROLLBACK
}
