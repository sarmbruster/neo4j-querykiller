package org.neo4j.extension.querykiller.filter;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.neo4j.extension.querykiller.QueryRegistryExtension;

/**
 * parses the cypher statement from a call to the legacy cypher endpoint
 */
public class LegacyCypherQueryKillerFilter extends QueryKillerFilter {

    public LegacyCypherQueryKillerFilter(QueryRegistryExtension queryRegistryExtension) {
        super(queryRegistryExtension);
    }

    @Override
    protected String extractCypherFromRequest(HttpServletRequest copyRequest) throws IOException
    {
        return getObjectMapper().readTree( copyRequest.getReader() ).get( "query" ).getTextValue();
    }

}
