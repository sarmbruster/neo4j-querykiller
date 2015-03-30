package org.neo4j.extension.querykiller.helper

import java.util.logging.LogManager

/**
 * set -Djava.util.logging.config.class=org.neo4j.extension.querykiller.helper.JULConfigurator
 * to mute java.util.logging when running tests
 */
class JULConfigurator {

    public JULConfigurator() {
        LogManager.logManager.readConfiguration(Thread.currentThread().contextClassLoader.getResourceAsStream("logging.properties"))
    }
}
