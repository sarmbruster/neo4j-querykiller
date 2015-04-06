package org.neo4j.extension.querykiller.jmx;

import org.neo4j.jmx.Description;
import org.neo4j.jmx.ManagementInterface;

import java.util.Collection;
import java.util.Map;

/**
 * @author Stefan Armbruster
 */
@ManagementInterface( name = QueriesMBean.NAME )
@Description( "Information about the Neo4j page cache" )
public interface QueriesMBean {
    String NAME = "Queries";

    @Description("number of currently running queries")
    int getRunningQueriesCount();

    @Description("list of currently running queries")
    Collection<Map<String, Object>> getRunningQueries();

    @Description("terminate a query by id")
    void terminate(String id);

    @Description("query statistics")
    Map<String, Map<String, Object>> getStatistics();

    @Description("clear statistics")
    void clearStatistics();

}
