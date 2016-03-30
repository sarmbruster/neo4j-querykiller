package org.neo4j.extension.querykiller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.extension.querykiller.helper.HelperProcedures;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.TestGraphDatabaseFactory;

/**
 * @author Stefan Armbruster
 */
public class ProcedureTests {

    private GraphDatabaseService gdb;

    @Before
    public void setUpGraphDb() throws KernelException {
        gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        Procedures procedures = ((GraphDatabaseAPI)gdb).getDependencyResolver().resolveDependency(Procedures.class);
        procedures.register(HelperProcedures.class);
    }

    @After
    public void teatDownGraphDb() {
        gdb.shutdown();
    }

    // this one is for demoing a kind of unexpected behaviour - to be discussed with
    @Test(expected = TransactionFailureException.class)
    public void shouldVoidProcedureWithCloseThrowException() throws KernelException {
        try (Transaction tx = gdb.beginTx()) {
            try (Result result = gdb.execute("CALL org.neo4j.extension.querykiller.helper.sleep(500)")) {
                //pass
            }
            tx.success();
        }
    }

    @Test
    public void shouldVoidProcedureWithoutCloseThrowNoException() throws KernelException {
        try (Transaction tx = gdb.beginTx()) {
            Result result = gdb.execute("CALL org.neo4j.extension.querykiller.helper.sleep(500)");
            // if we don't call result.close() we don't get an exception
            tx.success();
        }
    }
}
