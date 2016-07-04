package org.neo4j.extension.querykiller.helper

import com.google.common.eventbus.Subscribe
import groovy.util.logging.Slf4j

/**
 * @author Stefan Armbruster
 */
@Slf4j
class EventCounters {

    def counters = [:].withDefault { 0 }

    @Subscribe
    public void handleEvent(Object o) {
        synchronized (o.class) {  // this gets concurrently called from different threads -> synchronized is crucial
            counters[o.class]++
            log.error("incrementing for ${o.class.simpleName}: ${counters[o.class]}")
/*            counters.each {k,v -> log.debug("${k}: $v")
            }*/
        }
    }

    public void reset() {
        counters = [:].withDefault {0}
    }
}