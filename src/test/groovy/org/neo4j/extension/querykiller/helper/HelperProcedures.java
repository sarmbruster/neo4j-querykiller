package org.neo4j.extension.querykiller.helper;

import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

/**
 * @author Stefan Armbruster
 */
public class HelperProcedures {

    public HelperProcedures() {}

//    @Context
//    public GraphDatabaseService db;

    @Context
    public KernelTransaction transaction;

    @Procedure
    public void sleep(@Name("duration") long duration)  {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis()-start < duration) {
            try {
                Thread.sleep(5);
                if (transaction.shouldBeTerminated()) {
                    throw new RuntimeException("HURZ");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }


    }
}
