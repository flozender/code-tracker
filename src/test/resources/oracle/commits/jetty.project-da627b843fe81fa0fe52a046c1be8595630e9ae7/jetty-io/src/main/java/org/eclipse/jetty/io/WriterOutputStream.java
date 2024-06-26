// ========================================================================
// Copyright (c) 1999-2009 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;


/* ------------------------------------------------------------ */
/** Wrap a Writer as an OutputStream.
 * When all you have is a Writer and only an OutputStream will do.
 * Try not to use this as it indicates that your design is a dogs
 * breakfast (JSP made me write it).
 * 
 */
public class WriterOutputStream extends OutputStream
{
    protected Writer _writer;
    protected String _encoding;
    private byte[] _buf=new byte[1];
    
    /* ------------------------------------------------------------ */
    public WriterOutputStream(Writer writer, String encoding)
    {
        _writer=writer;
        _encoding=encoding;
    }
    
    /* ------------------------------------------------------------ */
    public WriterOutputStream(Writer writer)
    {
        _writer=writer;
    }

    /* ------------------------------------------------------------ */
    public void close()
        throws IOException
    {
        _writer.close();
        _writer=null;
        _encoding=null;
    }
    
    /* ------------------------------------------------------------ */
    public void flush()
        throws IOException
    {
        _writer.flush();
    }
    
    /* ------------------------------------------------------------ */
    public void write(byte[] b) 
        throws IOException
    {
        if (_encoding==null)
            _writer.write(new String(b));
        else
            _writer.write(new String(b,_encoding));
    }
    
    /* ------------------------------------------------------------ */
    public void write(byte[] b, int off, int len)
        throws IOException
    {
        if (_encoding==null)
            _writer.write(new String(b,off,len));
        else
            _writer.write(new String(b,off,len,_encoding));
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void write(int b)
        throws IOException
    {
        _buf[0]=(byte)b;
        write(_buf);
    }
}

