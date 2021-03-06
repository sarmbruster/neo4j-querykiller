package org.neo4j.extension.querykiller.server;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.extension.querykiller.NoSuchQueryException;
import org.neo4j.extension.querykiller.QueryRegistryExtension;
import org.neo4j.extension.querykiller.TransactionEntry;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/")
public class QueryKillerService {

    public final Logger log = LoggerFactory.getLogger(QueryKillerService.class);

    @Context
    private QueryRegistryExtension queryRegistryExtension;

    /**
     * to be used from tests
     * @param queryRegistryExtension
     */
    public void setQueryRegistryExtension( QueryRegistryExtension queryRegistryExtension )
    {
        this.queryRegistryExtension = queryRegistryExtension;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response activeQueries() throws IOException {
        List<Map<String,Object>> result = new ArrayList<>();
        for (TransactionEntry entry : queryRegistryExtension.getTransactionEntries()) {
            result.add(entry.asMap());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return Response.ok().entity(objectMapper.writeValueAsString(result)).build();
    }

    @DELETE
    @Path("/{queryKey}")
    public Map<String,String> killQuery(@PathParam("queryKey") String queryKey) {
        try {
            queryRegistryExtension.abortQuery(queryKey);
            return MapUtil.stringMap("deleted", queryKey);
        } catch (NoSuchQueryException e) {  // TODO: consider a more JAX-RS like approach for mapping exception to responses
            return null;
        }
    }
}
