package org.neo4j.extension.querykiller

import groovy.util.logging.Slf4j
import org.junit.Rule
import org.junit.rules.RuleChain
import org.neo4j.extension.querykiller.agent.WrapNeo4jComponentsAgent
import org.neo4j.extension.querykiller.helper.AgentRule
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.graphdb.TransactionFailureException
import org.neo4j.helpers.collection.Iterators
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

import static java.util.concurrent.Executors.*

/**
 * @author Stefan Armbruster
 */
@Slf4j
class QueryKillerProceduresSpec extends Specification {

    Neo4jResource neo4j = new Neo4jResource()

    @Rule
    RuleChain ruleChain = RuleChain.outerRule(new AgentRule(WrapNeo4jComponentsAgent))
//            .around(SuppressOutput.suppressAll())   // comment this out for debugging
            .around(neo4j)

    def setup() {
        log.error "setup: $specificationContext.currentFeature.name"
    }

    def cleanup() {
        neo4j.closeCypher()
        log.info "cleanup for $specificationContext.currentFeature.name "
        Neo4jUtils.assertNoOpenTransaction(neo4j.graphDatabaseService)
        log.error "done: $specificationContext.currentFeature.name"
    }

    def "queries.list and queries.kill work as expected"() {

        when:
        def cypherList = "CALL queries.list() yield value return *"
        def result = runCypher(cypherList)
        //def result = Iterators.asList(cypherList.cypher().columnAs("value"))

        then: "query list is emtpy at start"
        result.size() == 1
        result[0].value.query == cypherList

        when: "running a lengthy query"
        String lengthyStatement = "CALL org.neo4j.extension.querykiller.helper.transactionAwareSleep(10000)"
        Future future = newSingleThreadExecutor().submit({
            runCypher(lengthyStatement)
        } as Callable)
        sleep(100)
        result = runCypher(cypherList)

        then: "query list is emtpy at start"
        result.size() == 2
        result*.value.query.contains(lengthyStatement)

        when: "killing the query"
        def key = result*.value.grep { it.query == lengthyStatement}.first().key
        runCypher("CALL queries.kill({key})", [key: key])
        sleep(100)
        result = runCypher(cypherList)

        then: "the lengthy query vanished from query list"
        result.size() == 1

        when: "fetching result of lengthy query"
        future.get()

        then: "it was terminated with TransactionFailureException"
        def ex = thrown(ExecutionException)
        ex.getCause() instanceof TransactionFailureException

    }

    private def runCypher(String cypher, params=[:]) {
        Neo4jUtils.withSuccessTransaction(neo4j.graphDatabaseService, {// we need explicit tx mangement - otherwise two tx will be used
            Iterators.asList(cypher.cypher(params))
        })
    }
}
