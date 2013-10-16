package org.neo4j.extension.querykiller

import org.neo4j.kernel.impl.transaction.xaframework.ForceMode
import org.neo4j.server.CommunityNeoServer
import org.neo4j.server.configuration.Configurator
import org.neo4j.server.configuration.PropertyFileConfigurator
import org.neo4j.server.configuration.validation.DatabaseLocationMustBeSpecifiedRule
import org.neo4j.server.configuration.validation.Validator
import org.neo4j.server.database.CommunityDatabase
import org.neo4j.server.database.Database
import org.neo4j.server.database.EphemeralDatabase
import org.neo4j.server.helpers.ServerBuilder
import org.neo4j.server.preflight.PreFlightTasks
import org.neo4j.server.rest.paging.LeaseManager
import org.neo4j.server.rest.web.DatabaseActions
import org.neo4j.tooling.Clock
import org.neo4j.tooling.RealClock

class ConfigurableServerBuilder extends ServerBuilder {

    final Map<String,String> config = [:]

    ConfigurableServerBuilder withConfigProperty(String key, String value) {
        config[key] = value
        this
    }

    /**
     * mostly a copy of {@link ServerBuilder#build()} but allowing to tweak graphdb's config by hooking into getDbTuningPropertiesWithServerDefaults
     * @return
     * @throws IOException
     */
    @Override
    CommunityNeoServer build() throws IOException {

        if (preflightTasks == null) {
            preflightTasks = new PreFlightTasks(null) {
                @Override
                public boolean run() {
                    return true;
                }
            };
        }
        File configFile = createPropertiesFiles();
        return new CommunityNeoServer(new PropertyFileConfigurator(new Validator(new DatabaseLocationMustBeSpecifiedRule()), configFile)) {

            Map graphDbConfig = config

            @Override
            protected PreFlightTasks createPreflightTasks() {
                return preflightTasks;
            }

            @Override
            protected Database createDatabase() {
                return persistent ?
                    new CommunityDatabase(configurator) {
                        @Override
                        protected Map<String, String> getDbTuningPropertiesWithServerDefaults() {
                            Map map =super.getDbTuningPropertiesWithServerDefaults()
                            map.putAll(graphDbConfig)
                            map
                        }
                    } :
                    new EphemeralDatabase(configurator) {
                        @Override
                        protected Map<String, String> getDbTuningPropertiesWithServerDefaults() {
                            Map map =super.getDbTuningPropertiesWithServerDefaults()
                            map.putAll(graphDbConfig)
                            map
                        }
                    };
            }

            @Override
            protected DatabaseActions createDatabaseActions() {
                Clock clockToUse = (clock != null) ? clock : new RealClock();

                return new DatabaseActions(
                        database,
                        new LeaseManager(clockToUse),
                        ForceMode.forced,
                        configurator.configuration().getBoolean(
                                Configurator.SCRIPT_SANDBOXING_ENABLED_KEY,
                                Configurator.DEFAULT_SCRIPT_SANDBOXING_ENABLED));
            }

        }

    }

}
