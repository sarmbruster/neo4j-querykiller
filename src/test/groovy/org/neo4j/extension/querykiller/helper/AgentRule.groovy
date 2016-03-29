package org.neo4j.extension.querykiller.helper

import com.ea.agentloader.AgentLoader
import org.junit.rules.ExternalResource


/**
 * @author Stefan Armbruster
 */
class AgentRule extends ExternalResource {
    final Class agentClass

    AgentRule(Class agentClass) {
        this.agentClass = agentClass
    }

    @Override
    protected void before() throws Throwable {
        AgentLoader.loadAgentClass(agentClass.name, null);
    }
}
