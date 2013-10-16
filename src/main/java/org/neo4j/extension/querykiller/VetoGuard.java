package org.neo4j.extension.querykiller;

import org.neo4j.kernel.guard.Guard;

class VetoGuard implements Guard.GuardInternal {

    boolean abort = false;

    @Override
    public void check() {
        if (abort) {
            StringBuilder msg = new StringBuilder("aborted query for thread ");
            msg.append(Thread.currentThread().toString());
            throw new VetoGuardException(msg.toString());
        }
    }

    boolean isAbort() {
        return abort;
    }

    void setAbort(boolean abort) {
        this.abort = abort;
    }
}
