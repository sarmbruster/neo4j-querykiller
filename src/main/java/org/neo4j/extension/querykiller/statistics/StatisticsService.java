package org.neo4j.extension.querykiller.statistics;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.extension.querykiller.QueryRegistryEntry;
import org.neo4j.extension.querykiller.QueryRegistryExtension;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class StatisticsService {

    @Context
    private QueryStatisticsExtension queryStatisticsExtension;

    /**
     * to be used from tests
     * @param queryStatisticsExtension
     */
    public void setQueryStatisticsExtension(QueryStatisticsExtension queryStatisticsExtension)
    {
        this.queryStatisticsExtension = queryStatisticsExtension;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response statistics() throws IOException {

/*        long now = System.currentTimeMillis();
        List<Map<String,Object>> result = new ArrayList<>();
        for (QueryRegistryEntry q : queryStatisticsExtension.getRunningQueries()) {
            Map<String, Object> map = new HashMap<>();
            map.put("cypher", q.getCypher());
            map.put("key", q.getKey());
            map.put("since", now - q.getStarted().getTime());
            map.put("thread", q.getThread());
            map.put("remoteHost", q.getRemoteHost());
            map.put("remoteUser", q.getRemoteUser());
            map.put("endPoint", q.getEndPoint());
            result.add(map);
        }*/

        ObjectMapper objectMapper = new ObjectMapper();
        return Response.ok().entity(objectMapper.writeValueAsString(queryStatisticsExtension.getSortedStatistics())).build();
    }

/*
    @DELETE
    @Path("/{queryKey}")
    public void killQuery(@PathParam("queryKey") String queryKey) {
        queryStatisticsExtension.abortQuery(queryKey);
    }
*/
}
