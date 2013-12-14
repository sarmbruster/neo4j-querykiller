package org.neo4j.extension.querykiller

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.server.NeoServer
import spock.lang.Shared
import spock.lang.Specification

/**
 * abstract base class for spock tests using a NeoServer
 */
abstract class NeoServerSpecification extends Specification {

//    public static final String MOUNTPOINT = "/db/uuid"
    @Shared GraphDatabaseService graphDB
    @Shared NeoServer server

    def setupSpec() {
        def serverBuilder = new ConfigurableServerBuilder()
                .withConfigProperty("execution_guard_enabled", "true")
                .withAutoIndexingEnabledForNodes("dummy")
                .onPort(37474)

        thirdPartyJaxRsPackages().each { packageName, mountpoint ->
            serverBuilder.withThirdPartyJaxRsPackage(packageName, mountpoint)
        }
        server = serverBuilder.build()
        server.start()
        graphDB = server.database.graph
    }

    abstract Map thirdPartyJaxRsPackages()

    def cleanupSpec() {
        server.stop()
    }

    def withTransaction(Closure closure) {
        def tx = graphDB.beginTx()
        try {
            def result = closure.call()
            tx.success()
            return result
        } finally {
            tx.finish()
        }
    }

}
