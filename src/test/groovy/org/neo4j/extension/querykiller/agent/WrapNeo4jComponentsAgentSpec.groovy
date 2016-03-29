package org.neo4j.extension.querykiller.agent

import com.ea.agentloader.AgentLoader
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.neo4j.extension.querykiller.events.bind.BindTransactionEvent
import org.neo4j.kernel.api.KernelTransaction
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge
import spock.lang.Specification

/**
 * @author Stefan Armbruster
 */
class WrapNeo4jComponentsAgentSpec extends Specification {

    def setupSpec() {
        AgentLoader.loadAgentClass(WrapNeo4jComponentsAgent.class.name, null);
    }

    def "ThreadToStatementContextBridge has an eventbus"() {
        expect:
        ThreadToStatementContextBridge.class.getDeclaredField("eventBus") != null
    }

    def "events get set upon bindTransactionToCurrentThread"() {
        setup:
        def eventBus = new EventBus()
        def bridge = new ThreadToStatementContextBridge(eventBus:eventBus)

        def receiver = new EventReceiver()
        eventBus.register(receiver)

        def transaction = Mock(KernelTransaction)

        when:
        bridge.bindTransactionToCurrentThread(transaction)

        then:
        noExceptionThrown()

        and:
        receiver.events.size() == 1
        receiver.events[0] instanceof BindTransactionEvent
        receiver.events[0].kernelTransaction == transaction
    }


    def "simple test for eventbus"() {
        setup:
        def eventBus = new EventBus()
        def receiver = new EventReceiver()
        eventBus.register(receiver)

        when:
        eventBus.post("HURZ")
        eventBus.post(111)


        then:
        receiver.events == ["HURZ", 111]
    }

    public class EventReceiver {
        def events = []

        @Subscribe
        public void handleMessage(Object event) {
            events << event
        }
    }
}


