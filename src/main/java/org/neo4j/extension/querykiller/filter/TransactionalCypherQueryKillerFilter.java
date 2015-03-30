package org.neo4j.extension.querykiller.filter;

import org.codehaus.jackson.JsonNode;
import org.neo4j.extension.querykiller.QueryRegistryExtension;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * parse cypher from request when transactional endpoint is used
 */
public class TransactionalCypherQueryKillerFilter extends QueryKillerFilter {

    public TransactionalCypherQueryKillerFilter(QueryRegistryExtension queryRegistryExtension) {
        super(queryRegistryExtension);
    }

    @Override
    protected String extractCypherFromRequest(HttpServletRequest copyRequest) throws IOException {
        List<JsonNode> vals = getObjectMapper().readTree(copyRequest.getReader()).get("statements").findValues("statement");
        return vals.toString();
    }
}
