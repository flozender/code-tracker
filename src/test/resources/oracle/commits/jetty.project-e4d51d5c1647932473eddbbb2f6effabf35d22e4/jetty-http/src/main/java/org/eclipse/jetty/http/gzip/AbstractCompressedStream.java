//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http.gzip;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.zip.DeflaterOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.ByteArrayOutputStream2;

/* ------------------------------------------------------------ */
/**
 * Skeletal implementation of a CompressedStream. This class adds compression features to a ServletOutputStream and takes care of setting response headers, etc.
 * Major work and configuration is done here. Subclasses using different kinds of compression only have to implement the abstract methods doCompress() and
 * setContentEncoding() using the desired compression and setting the appropriate Content-Encoding header string.
 */
public abstract class AbstractCompressedStream extends ServletOutputStream 
{
    private final String _encoding;
    protected final CompressedResponseWrapper _wrapper;
    protected final HttpServletResponse _response;
    protected OutputStream _out;
    protected ByteArrayOutputStream2 _bOut;
    protected DeflaterOutputStream _compressedOutputStream;
    protected boolean _closed;
    protected boolean _doNotCompress;

    /**
     * Instantiates a new compressed stream.
     * 
     */
    public AbstractCompressedStream(String encoding,HttpServletRequest request, CompressedResponseWrapper wrapper)
            throws IOException
    {
        _encoding=encoding;
        _wrapper = wrapper;
        _response = (HttpServletResponse)wrapper.getResponse();
        
        if (_wrapper.getMinCompressSize()==0)
            doCompress();
    }

    /**
     * Reset buffer.
     */
    public void resetBuffer()
    {
        if (_response.isCommitted())
            throw new IllegalStateException("Committed");
        _closed = false;
        _out = null;
        _bOut = null;
        if (_compressedOutputStream != null)
            _response.setHeader("Content-Encoding",null);
        _compressedOutputStream = null;
        _doNotCompress = false;
    }

    /* ------------------------------------------------------------ */
    public void setContentLength()
    {
        if (_doNotCompress)
        {
            long length=_wrapper.getContentLength();
            if (length>=0)
            {
                if (length < Integer.MAX_VALUE)
                    _response.setContentLength((int)length);
                else
                    _response.setHeader("Content-Length",Long.toString(length));
            }
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException
    {
        if (_out == null || _bOut != null)
        {
            long length=_wrapper.getContentLength();
            if (length > 0 && length < _wrapper.getMinCompressSize())
                doNotCompress();
            else
                doCompress();
        }

        _out.flush();
    }

    /* ------------------------------------------------------------ */
    /**
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException
    {
        if (_closed)
            return;

        if (_wrapper.getRequest().getAttribute("javax.servlet.include.request_uri") != null)
            flush();
        else
        {
            if (_bOut != null)
            {
                long length=_wrapper.getContentLength();
                if (length < 0)
                {
                    length = _bOut.getCount();
                    _wrapper.setContentLength(length);
                }
                if (length < _wrapper.getMinCompressSize())
                    doNotCompress();
                else
                    doCompress();
            }
            else if (_out == null)
            {
                doNotCompress();
            }

            if (_compressedOutputStream != null)
                _compressedOutputStream.close();
            else
                _out.close();
            _closed = true;
        }
    }

    /**
     * Finish.
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void finish() throws IOException
    {
        if (!_closed)
        {
            if (_out == null || _bOut != null)
            {
                long length=_wrapper.getContentLength();
                if (length > 0 && length < _wrapper.getMinCompressSize())
                    doNotCompress();
                else
                    doCompress();
            }

            if (_compressedOutputStream != null && !_closed)
            {
                _closed = true;
                _compressedOutputStream.close();
            }
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException
    {
        checkOut(1);
        _out.write(b);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(byte b[]) throws IOException
    {
        checkOut(b.length);
        _out.write(b);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte b[], int off, int len) throws IOException
    {
        checkOut(len);
        _out.write(b,off,len);
    }
    
    /**
     * Do compress.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void doCompress() throws IOException
    {
        if (_compressedOutputStream==null) 
        {
            if (_response.isCommitted())
                throw new IllegalStateException();
            
            setHeader("Content-Encoding", _encoding);            
            if (_response.containsHeader("Content-Encoding"))
            {
                _out=_compressedOutputStream=createStream();

                if (_bOut!=null)
                {
                    _out.write(_bOut.getBuf(),0,_bOut.getCount());
                    _bOut=null;
                }
                
                String etag=_wrapper.getETag();
                if (etag!=null)
                    setHeader("ETag",etag.substring(0,etag.length()-1)+'-'+_encoding+'"');
            }
            else 
                doNotCompress();
        }
    }

    /**
     * Do not compress.
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void doNotCompress() throws IOException
    {
        if (_compressedOutputStream != null)
            throw new IllegalStateException("Compressed output stream is already assigned.");
        if (_out == null || _bOut != null)
        {
            if (_wrapper.getETag()!=null)
                setHeader("ETag",_wrapper.getETag());
                
            _doNotCompress = true;

            _out = _response.getOutputStream();
            setContentLength();

            if (_bOut != null)
                _out.write(_bOut.getBuf(),0,_bOut.getCount());
            _bOut = null;
        }
    }

    /**
     * Check out.
     * 
     * @param lengthToWrite
     *            the length
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void checkOut(int lengthToWrite) throws IOException
    {
        if (_closed)
            throw new IOException("CLOSED");

        if (_out == null)
        {
            long length=_wrapper.getContentLength();
            if (_response.isCommitted() || (length >= 0 && length < _wrapper.getMinCompressSize()))
                doNotCompress();
            else if (lengthToWrite > _wrapper.getMinCompressSize())
                doCompress();
            else
                _out = _bOut = new ByteArrayOutputStream2(_wrapper.getBufferSize());
        }
        else if (_bOut != null)
        {
            long length=_wrapper.getContentLength();
            if (_response.isCommitted() || (length >= 0 && length < _wrapper.getMinCompressSize()))
                doNotCompress();
            else if (lengthToWrite >= (_bOut.getBuf().length - _bOut.getCount()))
                doCompress();
        }
    }

    /**
     * @see org.eclipse.jetty.http.gzip.CompressedStream#getOutputStream()
     */
    public OutputStream getOutputStream()
    {
        return _out;
    }

    /**
     * @see org.eclipse.jetty.http.gzip.CompressedStream#isClosed()
     */
    public boolean isClosed()
    {
        return _closed;
    }
    
    /**
     * Allows derived implementations to replace PrintWriter implementation.
     */
    protected PrintWriter newWriter(OutputStream out, String encoding) throws UnsupportedEncodingException
    {
        return encoding == null?new PrintWriter(out):new PrintWriter(new OutputStreamWriter(out,encoding));
    }

    protected void setHeader(String name,String value)
    {
        _response.setHeader(name, value);
    }
    
    /**
     * Create the stream fitting to the underlying compression type.
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected abstract DeflaterOutputStream createStream() throws IOException;

}
