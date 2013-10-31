package org.neo4j.extension.querykiller.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

/**
 * A HttpServletRequest allowing multiple calls to getReader/getInputStream
 * N.B. this consumes more memory since the contents of the underlying stream will be stored in a byte[]. In case of
 * huge request sizes, this might be harmful.
 */
public class CopyHttpServletRequest extends HttpServletRequestWrapper
{

    private byte[] requestBody;

    public CopyHttpServletRequest( HttpServletRequest request )
    {
        super(request);
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        populateRequestBody();
        return new BufferedReader( new InputStreamReader( new ByteArrayInputStream( requestBody ) ) );
    }


    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        populateRequestBody();
        return new ServletInputStreamWrapper(new ByteArrayInputStream( requestBody ));
    }

    private void populateRequestBody() throws IOException
    {
        if (requestBody==null) {
            requestBody = IOUtils.toByteArray( super.getReader() );
        }
    }
}
