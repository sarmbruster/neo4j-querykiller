package org.neo4j.extension.querykiller.server;

import org.neo4j.server.plugins.Injectable;

public class InjectableAdapter<T> implements Injectable<T> {

    private T instance;

    public InjectableAdapter(T instance) {
        this.instance = instance;
    }

    @Override
    public T getValue() {
        return instance;
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) instance.getClass();
    }
}
