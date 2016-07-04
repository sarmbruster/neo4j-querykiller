package org.neo4j.extension.querykiller.agent;

import javassist.*;
import javassist.NotFoundException;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;

/**
 * @author Stefan Armbruster
 */
public class WrapNeo4jComponentsAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentNeo4jClasses(inst);
    }

    private static void instrumentNeo4jClasses(Instrumentation inst) {
        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            ClassPool pool = ClassPool.getDefault();
            try {
                switch (className) {
                    case "org/neo4j/kernel/impl/core/ThreadToStatementContextBridge":
                        CtClass threadToStatementContextBridge = pool.get("org.neo4j.kernel.impl.core.ThreadToStatementContextBridge");

                        addEventBusField(threadToStatementContextBridge);

                        CtMethod bindTransactionToCurrentThread = threadToStatementContextBridge.getDeclaredMethod("bindTransactionToCurrentThread");
                        bindTransactionToCurrentThread.insertAfter("if (eventBus!=null) { eventBus.post(new org.neo4j.extension.querykiller.events.bind.BindTransactionEvent($1));}");

                        CtMethod unbindTransactionFromCurrentThread = threadToStatementContextBridge.getDeclaredMethod("unbindTransactionFromCurrentThread");
                        unbindTransactionFromCurrentThread.insertBefore("if (eventBus!=null) { eventBus.post(new org.neo4j.extension.querykiller.events.bind.UnbindTransactionEvent((org.neo4j.kernel.api.KernelTransaction)(threadToTransactionMap.get())));}");

                        return byteCodeOf(threadToStatementContextBridge);

                    case "org/neo4j/cypher/internal/javacompat/ExecutionEngine":
                        CtClass executionEngine = pool.get("org.neo4j.cypher.internal.javacompat.ExecutionEngine");
                        addEventBusField(executionEngine);

                        String setContext = "if (eventBus!=null) {eventBus.post(new org.neo4j.extension.querykiller.events.cypher.CypherContext($1, $2));}";
                        String unsetContext = "if (eventBus!=null) {eventBus.post(new org.neo4j.extension.querykiller.events.cypher.ResetCypherContext());}";
                        executionEngine.getDeclaredMethod("executeQuery").insertBefore(setContext);
                        executionEngine.getDeclaredMethod("executeQuery").insertAfter(unsetContext, true);
                        executionEngine.getDeclaredMethod("profileQuery").insertBefore(setContext);
                        executionEngine.getDeclaredMethod("profileQuery").insertAfter(unsetContext, true);
                        return byteCodeOf(executionEngine);

                    case "org/neo4j/bolt/v1/runtime/internal/SessionStateMachine":
                        CtClass sessionStateMachine = pool.get("org.neo4j.bolt.v1.runtime.internal.SessionStateMachine");
                        addEventBusField(sessionStateMachine);

                        // populate eventBus field in constructor
                        // grab the constructor holding a GraphDatabaseAPI instance and amend code to that one
                        Pair<CtConstructor, Integer> constructorAndPosition = findConstructorAndPositionForGraphDatabaseAPI(sessionStateMachine);
                        String codeBlock = String.format("{setEventBus((com.google.common.eventbus.EventBus)$%d.getDependencyResolver().resolveDependency(org.neo4j.extension.querykiller.EventBusLifecycle.class));}", constructorAndPosition.other() );
                        constructorAndPosition.first().insertAfter(codeBlock);


                        String setBoltContext = "if (eventBus!=null) {eventBus.post(new org.neo4j.extension.querykiller.events.transport.BoltContext(connectionDescriptor()));}";
                        String unsetBoltContext = "if (eventBus!=null) {eventBus.post(new org.neo4j.extension.querykiller.events.transport.ResetBoltContext());}";
                        sessionStateMachine.getDeclaredMethod("run").insertBefore(setBoltContext);
                        sessionStateMachine.getDeclaredMethod("pullAll").insertAfter(unsetBoltContext, true);
                        //sessionWorkerFacade.getDeclaredMethod("discardAll").insertAfter(unsetBoltContext, true);  // maybe this is needed as well
                        return byteCodeOf(sessionStateMachine);

                    default: // don't intercept any other class
                        return null;
                }
            } catch (NotFoundException|CannotCompileException|IOException  e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    private static Pair<CtConstructor, Integer> findConstructorAndPositionForGraphDatabaseAPI(CtClass clazz) throws NotFoundException {
        for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
            CtClass[] parameterTypes = constructor.getParameterTypes();
            for (int position = 0; position < parameterTypes.length; position++) {
                for (CtClass iface: parameterTypes[position].getInterfaces()) {
                    if (iface.getName().equals(GraphDatabaseAPI.class.getName())) {
                        return Pair.of(constructor, position+1);
                    }
                }
            }
        }
        throw new NotFoundException("could not find constructor providing a GraphDatabaseAPI");
    }

    private static byte[] byteCodeOf(CtClass clazz) throws IOException, CannotCompileException {
        byte[] byteCode = clazz.toBytecode();
        clazz.detach();
        return byteCode;
    }

    private static void addEventBusField(CtClass clazz) throws CannotCompileException {
        CtField field = CtField.make("private com.google.common.eventbus.EventBus eventBus;", clazz);
        field.setModifiers(	4096 | Modifier.PRIVATE);  // Modifier.SYNTHETIC | PRIVATE
        clazz.addField(field); // TODO: consider using a synthetic field
        clazz.addMethod(CtMethod.make("public com.google.common.eventbus.EventBus getEventBus() { return eventBus;}", clazz));
        clazz.addMethod(CtMethod.make("public void setEventBus(com.google.common.eventbus.EventBus eventBus) { this.eventBus=eventBus;}", clazz));
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        instrumentNeo4jClasses(inst);
    }
}
