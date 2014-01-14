package org.neo4j.extension.querykiller.statistics;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class QueryStat implements Comparable<QueryStat> {

    private final SortedMap<Date, Long> durations = new TreeMap<>();
    private long total = 0;

    public SortedMap<Date, Long> getDurations() {
        return durations;
    }

    public long getTotal() {
        return total;
    }

    public void add(Date started, Long l) {
        durations.put(started, l);
        total += l;
    }

    @Override
    public int compareTo(QueryStat other) {
        Long total1 = this.getTotal();
        Long total2 = other.getTotal();
        return total1.compareTo(total2);
    }
}
