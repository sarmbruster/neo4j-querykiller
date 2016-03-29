package org.neo4j.extension.querykiller;

import org.neo4j.kernel.api.KernelTransaction;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * holds information about a {@link KernelTransaction}, which thread it is bound to, when it has been started
 * and provides a {@link #key} used for killing it.
 */
public class TransactionEntry implements Comparable {

    private final Date started = new Date();
    private final long threadId = Thread.currentThread().getId();
    private final KernelTransaction kernelTransaction;
    private boolean killed = false;
    private String key;

    public TransactionEntry(KernelTransaction kernelTransaction) {
        this.kernelTransaction = kernelTransaction;
    }

    // key is lazy
    public String getKey() {
        if (key == null) {
            key = calculateKey();
        }
        return key;
    }

    public long getThreadId() {
        return threadId;
    }

    public Date getStarted() {
        return started;
    }

    public KernelTransaction getKernelTransaction() {
        return kernelTransaction;
    }

    public boolean isKilled() {
        return killed;
    }

    public void kill() {
        kernelTransaction.markForTermination();
        killed = true;
    }

    private String calculateKey() {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-1");
            String toDigest = String.format("%d, %d", threadId, started.getTime());
            hash.update(toDigest.getBytes());
            return new HexBinaryAdapter().marshal(hash.digest()).substring(0,8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int compareTo(Object o) {
        if ( this==o) {
            return 0;
        }

        if (!(o instanceof TransactionEntry)) {
            throw new IllegalStateException("cannot compare QueryRegistryEntry to " + o.getClass().getName());
        }
        TransactionEntry other = (TransactionEntry) o;

        int result = started.compareTo(other.started);
        if (result != 0 ) return result;

        return getKey().compareTo(other.getKey());
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !(o instanceof TransactionEntry) )
        {
            return false;
        }

        TransactionEntry that = (TransactionEntry) o;

        if (  !(threadId == that.threadId))
        {
            return false;
        }
        if ( !started.equals( that.started ) )
        {
            return false;
        }
        if ( kernelTransaction != null ? !kernelTransaction.equals( that.kernelTransaction ) : that.kernelTransaction != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + started.hashCode();
        result = 31 * result + (int) (threadId ^ (threadId >>> 32));
        result = 31 * result + (kernelTransaction != null ? kernelTransaction.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "QueryRegistryEntry{" +
                "thread=" + threadId +
                ", key='" + key + "'" +
                ", started=" + started +
                '}';
    }

}