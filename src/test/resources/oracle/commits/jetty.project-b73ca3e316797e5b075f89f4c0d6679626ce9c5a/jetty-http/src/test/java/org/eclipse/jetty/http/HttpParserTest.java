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

package org.eclipse.jetty.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http.HttpParser.State;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.StringUtil;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class HttpParserTest
{

    /* ------------------------------------------------------------------------------- */
    /**
     * Parse until {@link #END END} state.
     * If the parser is already in the END state, then it is {@link #reset reset} and re-parsed.
     * @param parser TODO
     * @throws IllegalStateException If the buffers have already been partially parsed.
     */
    public static void parseAll(HttpParser parser, ByteBuffer buffer)
    {
        if (parser.isState(State.END))
            parser.reset();
        if (!parser.isState(State.START))
            throw new IllegalStateException("!START");

        // continue parsing
        while (!parser.isState(State.END) && buffer.hasRemaining())
        {
            int remaining=buffer.remaining();
            parser.parseNext(buffer);
            if (remaining==buffer.remaining())
                break;
        }
    }
    
    @Test
    public void testLineParse0() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer("POST /foo HTTP/1.0\015\012" + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);
        parseAll(parser,buffer);
        assertEquals("POST", _methodOrVersion);
        assertEquals("/foo", _uriOrStatus);
        assertEquals("HTTP/1.0", _versionOrReason);
        assertEquals(-1, _h);
    }

    @Test
    public void testLineParse1() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer("GET /999\015\012");

        _versionOrReason= null;
        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);
        parseAll(parser,buffer);
        assertEquals("GET", _methodOrVersion);
        assertEquals("/999", _uriOrStatus);
        assertEquals(null, _versionOrReason);
        assertEquals(-1, _h);
    }

    @Test
    public void testLineParse2() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer("POST /222  \015\012");

        _versionOrReason= null;
        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);
        parseAll(parser,buffer);
        assertEquals("POST", _methodOrVersion);
        assertEquals("/222", _uriOrStatus);
        assertEquals(null, _versionOrReason);
        assertEquals(-1, _h);
    }

    @Test
    public void testLineParse3() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer("POST /fo\u0690 HTTP/1.0\015\012" + "\015\012",StringUtil.__UTF8_CHARSET);

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);
        parseAll(parser,buffer);
        assertEquals("POST", _methodOrVersion);
        assertEquals("/fo\u0690", _uriOrStatus);
        assertEquals("HTTP/1.0", _versionOrReason);
        assertEquals(-1, _h);
    }

    @Test
    public void testLineParse4() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer("POST /foo?param=\u0690 HTTP/1.0\015\012" + "\015\012",StringUtil.__UTF8_CHARSET);

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);
        parseAll(parser,buffer);
        assertEquals("POST", _methodOrVersion);
        assertEquals("/foo?param=\u0690", _uriOrStatus);
        assertEquals("HTTP/1.0", _versionOrReason);
        assertEquals(-1, _h);
    }

    @Test
    public void testConnect() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer("CONNECT 192.168.1.2:80 HTTP/1.1\015\012" + "\015\012");
        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);
        parseAll(parser,buffer);
        assertTrue(handler.request);
        assertEquals("CONNECT", _methodOrVersion);
        assertEquals("192.168.1.2:80", _uriOrStatus);
        assertEquals("HTTP/1.1", _versionOrReason);
        assertEquals(-1, _h);
    }

    @Test
    public void testHeaderParse() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "GET / HTTP/1.0\015\012" +
                        "Host: localhost\015\012" +
                        "Header1: value1\015\012" +
                        "Header 2  :   value 2a  \015\012" +
                        "                    value 2b  \015\012" +
                        "Header3: \015\012" +
                        "Header4 \015\012" +
                        "  value4\015\012" +
                        "Server5 : notServer\015\012" +
                        "Host Header: notHost\015\012" +
                "\015\012");
        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);
        parseAll(parser,buffer);

        assertEquals("GET", _methodOrVersion);
        assertEquals("/", _uriOrStatus);
        assertEquals("HTTP/1.0", _versionOrReason);
        assertEquals("Host", _hdr[0]);
        assertEquals("localhost", _val[0]);
        assertEquals("Header1", _hdr[1]);
        assertEquals("value1", _val[1]);
        assertEquals("Header 2", _hdr[2]);
        assertEquals("value 2a value 2b", _val[2]);
        assertEquals("Header3", _hdr[3]);
        assertEquals(null, _val[3]);
        assertEquals("Header4", _hdr[4]);
        assertEquals("value4", _val[4]);
        assertEquals("Server5", _hdr[5]);
        assertEquals("notServer", _val[5]);
        assertEquals("Host Header", _hdr[6]);
        assertEquals("notHost", _val[6]);
        assertEquals(6, _h);
    }

    @Test
    public void testSplitHeaderParse() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "XXXXSPLIT / HTTP/1.0\015\012" +
                    "Host: localhost\015\012" +
                    "Header1: value1\015\012" +
                    "Header2  :   value 2a  \015\012" +
                    "                    value 2b  \015\012" +
                    "Header3: \015\012" +
                    "Header4 \015\012" +
                    "  value4\015\012" +
                    "Server5: notServer\015\012" +
                    "\015\012ZZZZ");
        buffer.position(2);
        buffer.limit(buffer.capacity()-2);
        buffer=buffer.slice();

        for (int i=0;i<buffer.capacity()-4;i++)
        {
            Handler handler = new Handler();
            HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);

            buffer.position(2);
            buffer.limit(2+i);

            if (!parser.parseNext(buffer))
            {
                // consumed all
                assertEquals(0,buffer.remaining());
                
                // parse the rest
                buffer.limit(buffer.capacity()-2);
                parser.parseNext(buffer);
            }

            assertEquals("SPLIT", _methodOrVersion);
            assertEquals("/", _uriOrStatus);
            assertEquals("HTTP/1.0", _versionOrReason);
            assertEquals("Host", _hdr[0]);
            assertEquals("localhost", _val[0]);
            assertEquals("Header1", _hdr[1]);
            assertEquals("value1", _val[1]);
            assertEquals("Header2", _hdr[2]);
            assertEquals("value 2a value 2b", _val[2]);
            assertEquals("Header3", _hdr[3]);
            assertEquals(null, _val[3]);
            assertEquals("Header4", _hdr[4]);
            assertEquals("value4", _val[4]);
            assertEquals("Server5", _hdr[5]);
            assertEquals("notServer", _val[5]);
            assertEquals(5, _h);
        }
    }


    @Test
    public void testChunkParse() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "GET /chunk HTTP/1.0\015\012"
                        + "Header1: value1\015\012"
                        + "Transfer-Encoding: chunked\015\012"
                        + "\015\012"
                        + "a;\015\012"
                        + "0123456789\015\012"
                        + "1a\015\012"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                        + "0\015\012");
        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);
        parseAll(parser,buffer);

        assertEquals("GET", _methodOrVersion);
        assertEquals("/chunk", _uriOrStatus);
        assertEquals("HTTP/1.0", _versionOrReason);
        assertEquals(1, _h);
        assertEquals("Header1", _hdr[0]);
        assertEquals("value1", _val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", _content);
    }

    @Test
    public void testMultiParse() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                          "GET /mp HTTP/1.0\015\012"
                        + "Connection: Keep-Alive\015\012"
                        + "Header1: value1\015\012"
                        + "Transfer-Encoding: chunked\015\012"
                        + "\015\012"
                        + "a;\015\012"
                        + "0123456789\015\012"
                        + "1a\015\012"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                        + "0\015\012"
                        
                        + "\015\012"
                        
                        + "POST /foo HTTP/1.0\015\012"
                        + "Connection: Keep-Alive\015\012"
                        + "Header2: value2\015\012"
                        + "Content-Length: 0\015\012"
                        + "\015\012"
                        
                        + "PUT /doodle HTTP/1.0\015\012"
                        + "Connection: close\015\012"
                        + "Header3: value3\015\012"
                        + "Content-Length: 10\015\012"
                        + "\015\012"
                        + "0123456789\015\012");


        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);
        parser.parseNext(buffer);
        assertEquals("GET", _methodOrVersion);
        assertEquals("/mp", _uriOrStatus);
        assertEquals("HTTP/1.0", _versionOrReason);
        assertEquals(2, _h);
        assertEquals("Header1", _hdr[1]);
        assertEquals("value1", _val[1]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", _content);

        parser.reset();
        init();
        parser.parseNext(buffer);
        assertEquals("POST", _methodOrVersion);
        assertEquals("/foo", _uriOrStatus);
        assertEquals("HTTP/1.0", _versionOrReason);
        assertEquals(2, _h);
        assertEquals("Header2", _hdr[1]);
        assertEquals("value2", _val[1]);
        assertEquals(null, _content);

        parser.reset();
        init();
        parser.parseNext(buffer);
        parser.inputShutdown();
        assertEquals("PUT", _methodOrVersion);
        assertEquals("/doodle", _uriOrStatus);
        assertEquals("HTTP/1.0", _versionOrReason);
        assertEquals(2, _h);
        assertEquals("Header3", _hdr[1]);
        assertEquals("value3", _val[1]);
        assertEquals("0123456789", _content);
    }

    @Test
    public void testResponseParse0() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "HTTP/1.1 200 Correct\015\012"
                        + "Content-Length: 10\015\012"
                        + "Content-Type: text/plain\015\012"
                        + "\015\012"
                        + "0123456789\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", _methodOrVersion);
        assertEquals("200", _uriOrStatus);
        assertEquals("Correct", _versionOrReason);
        assertEquals(10,_content.length());
        assertTrue(_headerCompleted);
        assertTrue(_messageCompleted);
    }

    @Test
    public void testResponseParse1() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "HTTP/1.1 304 Not-Modified\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", _methodOrVersion);
        assertEquals("304", _uriOrStatus);
        assertEquals("Not-Modified", _versionOrReason);
        assertTrue(_headerCompleted);
        assertTrue(_messageCompleted);
    }

    @Test
    public void testResponseParse2() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                          "HTTP/1.1 204 No-Content\015\012"
                        + "Header: value\015\012"
                        + "\015\012"
                        
                        + "HTTP/1.1 200 Correct\015\012"
                        + "Content-Length: 10\015\012"
                        + "Content-Type: text/plain\015\012"
                        + "\015\012"
                        + "0123456789\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", _methodOrVersion);
        assertEquals("204", _uriOrStatus);
        assertEquals("No-Content", _versionOrReason);
        assertTrue(_headerCompleted);
        assertTrue(_messageCompleted);

        parser.reset();
        init();
        
        parser.parseNext(buffer);
        parser.inputShutdown();
        assertEquals("HTTP/1.1", _methodOrVersion);
        assertEquals("200", _uriOrStatus);
        assertEquals("Correct", _versionOrReason);
        assertEquals(_content.length(), 10);
        assertTrue(_headerCompleted);
        assertTrue(_messageCompleted);
    }


    @Test
    public void testResponseParse3() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "HTTP/1.1 200\015\012"
                        + "Content-Length: 10\015\012"
                        + "Content-Type: text/plain\015\012"
                        + "\015\012"
                        + "0123456789\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", _methodOrVersion);
        assertEquals("200", _uriOrStatus);
        assertEquals(null, _versionOrReason);
        assertEquals(_content.length(), 10);
        assertTrue(_headerCompleted);
        assertTrue(_messageCompleted);
    }

    @Test
    public void testResponseParse4() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "HTTP/1.1 200 \015\012"
                        + "Content-Length: 10\015\012"
                        + "Content-Type: text/plain\015\012"
                        + "\015\012"
                        + "0123456789\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", _methodOrVersion);
        assertEquals("200", _uriOrStatus);
        assertEquals(null, _versionOrReason);
        assertEquals(_content.length(), 10);
        assertTrue(_headerCompleted);
        assertTrue(_messageCompleted);
    }

    @Test
    public void testResponse304WithContentLength() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "HTTP/1.1 304 found\015\012"
                        + "Content-Length: 10\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", _methodOrVersion);
        assertEquals("304", _uriOrStatus);
        assertEquals("found", _versionOrReason);
        assertEquals(null,_content);
        assertTrue(_headerCompleted);
        assertTrue(_messageCompleted);
    }

    @Test
    public void testSeekEOF() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "HTTP/1.1 200 OK\015\012"
                        + "Content-Length: 0\015\012"
                        + "Connection: close\015\012"
                        + "\015\012"
                        + "\015\012" // extra CRLF ignored
                        + "HTTP/1.1 400 OK\015\012");  // extra data causes close


        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);

        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", _methodOrVersion);
        assertEquals("200", _uriOrStatus);
        assertEquals("OK", _versionOrReason);
        assertEquals(null,_content);
        assertTrue(_headerCompleted);
        assertTrue(_messageCompleted);


    }

    @Test
    public void testNoURI() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "GET\015\012"
                        + "Content-Length: 0\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);

        parser.parseNext(buffer);
        assertEquals(null,_methodOrVersion);
        assertEquals("No URI",_bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.END,parser.getState());
    }


    @Test
    public void testNoURI2() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "GET \015\012"
                        + "Content-Length: 0\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);

        parser.parseNext(buffer);
        assertEquals(null,_methodOrVersion);
        assertEquals("No URI",_bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.END,parser.getState());
    }
    
    @Test
    public void testUnknownReponseVersion() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "HPPT/7.7 200 OK\015\012"
                        + "Content-Length: 0\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);

        parser.parseNext(buffer);
        assertEquals(null,_methodOrVersion);
        assertEquals("Unknown Version",_bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.END,parser.getState());
    }
    
    @Test
    public void testNoStatus() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "HTTP/1.1\015\012"
                        + "Content-Length: 0\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);

        parser.parseNext(buffer);
        assertEquals(null,_methodOrVersion);
        assertEquals("No Status",_bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.END,parser.getState());
    }
    
    @Test
    public void testNoStatus2() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "HTTP/1.1 \015\012"
                        + "Content-Length: 0\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);

        parser.parseNext(buffer);
        assertEquals(null,_methodOrVersion);
        assertEquals("No Status",_bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.END,parser.getState());
    }
    
    @Test
    public void testBadRequestVersion() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "GET / HPPT/7.7\015\012"
                        + "Content-Length: 0\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.ResponseHandler)handler);

        parser.parseNext(buffer);
        assertEquals(null,_methodOrVersion);
        assertEquals("Unknown Version",_bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.END,parser.getState());
    }
    
    @Test
    public void testBadContentLength0() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "GET / HTTP/1.0\015\012"
                        + "Content-Length: abc\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);

        parser.parseNext(buffer);
        assertEquals("GET",_methodOrVersion);
        assertEquals("Bad Content-Length",_bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.END,parser.getState());
    }
    
    @Test
    public void testBadContentLength1() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "GET / HTTP/1.0\015\012"
                        + "Content-Length: 9999999999999999999999999999999999999999999999\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);

        parser.parseNext(buffer);
        assertEquals("GET",_methodOrVersion);
        assertEquals("Bad Content-Length",_bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.END,parser.getState());
    }
    
    @Test
    public void testBadContentLength2() throws Exception
    {
        ByteBuffer buffer= BufferUtil.toBuffer(
                "GET / HTTP/1.0\015\012"
                        + "Content-Length: 1.5\015\012"
                        + "Connection: close\015\012"
                        + "\015\012");

        Handler handler = new Handler();
        HttpParser parser= new HttpParser((HttpParser.RequestHandler)handler);

        parser.parseNext(buffer);
        assertEquals("GET",_methodOrVersion);
        assertEquals("Bad Content-Length",_bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.END,parser.getState());
    }
    
    
    @Before
    public void init()
    {
        _bad=null;
        _content=null;
        _methodOrVersion=null;
        _uriOrStatus=null;
        _versionOrReason=null;
        _hdr=null;
        _val=null;
        _h=0;
        _headerCompleted=false;
        _messageCompleted=false;
    }
    
    private String _bad;
    private String _content;
    private String _methodOrVersion;
    private String _uriOrStatus;
    private String _versionOrReason;
    private String[] _hdr;
    private String[] _val;
    private int _h;

    private boolean _headerCompleted;
    private boolean _messageCompleted;

    private class Handler implements HttpParser.RequestHandler, HttpParser.ResponseHandler
    {
        private HttpFields fields;
        private boolean request;

        @Override
        public boolean content(ByteBuffer ref)
        {
            if (_content==null)
                _content="";
            String c = BufferUtil.toString(ref,StringUtil.__UTF8_CHARSET);
            //System.err.println("content '"+c+"'");
            _content= _content + c;
            ref.position(ref.limit());
            return false;
        }

        @Override
        public boolean startRequest(HttpMethod httpMethod, String method, String uri, HttpVersion version)
        {
            request=true;
            _h= -1;
            _hdr= new String[9];
            _val= new String[9];
            _methodOrVersion= method;
            _uriOrStatus= uri;
            _versionOrReason= version==null?null:version.asString();

            fields=new HttpFields();
            _messageCompleted = false;
            _headerCompleted = false;
            return false;
        }

        @Override
        public boolean parsedHeader(HttpHeader header, String name, String value)
        {
            //System.err.println("header "+name+": "+value);
            _hdr[++_h]= name;
            _val[_h]= value;
            return false;
        }

        @Override
        public boolean headerComplete(boolean hasBody,boolean persistent)
        {
            //System.err.println("headerComplete");
            _content= null;
            String s0=fields.toString();
            String s1=fields.toString();
            if (!s0.equals(s1))
            {
                //System.err.println(s0);
                //System.err.println(s1);
                throw new IllegalStateException();
            }

            _headerCompleted = true;
            return false;
        }

        @Override
        public boolean messageComplete(long contentLength)
        {
            //System.err.println("messageComplete");
            _messageCompleted = true;
            return true;
        }

        @Override
        public void badMessage(int status, String reason)
        {
            _bad=reason;
        }

        @Override
        public boolean startResponse(HttpVersion version, int status, String reason)
        {
            request=false;
            _methodOrVersion = version.asString();
            _uriOrStatus = Integer.toString(status);
            _versionOrReason = reason==null?null:reason.toString();

            fields=new HttpFields();
            _hdr= new String[9];
            _val= new String[9];

            _messageCompleted = false;
            _headerCompleted = false;
            return false;
        }

        @Override
        public boolean earlyEOF()
        {
            return true;
        }
    }
}
