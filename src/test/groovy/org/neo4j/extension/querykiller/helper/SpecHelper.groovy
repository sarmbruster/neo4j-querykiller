package org.neo4j.extension.querykiller.helper

import java.util.concurrent.TimeoutException

/**
 * @author Stefan Armbruster
 */
class SpecHelper {

    static void sleepUntil(Closure closure) {
        long started = System.currentTimeMillis()
        while (closure.call() == false) {
            sleep 5;
            if ((System.currentTimeMillis()-started) > 10*1000) {
                closure.owner.log.error("timeout in sleepUntil")
                throw new TimeoutException("timeout in sleepUntil")
            }
        }
    }

}
