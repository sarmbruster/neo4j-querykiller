package org.neo4j.extension.querykiller.http;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletInputStream;

public class ServletInputStreamWrapper extends ServletInputStream
{

    private InputStream delegate;

    public ServletInputStreamWrapper( InputStream inputStream )
    {
        this.delegate = inputStream;
    }

    @Override
    public int read() throws IOException
    {
        return delegate.read();
    }
}
