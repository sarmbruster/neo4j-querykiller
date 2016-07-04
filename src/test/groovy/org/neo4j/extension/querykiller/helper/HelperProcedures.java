package org.neo4j.extension.querykiller.helper;

import org.neo4j.extension.querykiller.EventBusLifecycle;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefan Armbruster
 */
public class HelperProcedures {

    private static final Logger logger = LoggerFactory.getLogger(HelperProcedures.class);

    public HelperProcedures() {}

    @Context
    public GraphDatabaseAPI graphDatabaseAPI;

    @Context
    public KernelTransaction transaction;

    @Procedure
    public void sleep(@Name("duration") long duration)  {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis()-start < duration) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Procedure
    public void transactionAwareSleep(@Name("duration") long duration)  {

        EventBusLifecycle eventBusLifecycle = graphDatabaseAPI.getDependencyResolver().resolveDependency(EventBusLifecycle.class);
        System.out.println("execution txawaresleep");
        if (eventBusLifecycle!=null) {
//            eventBusLifecycle.post();
        }

        logger.error("started transactionAwareSleep");
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis()-start < duration) {
            try {
                Thread.sleep(5);
                if (transaction.shouldBeTerminated()) {
                    logger.error("received terminated signal - returning gracefully");
                    // throwing a RTE will cause transactions stay forever
                    //throw new RuntimeException("this transaction is marked as terminated, so throw an exception");
                    return;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }


}
