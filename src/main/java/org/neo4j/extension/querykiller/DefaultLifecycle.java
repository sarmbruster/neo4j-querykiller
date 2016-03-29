package org.neo4j.extension.querykiller;

import org.neo4j.kernel.lifecycle.Lifecycle;

/**
 * provide default implementations for {@link Lifecycle}
 * @author Stefan Armbruster
 */
public interface DefaultLifecycle extends Lifecycle {

    @Override
    default void init() throws Throwable {
    }

    @Override
    default void start() throws Throwable {
    }

    @Override
    default void stop() throws Throwable {
    }

    @Override
    default void shutdown() throws Throwable {
    }
}
