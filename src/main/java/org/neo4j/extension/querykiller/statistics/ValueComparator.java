package org.neo4j.extension.querykiller.statistics;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by stefan on 18.12.13.
 */
public class ValueComparator implements Comparator<String> {

    private Map<String, QueryStat> map;

    public ValueComparator(Map<String, QueryStat> map) {
        this.map = map;
    }

    @Override
    public int compare(String s1, String s2) {
        return map.get(s1).compareTo(map.get(s2));
    }
}
