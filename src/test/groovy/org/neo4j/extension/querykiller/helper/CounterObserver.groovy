package org.neo4j.extension.querykiller.helper

import groovy.util.logging.Slf4j

/**
 * @author Stefan Armbruster
 */
@Slf4j
class CounterObserver implements Observer {

    def counters = [:].withDefault { 0 }

    @Override
    void update(Observable obs, Object o) {
        synchronized (o.class) {  // this get concurrently called from different threads -> synchronized is crucial
            counters[o.class]++
            log.info("incrementing for ${o.class}: ${counters[o.class]}")
        }
    }
}