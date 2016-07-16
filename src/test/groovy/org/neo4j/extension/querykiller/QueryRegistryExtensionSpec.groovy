package org.neo4j.extension.querykiller

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.neo4j.extension.querykiller.events.bind.BindTransactionEvent
import org.neo4j.extension.querykiller.events.query.QueryRegisteredEvent
import org.neo4j.extension.querykiller.events.query.QueryUnregisteredEvent
import org.neo4j.extension.querykiller.events.bind.UnbindTransactionEvent
import org.neo4j.kernel.api.KernelTransaction
import org.neo4j.kernel.configuration.Config
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QueryRegistryExtensionSpec extends Specification {

    EventBus eventBus
    QueryRegistryExtension queryRegistryExtension
    ExecutorService exec1, exec2

    def setup() {
        eventBus = new EventBusLifecycle(null)

        queryRegistryExtension = new QueryRegistryExtension(
                [getConfig: { new Config()},
                 getEventBusLifecycle: { eventBus } ]
                        as QueryRegistryExtensionFactory.Dependencies)
        queryRegistryExtension.start()

        // create 2 ExecutorServices working on separate threads - that's why a threadfactory needs to be provided
        exec1 = Executors.newSingleThreadExecutor( {new Thread(it)} )
        exec2 = Executors.newSingleThreadExecutor( {new Thread(it)} )
    }

    def "registering and unregistering of queries"() {

        when:
        def transaction1 = Mock(KernelTransaction)
        exec1.submit({
            println Thread.currentThread()
            eventBus.post(new BindTransactionEvent(transaction1))
        }).get()

        then:
        queryRegistryExtension.transactionEntries.size() == 1

        when:
        def transaction2 = Mock(KernelTransaction)
        sleep 1
        exec2.submit({
            println Thread.currentThread()
            eventBus.post(new BindTransactionEvent(transaction2))
        }).get()

        then:
        queryRegistryExtension.transactionEntries.size() == 2

        when:
        exec1.submit({
            eventBus.post(new UnbindTransactionEvent(Mock(KernelTransaction)))
        }).get()

        then:
        noExceptionThrown()
        queryRegistryExtension.transactionEntries.size() == 2

        when:
        exec1.submit({
            eventBus.post(new UnbindTransactionEvent(transaction1))
        }).get()

        then:
        queryRegistryExtension.transactionEntries.size() == 1

        when:
        exec1.submit({
            eventBus.post(new UnbindTransactionEvent(transaction1))
        }).get()

        then:
        noExceptionThrown()

        and:
        queryRegistryExtension.transactionEntries.size() == 1

        when:
        exec2.submit({
            eventBus.post(new UnbindTransactionEvent(transaction2))
        }).get()

        then:
        queryRegistryExtension.transactionEntries.size() == 0

    }

    def "termination of a queries"() {
        setup:

        def queryRegistryEntries = []
        eventBus.register(new Object() {
            @Subscribe
            public void handleQueryRegisteredEvent(QueryRegisteredEvent event) {
                queryRegistryEntries << event.transactionEntry
            }
            @Subscribe
            public void handleQueryUnregisteredEvent(QueryUnregisteredEvent event) {
                queryRegistryEntries.remove(event.transactionEntry)
            }
        })

        when:
        exec1.submit {
            eventBus.post(new BindTransactionEvent(Mock(KernelTransaction)))
        }.get()
        sleep 1
        KernelTransaction transaction2 = Mock(KernelTransaction)
        exec2.submit {
            eventBus.post(new BindTransactionEvent(transaction2))
        }.get()

        then: "transactions are not killed"
        queryRegistryEntries*.killed == [false, false]

        when:
        queryRegistryExtension.abortQuery(queryRegistryEntries[0].key)

        then:
        queryRegistryEntries*.killed == [true, false]

        when: "try to abort a unregistered query"
        def key2 = queryRegistryEntries[1].key

        exec2.submit {
            eventBus.post(new UnbindTransactionEvent(transaction2))
        }.get()
        queryRegistryExtension.abortQuery(key2)

        then:
        thrown NoSuchQueryException
    }

    def "tabular output matches table structure"() {
        setup:

        assert queryRegistryExtension.transactionEntries.size() == 0
        exec1.submit {
            eventBus.post(new BindTransactionEvent(Mock(KernelTransaction)))
        }.get()
        sleep 1
        exec2.submit {
            eventBus.post(new BindTransactionEvent(Mock(KernelTransaction)))
        }.get()

        when:
        def lines = queryRegistryExtension.formatAsTable().split("\n")
//        println lines[2]

        then:
        queryRegistryExtension.transactionEntries.size() == 2
        lines.size() == 5
        lines[0] == "+---------+----------+--------------------------------------------------------------+-----------------+-----------------+-----------------+"
        lines[1] == "| millis  | key      | query                                                        | remoteUser      | remoteHost      | endPoint        |"
        lines[2] =~ /^\| ....... \| \w{8} \| null                                                         \| n\/a             \| n\/a             \| null            \|$/
        lines[3] =~ /^\| ....... \| \w{8} \| null                                                         \| n\/a             \| n\/a             \| null            \|$/
        lines[4] == "+---------+----------+--------------------------------------------------------------+-----------------+-----------------+-----------------+"
    }

}
