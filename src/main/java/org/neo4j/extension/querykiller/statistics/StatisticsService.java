package org.neo4j.extension.querykiller.statistics;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Map;

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
    public Map<String, QueryStat> statistics() {
        return queryStatisticsExtension.getSortedStatistics();


    }

    @DELETE
    public void clearStatistics() {
        queryStatisticsExtension.clear();
    }
}
