package org.neo4j.extension.querykiller;


import javax.xml.bind.annotation.*;
import java.util.Date;
import java.util.zip.CRC32;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class QueryRegistryEntry implements Comparable {

    private String key;
    private String cypher;
    private String remoteUser;
    private String remoteHost;
    private String endPoint;
    private Date started = new Date();
    private long thread = Thread.currentThread().getId();
    private VetoGuard vetoGuard;

    public QueryRegistryEntry() {
    }

    public QueryRegistryEntry( VetoGuard vetoGuard, String cypher, String endPoint, String remoteHost,
                               String remoteUser )
    {
        this.vetoGuard = vetoGuard;
        this.cypher = cypher.replace( "\n", "" ).trim();
        this.key = calculateKey();
        this.endPoint = endPoint;
        this.remoteHost = remoteHost;
        this.remoteUser = remoteUser;
    }

    public void setKey(String key) {
        this.key = key;
    }

    // key is lazy
    public String getKey() {
        if (key == null) {
            key = calculateKey();
        }
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

    public String getRemoteUser()
    {
        return remoteUser;
    }

    public void setRemoteUser( String remoteUser )
    {
        this.remoteUser = remoteUser;
    }

    public String getRemoteHost()
    {
        return remoteHost;
    }

    public void setRemoteHost( String remoteHost )
    {
        this.remoteHost = remoteHost;
    }

    public String getEndPoint()
    {
        return endPoint;
    }

    public void setEndPoint( String endPoint )
    {
        this.endPoint = endPoint;
    }

    @XmlTransient
    public VetoGuard getVetoGuard() {
        return vetoGuard;
    }

    public void setVetoGuard(VetoGuard vetoGuard) {
        this.vetoGuard = vetoGuard;
    }


    private String calculateKey() {
        // TODO: find better key, e.g. md5
        CRC32 crc = new CRC32();
        crc.update( (int) thread );
        crc.update( (int) started.getTime() );

        //StringBuilder sb = new StringBuilder().append( thread ).append("_").append(started.getTime());
        return Long.toString( crc.getValue() );
    }

    @Override
    public int compareTo(Object o) {
        return started.compareTo(((QueryRegistryEntry) o).started);
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !(o instanceof QueryRegistryEntry) )
        {
            return false;
        }

        QueryRegistryEntry that = (QueryRegistryEntry) o;

        if ( thread != that.thread )
        {
            return false;
        }
        if ( !cypher.equals( that.cypher ) )
        {
            return false;
        }
        if ( endPoint != null ? !endPoint.equals( that.endPoint ) : that.endPoint != null )
        {
            return false;
        }
        if ( key != null ? !key.equals( that.key ) : that.key != null )
        {
            return false;
        }
        if ( remoteHost != null ? !remoteHost.equals( that.remoteHost ) : that.remoteHost != null )
        {
            return false;
        }
        if ( remoteUser != null ? !remoteUser.equals( that.remoteUser ) : that.remoteUser != null )
        {
            return false;
        }
        if ( !started.equals( that.started ) )
        {
            return false;
        }
        if ( vetoGuard != null ? !vetoGuard.equals( that.vetoGuard ) : that.vetoGuard != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + cypher.hashCode();
        result = 31 * result + (remoteUser != null ? remoteUser.hashCode() : 0);
        result = 31 * result + (remoteHost != null ? remoteHost.hashCode() : 0);
        result = 31 * result + (endPoint != null ? endPoint.hashCode() : 0);
        result = 31 * result + started.hashCode();
        result = 31 * result + (int) (thread ^ (thread >>> 32));
        result = 31 * result + (vetoGuard != null ? vetoGuard.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "QueryRegistryEntry{" +
                "thread=" + thread +
                ", key='" + key + '\'' +
                ", cypher='" + cypher + '\'' +
                ", remoteUser='" + remoteUser + '\'' +
                ", remoteHost='" + remoteHost + '\'' +
                ", endPoint='" + endPoint + '\'' +
                ", started=" + started +
                '}';
    }

    public String formatAsTable()
    {
        long duration = System.currentTimeMillis() - started.getTime();
        return String.format( "| %7d | %10s | %-60.60s | %-15.15s | %-15.15s |",
                duration,
                getKey(),
                getCypher(),
                getRemoteHost(),
                getEndPoint()
        );
    }
}