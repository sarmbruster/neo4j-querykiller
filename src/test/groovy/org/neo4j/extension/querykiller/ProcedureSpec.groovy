package org.neo4j.extension.querykiller

import groovy.util.logging.Slf4j
import org.junit.Rule
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.graphdb.QueryExecutionException
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.TransactionFailureException
import org.neo4j.graphdb.TransactionTerminatedException
import spock.lang.Specification
import spock.lang.Unroll

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

    @Unroll("executing #cypher")
    def "test procedure to sleep"() {
        when:

        def terminatedWithoutException = 0
        def terminatedWithException = 0

        // we run the cypher statement below 5 times to understand different behaviour due to query plan caching
        // and collect the runtime of each invocation in an array
        def durations = (0..<5).collect {
            def start = System.currentTimeMillis()
            log.info("start with sleeping $it")
            Transaction tx = graphDatabaseService.beginTx()

            // in 500ms we're sending the transaction a "kill" from a separate thread
            Thread.start {
                sleep(500)
                log.info("terminating")
                tx.terminate()
            }

            try {
                graphDatabaseService.execute(cypher) // go sleeping
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

        then: "the runtime of each invocation is smaller than a threshold"
        durations.eachWithIndex{ duration, index -> duration < maxDurations[index]}

        where:
        cypher                                                                    | maxDurations
        "CALL org.neo4j.extension.querykiller.helper.sleep(1000)"                 | [1500, 1500, 1100, 1100, 1100]
        "CALL org.neo4j.extension.querykiller.helper.transactionAwareSleep(1000)" | [550, 550, 550, 550, 500]

    }
}
