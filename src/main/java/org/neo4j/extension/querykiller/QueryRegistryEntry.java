package org.neo4j.extension.querykiller;


import javax.xml.bind.annotation.*;
import java.util.Date;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class QueryRegistryEntry implements Comparable {

    private String key;
    private String cypher;
    private Date started = new Date();
    private long thread = Thread.currentThread().getId();
    private VetoGuard vetoGuard;

    public QueryRegistryEntry() {
    }

    public QueryRegistryEntry(String cypher, VetoGuard vetoGuard) {
        this.cypher = cypher;
        this.vetoGuard = vetoGuard;
        this.key = calculateKey();
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {

        return key;
    }

    public String getCypher() {
        return cypher;
    }

    public void setCypher(String cypher) {
        this.cypher = cypher;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public long getThread() {
        return thread;
    }

    public void setThread(long thread) {
        this.thread = thread;
    }

    @XmlTransient
    public VetoGuard getVetoGuard() {
        return vetoGuard;
    }

    public void setVetoGuard(VetoGuard vetoGuard) {
        this.vetoGuard = vetoGuard;
    }

    @Override
    public boolean equals(Object o) {
        if (null == o) return true;
        if (!(o instanceof QueryRegistryEntry)) return false;

        QueryRegistryEntry that = (QueryRegistryEntry) o;

        if (!cypher.equals(that.cypher)) return false;
        if (!started.equals(that.started)) return false;
        if (thread != that.thread) return false;
        if (!vetoGuard.equals(that.vetoGuard)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = cypher.hashCode();
        result = 31 * result + started.hashCode();
        result = 31 * result + (int)thread;
        result = 31 * result + vetoGuard.hashCode();
        return result;
    }

    private String calculateKey() {
        // TODO: find better key, e.g. md5
        StringBuilder sb = new StringBuilder().append( thread ).append("_").append(started.getTime());
        return sb.toString();
    }

    @Override
    public int compareTo(Object o) {
        return started.compareTo(((QueryRegistryEntry) o).started);
    }

    @Override
    public String toString() {
        return "QueryMapEntry{" +
                "cypher='" + cypher + '\'' +
                ", started=" + started +
                ", thread=" + thread +
                ", vetoGuard=" + vetoGuard +
                '}';
    }
}