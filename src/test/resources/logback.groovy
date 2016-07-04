import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.*

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %r [%thread] %-5level %logger{36} - %msg%n"
    }
}

logger("org.neo4j.extension", DEBUG)
root(INFO, ["CONSOLE"])