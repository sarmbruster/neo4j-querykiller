package org.neo4j.extension.querykiller.events.cypher;

import java.util.Map;

/**
 * @author Stefan Armbruster
 */
public class CypherContext {
    
    final private String query; 
    final private Map<String, Object> parameters;

    public CypherContext(String query, Map<String, Object> parameters) {
        this.query = query;
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return query;
    }
}
