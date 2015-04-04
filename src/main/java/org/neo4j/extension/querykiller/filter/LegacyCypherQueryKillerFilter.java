package org.neo4j.extension.querykiller.filter;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * parses the cypher statement from a call to the legacy cypher endpoint
 */
public class LegacyCypherQueryKillerFilter extends QueryKillerFilter {

    public LegacyCypherQueryKillerFilter(QueryRegistryExtension queryRegistryExtension,
                                         GraphDatabaseService graphDatabaseService) {
        super(queryRegistryExtension, graphDatabaseService);
    }

    @Override
    protected String extractCypherFromRequest(HttpServletRequest copyRequest)
    {
        try {
            return getObjectMapper().readTree( copyRequest.getReader() ).get( "query" ).getTextValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
