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

    private checkMethod(String className, String methodName, String... paramClassNames) {
        def clazz = Class.forName(className)
        List<Class> paramClasses = paramClassNames.collect { Class.forName(it) }
        def method = clazz.getDeclaredMethod( methodName, *paramClasses)
        assert method
    }

    private checkConstructor(String className, String... paramClassNames) {
        def clazz = Class.forName(className)
        List<Class> paramClasses = paramClassNames.collect { Class.forName(it) }
        def constructor = clazz.getDeclaredConstructor(*paramClasses)
        assert constructor
    }

    def "verify Neo4j API is ready for instrumentation"() {

        when:
        checkMethod("org.neo4j.kernel.impl.core.ThreadToStatementContextBridge", "bindTransactionToCurrentThread",
                "org.neo4j.kernel.api.KernelTransaction")
        checkMethod("org.neo4j.kernel.impl.core.ThreadToStatementContextBridge", "unbindTransactionFromCurrentThread")
        checkMethod("org.neo4j.cypher.internal.javacompat.ExecutionEngine", "executeQuery", "java.lang.String", "java.util.Map", "org.neo4j.kernel.impl.query.QuerySession")
        checkMethod("org.neo4j.cypher.internal.javacompat.ExecutionEngine", "profileQuery", "java.lang.String", "java.util.Map", "org.neo4j.kernel.impl.query.QuerySession")
        checkMethod("org.neo4j.bolt.v1.runtime.internal.SessionStateMachine", "run", "java.lang.String", "java.util.Map", "java.lang.Object", 'org.neo4j.bolt.v1.runtime.Session$Callback')
        checkMethod("org.neo4j.bolt.v1.runtime.internal.SessionStateMachine", "pullAll", "java.lang.Object", 'org.neo4j.bolt.v1.runtime.Session$Callback')

        checkConstructor("org.neo4j.bolt.v1.runtime.internal.SessionStateMachine",
                "java.lang.String",
                "org.neo4j.udc.UsageData",
                "org.neo4j.kernel.impl.factory.GraphDatabaseFacade",
                "org.neo4j.kernel.impl.core.ThreadToStatementContextBridge",
                "org.neo4j.bolt.v1.runtime.spi.StatementRunner",
                "org.neo4j.kernel.impl.logging.LogService",
                "org.neo4j.bolt.security.auth.Authentication"
        )

        //public SessionStateMachine( String connectionDescriptor, UsageData usageData, GraphDatabaseFacade db, ThreadToStatementContextBridge txBridge,
//                    StatementRunner engine, LogService logging, Authentication authentication )

        then:
        noExceptionThrown()
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


