package org.neo4j.extension.querykiller;

import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Stefan Armbruster
 */
public class QueryKillerProcedures {

    @Context
    public GraphDatabaseAPI graphDatabaseAPI;

    @Procedure(value = "queries.list")
    public Stream<MapValue> runningQueries() {
        QueryRegistryExtension queryRegistryExtension = graphDatabaseAPI.getDependencyResolver().resolveDependency(QueryRegistryExtension.class);
        return queryRegistryExtension.getTransactionEntries().stream().map(
                transactionEntry -> new MapValue(transactionEntry.asMap())
        );
    }

    @Procedure(value = "queries.kill")
    public void kill(@Name("key") String key) {
        QueryRegistryExtension queryRegistryExtension = graphDatabaseAPI.getDependencyResolver().resolveDependency(QueryRegistryExtension.class);
        queryRegistryExtension.abortQuery(key);
    }

    public static class MapValue {
        public final Map<String, Object> value;

        public MapValue(Map<String, Object> value) {
            this.value = value;
        }
    }
}
