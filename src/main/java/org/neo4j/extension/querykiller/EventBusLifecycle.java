package org.neo4j.extension.querykiller;

import com.google.common.eventbus.EventBus;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends a Guava's {@link EventBus} as a Neo4j component to be injected from other modules
 * @author Stefan Armbruster
 */
public class EventBusLifecycle extends EventBus implements DefaultLifecycle {

    final Logger logger = LoggerFactory.getLogger(EventBusLifecycle.class);

    private final EventBusExtensionFactory.Dependencies dependencies;

    public EventBusLifecycle(EventBusExtensionFactory.Dependencies dependencies) {
        super();
        this.dependencies = dependencies;
    }

    @Override
    public void start() throws Throwable {
        // since we're in the same project as the agent modifying ThreadToStatementContextBridge's bytecode
        // we need to use reflection to set the eventBus instance
        setEventBusOn(dependencies.getThreadToStatementContextBridge());
        setEventBusOn(dependencies.getQueryExecutionEngine());
    }

    private void setEventBusOn(Object object) throws Throwable {
        try {
            MethodUtils.invokeExactMethod(object, "setEventBus", new Object[] {this}, new Class[] {EventBus.class});
        } catch (NoSuchMethodException e) {
            logger.warn("could not hook into " + object.getClass().getSimpleName() +
                     ". Most likely the agent 'WrapNeo4jComponentsAgent' is not activated." );
//            throw new RuntimeException("could not hook into ThreadToStatementContextBridge. Most likely the agent is not activated", e);

        }

    }
}
