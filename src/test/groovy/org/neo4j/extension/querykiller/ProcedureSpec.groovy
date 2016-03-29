package org.neo4j.extension.querykiller

import groovy.util.logging.Slf4j
import org.junit.Rule
import org.junit.rules.RuleChain
import org.neo4j.extension.querykiller.agent.WrapNeo4jComponentsAgent
import org.neo4j.extension.querykiller.helper.AgentRule
import org.neo4j.extension.querykiller.helper.HelperProcedures
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.graphdb.QueryExecutionException
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.TransactionFailureException
import org.neo4j.graphdb.TransactionTerminatedException
import org.neo4j.helpers.collection.IteratorUtil
import org.neo4j.kernel.impl.proc.Procedures
import org.neo4j.kernel.internal.GraphDatabaseAPI
import spock.lang.Specification

/**
 * @author Stefan Armbruster
 */
@Slf4j
class ProcedureSpec extends Specification {

    @Rule
    @Delegate(interfaces=false)
    Neo4jResource neo4j = new Neo4jResource()

//    @Rule
//    RuleChain ruleChain = RuleChain.outerRule(new AgentRule(WrapNeo4jComponentsAgent)).around(neo4j)

    def "test procedure to sleep"() {
        when:

        def terminatedWithoutException = 0
        def terminatedWithException = 0

        def durations = (0..<5).collect {
            def start = System.currentTimeMillis()
            log.info("start with sleeping $it")
            Transaction tx = graphDatabaseService.beginTx()

            Thread.start {
                sleep(500)
                log.info("terminating")
                tx.terminate()
            }

            try {
                def result = graphDatabaseService.execute('CALL org.neo4j.extension.querykiller.helper.sleep(5000)')
//                def result = graphDatabaseService.execute('unwind range(0,100000) as x create (:Dummy{number:x})')
    //            result.close()
                tx.success()
            } catch (QueryExecutionException|TransactionTerminatedException e)  {
                log.info("terminated with ${e.class}");
            } finally {
                try {
                    tx.close()
                    terminatedWithoutException++
                } catch (TransactionFailureException e) {
                    log.error(e.message)
                    terminatedWithException++
                    //pass
                }

            }
            log.info("done with sleeping")
            Neo4jUtils.assertNoOpenTransaction(graphDatabaseService)
            System.currentTimeMillis() - start
        }
        log.info("terminatedWithException $terminatedWithException, terminatedWithoutException $terminatedWithoutException")
        log.info "Durations $durations"

        then:
        durations.every { it < 1000}
    }
}
