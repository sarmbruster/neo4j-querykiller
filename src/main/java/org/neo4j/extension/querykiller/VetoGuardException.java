package org.neo4j.extension.querykiller;

import org.neo4j.kernel.guard.GuardException;

public class VetoGuardException extends GuardException {

    VetoGuardException(String message) {
        super(message);
    }

}
