package org.neo4j.extension.querykiller;

import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
public class QueryKillerService {

    private QueryRegistry queryRegistry;

    public QueryKillerService(@Context QueryRegistry queryRegistry) {
        this.queryRegistry = queryRegistry;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response activeQueries() throws IOException {

        List<Map<String,Object>> result = new ArrayList<>();
        for (QueryRegistryEntry q : queryRegistry.getRunningQueries()) {
            Map<String, Object> map = new HashMap<>();
            map.put("cypher", q.getCypher());
            map.put("key", q.getKey());
            map.put("started", q.getStarted());
            map.put("thread", q.getThread());
            result.add(map);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return Response.ok().entity(objectMapper.writeValueAsString(result)).build();
    }

    @DELETE
    @Path("queryKey")
    public void killQuery(@PathParam("queryKey") String queryKey) {
        queryRegistry.abortQuery(queryKey);
    }
}
