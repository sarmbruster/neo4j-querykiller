package org.neo4j.extension.querykiller.http;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ReadListener;
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

    @Override
    public boolean isFinished() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReady() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException();
    }
}
