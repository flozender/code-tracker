// ========================================================================
// Copyright (c) 2004-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================


package org.eclipse.jetty.io.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.EndPoint;

/**
 * 
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class StreamEndPoint implements EndPoint
{
    InputStream _in;
    OutputStream _out;

    /**
     * 
     */
    public StreamEndPoint(InputStream in, OutputStream out)
    {
        _in=in;
        _out=out;
    }
    
    public boolean isBlocking()
    {
        return true;
    }

    public boolean blockReadable(long millisecs) throws IOException
    {
        return true;
    }
    
    public boolean blockWritable(long millisecs) throws IOException
    {
        return true;
    }

    /* 
     * @see org.eclipse.io.BufferIO#isOpen()
     */
    public boolean isOpen()
    {
        return _in!=null;
    }

    /* 
     * @see org.eclipse.io.BufferIO#isOpen()
     */
    public final boolean isClosed()
    {
        return !isOpen();
    }

    /* 
     * @see org.eclipse.io.BufferIO#close()
     */
    public void close() throws IOException
    {
        if (_in!=null)
            _in.close();
        _in=null;
        if (_out!=null)
            _out.close();
        _out=null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.io.BufferIO#fill(org.eclipse.io.Buffer)
     */
    public int fill(Buffer buffer) throws IOException
    {
        // TODO handle null array()
        if (_in==null)
            return 0;
            
    	int space=buffer.space();
    	if (space<=0)
    	{
    	    if (buffer.hasContent())
    	        return 0;
    	    throw new IOException("FULL");
    	}
        
        int len = buffer.readFrom(_in,space);
    
    	return len;
    }

    /* (non-Javadoc)
     * @see org.eclipse.io.BufferIO#flush(org.eclipse.io.Buffer)
     */
    public int flush(Buffer buffer) throws IOException
    {
        // TODO handle null array()
        if (_out==null)
            return -1;
        int length=buffer.length();
        if (length>0)
            buffer.writeTo(_out);
        buffer.clear();
        return length;
    }

    /* (non-Javadoc)
     * @see org.eclipse.io.BufferIO#flush(org.eclipse.io.Buffer, org.eclipse.io.Buffer, org.eclipse.io.Buffer)
     */
    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
    {
        int len=0;
        
        // TODO  consider copying buffer and trailer into header if there is space.
        
        
        if (header!=null)
        {
            int tw=header.length();
            if (tw>0)
            {
                int f=flush(header);
                len=f;
                if (f<tw)
                    return len;
            }
        }
        
        if (buffer!=null)
        {
            int tw=buffer.length();
            if (tw>0)
            {
                int f=flush(buffer);
                if (f<0)
                    return len>0?len:f;
                len+=f;
                if (f<tw)
                    return len;
            }
        }
        
        if (trailer!=null)
        {
            int tw=trailer.length();
            if (tw>0)
            {
                int f=flush(trailer);
                if (f<0)
                    return len>0?len:f;
                len+=f;
            }
        }
        return len;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.io.EndPoint#getLocalAddr()
     */
    public String getLocalAddr()
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.io.EndPoint#getLocalHost()
     */
    public String getLocalHost()
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.io.EndPoint#getLocalPort()
     */
    public int getLocalPort()
    {
        return 0;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.io.EndPoint#getRemoteAddr()
     */
    public String getRemoteAddr()
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.io.EndPoint#getRemoteHost()
     */
    public String getRemoteHost()
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.io.EndPoint#getRemotePort()
     */
    public int getRemotePort()
    {
        return 0;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.io.EndPoint#getConnection()
     */
    public Object getTransport()
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    public InputStream getInputStream()
    {
        return _in;
    }

    /* ------------------------------------------------------------ */
    public void setInputStream(InputStream in)
    {
        _in=in;
    }

    /* ------------------------------------------------------------ */
    public OutputStream getOutputStream()
    {
        return _out;
    }

    /* ------------------------------------------------------------ */
    public void setOutputStream(OutputStream out)
    {
        _out=out;
    }


    /* ------------------------------------------------------------ */
    public void flush()
        throws IOException
    {   
        _out.flush();
    }

    /* ------------------------------------------------------------ */
    public boolean isBufferingInput()
    {
        return false;
    }

    /* ------------------------------------------------------------ */
    public boolean isBufferingOutput()
    {
        return false;
    }

    /* ------------------------------------------------------------ */
    public boolean isBufferred()
    {
        return false;
    }

}
