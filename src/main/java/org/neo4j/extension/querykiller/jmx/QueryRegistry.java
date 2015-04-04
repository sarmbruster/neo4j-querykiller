package org.neo4j.extension.querykiller.jmx;

import org.neo4j.extension.querykiller.QueryRegistryEntry;
import org.neo4j.jmx.Description;
import org.neo4j.jmx.ManagementInterface;

import java.util.SortedSet;

/**
 * @author Stefan Armbruster
 */
@ManagementInterface( name = QueryRegistry.NAME )
@Description( "Information about the Neo4j page cache" )
public interface QueryRegistry {
    String NAME = "Queries";

    @Description("number of currently running queries")
    int getRunningQueriesCount();

    @Description("list of currently running queries")
    SortedSet<QueryRegistryEntry> getRunningQueries();

    @Description("terminate a query by id")
    void terminate(String id);
}
