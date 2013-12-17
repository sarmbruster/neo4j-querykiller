package org.neo4j.extension.querykiller;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import org.neo4j.extension.querykiller.http.CopyHttpServletRequest;

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
