//
//  ========================================================================
//  Copyright (c) 1995-2016 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.IO;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public abstract class ConnectorTimeoutTest extends HttpServerTestFixture
{
    protected static final int MAX_IDLE_TIME=500;
    private int sleepTime = MAX_IDLE_TIME + MAX_IDLE_TIME/5;
    private int minimumTestRuntime = MAX_IDLE_TIME-MAX_IDLE_TIME/5;
    private int maximumTestRuntime = MAX_IDLE_TIME*10;

    static
    {
        System.setProperty("org.eclipse.jetty.io.nio.IDLE_TICK","100");
    }


    @Test
    public void testMaxIdleWithRequest10() throws Exception
    {
        configureServer(new HelloWorldHandler());
        
        Socket client=newSocket(_serverURI.getHost(),_serverURI.getPort());
        client.setSoTimeout(10000);

        Assert.assertFalse(client.isClosed());

        OutputStream os=client.getOutputStream();
        InputStream is=client.getInputStream();

        os.write((
                "GET / HTTP/1.0\r\n"+
                "host: "+_serverURI.getHost()+":"+_serverURI.getPort()+"\r\n"+
                "connection: keep-alive\r\n"+
        "\r\n").getBytes("utf-8"));
        os.flush();

        long start = System.currentTimeMillis();
        IO.toString(is);

        Thread.sleep(sleepTime);
        Assert.assertEquals(-1, is.read());

        Assert.assertTrue(System.currentTimeMillis() - start > minimumTestRuntime);
        Assert.assertTrue(System.currentTimeMillis() - start < maximumTestRuntime);
    }

    @Test
    public void testMaxIdleWithRequest11() throws Exception
    {
        configureServer(new EchoHandler());
        Socket client=newSocket(_serverURI.getHost(),_serverURI.getPort());
        client.setSoTimeout(10000);

        Assert.assertFalse(client.isClosed());

        OutputStream os=client.getOutputStream();
        InputStream is=client.getInputStream();

        String content="Wibble";
        byte[] contentB=content.getBytes("utf-8");
        os.write((
                "POST /echo HTTP/1.1\r\n"+
                "host: "+_serverURI.getHost()+":"+_serverURI.getPort()+"\r\n"+
                "content-type: text/plain; charset=utf-8\r\n"+
                "content-length: "+contentB.length+"\r\n"+
        "\r\n").getBytes("utf-8"));
        os.write(contentB);
        os.flush();

        long start = System.currentTimeMillis();
        IO.toString(is);

        Thread.sleep(sleepTime);
        Assert.assertEquals(-1, is.read());

        Assert.assertTrue(System.currentTimeMillis() - start > minimumTestRuntime);
        Assert.assertTrue(System.currentTimeMillis() - start < maximumTestRuntime);
    }

    @Test
    public void testMaxIdleWithRequest10NoClientClose() throws Exception
    {
        final Exchanger<EndPoint> exchanger = new Exchanger<>();
        configureServer(new HelloWorldHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException,
                    ServletException
            {
                try
                {
                    exchanger.exchange(baseRequest.getHttpChannel().getEndPoint());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                super.handle(target, baseRequest, request, response);
            }

        });
        Socket client=newSocket(_serverURI.getHost(),_serverURI.getPort());
        client.setSoTimeout(10000);

        Assert.assertFalse(client.isClosed());

        OutputStream os=client.getOutputStream();
        InputStream is=client.getInputStream();

        os.write((
                "GET / HTTP/1.0\r\n"+
                "host: "+_serverURI.getHost()+":"+_serverURI.getPort()+"\r\n"+
                "connection: close\r\n"+
        "\r\n").getBytes("utf-8"));
        os.flush();

        // Get the server side endpoint
        EndPoint endPoint = exchanger.exchange(null,10,TimeUnit.SECONDS);
        if (endPoint instanceof SslConnection.DecryptedEndPoint)
            endPoint=endPoint.getConnection().getEndPoint();

        // read the response
        String result=IO.toString(is);
        Assert.assertThat("OK",result, Matchers.containsString("200 OK"));

        // check client reads EOF
        Assert.assertEquals(-1, is.read());

        // wait for idle timeout
        TimeUnit.MILLISECONDS.sleep(3 * MAX_IDLE_TIME);

        // further writes will get broken pipe or similar
        try
        {
            for (int i=0;i<1000;i++)
            {
                os.write((
                        "GET / HTTP/1.0\r\n"+
                        "host: "+_serverURI.getHost()+":"+_serverURI.getPort()+"\r\n"+
                        "connection: keep-alive\r\n"+
                "\r\n").getBytes("utf-8"));
                os.flush();
            }
            Assert.fail("half close should have timed out");
        }
        catch(SocketException e)
        {
            // expected
        }
        // check the server side is closed
        Assert.assertFalse(endPoint.isOpen());
    }

    @Test
    public void testMaxIdleWithRequest10ClientIgnoresClose() throws Exception
    {
        final Exchanger<EndPoint> exchanger = new Exchanger<>();
        configureServer(new HelloWorldHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException,
                    ServletException
            {
                try
                {
                    exchanger.exchange(baseRequest.getHttpChannel().getEndPoint());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                super.handle(target, baseRequest, request, response);
            }

        });
        Socket client=newSocket(_serverURI.getHost(),_serverURI.getPort());
        client.setSoTimeout(10000);

        Assert.assertFalse(client.isClosed());

        OutputStream os=client.getOutputStream();
        InputStream is=client.getInputStream();

        os.write((
                "GET / HTTP/1.0\r\n"+
                "host: "+_serverURI.getHost()+":"+_serverURI.getPort()+"\r\n"+
                "connection: close\r\n"+
        "\r\n").getBytes("utf-8"));
        os.flush();

        // Get the server side endpoint
        EndPoint endPoint = exchanger.exchange(null,10,TimeUnit.SECONDS);
        if (endPoint instanceof SslConnection.DecryptedEndPoint)
            endPoint=endPoint.getConnection().getEndPoint();

        // read the response
        String result=IO.toString(is);
        Assert.assertThat("OK",result, Matchers.containsString("200 OK"));

        // check client reads EOF
        Assert.assertEquals(-1, is.read());
        Assert.assertTrue(endPoint.isOutputShutdown());

        Thread.sleep(2 * MAX_IDLE_TIME);

        // further writes will get broken pipe or similar
        try
        {
            long end=System.currentTimeMillis()+MAX_IDLE_TIME+3000;
            while (System.currentTimeMillis()<end)
            {
                os.write("THIS DATA SHOULD NOT BE PARSED!\n\n".getBytes("utf-8"));
                os.flush();
                Thread.sleep(100);
            }
            Assert.fail("half close should have timed out");
        }
        catch(SocketException e)
        {
            // expected

            // Give the SSL onClose time to act
            Thread.sleep(100);
        }

        // check the server side is closed
        Assert.assertFalse(endPoint.isOpen());
    }

    @Test
    public void testMaxIdleWithRequest11NoClientClose() throws Exception
    {
        final Exchanger<EndPoint> exchanger = new Exchanger<>();
        configureServer(new EchoHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException,
                    ServletException
            {
                try
                {
                    exchanger.exchange(baseRequest.getHttpChannel().getEndPoint());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                super.handle(target, baseRequest, request, response);
            }

        });
        Socket client=newSocket(_serverURI.getHost(),_serverURI.getPort());
        client.setSoTimeout(10000);

        Assert.assertFalse(client.isClosed());

        OutputStream os=client.getOutputStream();
        InputStream is=client.getInputStream();

        String content="Wibble";
        byte[] contentB=content.getBytes("utf-8");
        os.write((
                "POST /echo HTTP/1.1\r\n" +
                        "host: " + _serverURI.getHost() + ":" + _serverURI.getPort() + "\r\n" +
                        "content-type: text/plain; charset=utf-8\r\n" +
                        "content-length: " + contentB.length + "\r\n" +
                        "connection: close\r\n" +
                        "\r\n").getBytes("utf-8"));
        os.write(contentB);
        os.flush();

        // Get the server side endpoint
        EndPoint endPoint = exchanger.exchange(null,10,TimeUnit.SECONDS);

        // read the response
        IO.toString(is);

        // check client reads EOF
        Assert.assertEquals(-1, is.read());

        TimeUnit.MILLISECONDS.sleep(3*MAX_IDLE_TIME);


        // further writes will get broken pipe or similar
        try
        {
            for (int i=0;i<1000;i++)
            {
                os.write((
                        "GET / HTTP/1.0\r\n"+
                        "host: "+_serverURI.getHost()+":"+_serverURI.getPort()+"\r\n"+
                        "connection: keep-alive\r\n"+
                "\r\n").getBytes("utf-8"));
                os.flush();
            }
            Assert.fail("half close should have timed out");
        }
        catch(SocketException e)
        {
            // expected
        }

        // check the server side is closed
        Assert.assertFalse(endPoint.isOpen());
    }

    @Test
    public void testMaxIdleNoRequest() throws Exception
    {
        configureServer(new EchoHandler());
        Socket client=newSocket(_serverURI.getHost(),_serverURI.getPort());
        client.setSoTimeout(10000);
        InputStream is=client.getInputStream();
        Assert.assertFalse(client.isClosed());

        Thread.sleep(sleepTime);
        long start = System.currentTimeMillis();
        try
        {
            IO.toString(is);
            Assert.assertEquals(-1, is.read());
        }
        catch(SSLException e)
        {

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        Assert.assertTrue(System.currentTimeMillis() - start < maximumTestRuntime);

    }

    @Test
    public void testMaxIdleWithSlowRequest() throws Exception
    {
        configureServer(new EchoHandler());
        Socket client=newSocket(_serverURI.getHost(),_serverURI.getPort());
        client.setSoTimeout(10000);

        Assert.assertFalse(client.isClosed());

        OutputStream os=client.getOutputStream();
        InputStream is=client.getInputStream();

        String content="Wibble\r\n";
        byte[] contentB=content.getBytes("utf-8");
        os.write((
                "GET / HTTP/1.0\r\n"+
                "host: "+_serverURI.getHost()+":"+_serverURI.getPort()+"\r\n"+
                "connection: keep-alive\r\n"+
                "Content-Length: "+(contentB.length*20)+"\r\n"+
                "Content-Type: text/plain\r\n"+
                "Connection: close\r\n"+
        "\r\n").getBytes("utf-8"));
        os.flush();

        for (int i =0;i<20;i++)
        {
            Thread.sleep(50);
            os.write(contentB);
            os.flush();
        }

        String in = IO.toString(is);
        int offset=0;
        for (int i =0;i<20;i++)
        {
            offset=in.indexOf("Wibble",offset+1);
            Assert.assertTrue("" + i, offset > 0);
        }
    }

    @Test
    public void testMaxIdleWithSlowResponse() throws Exception
    {
        configureServer(new SlowResponseHandler());
        Socket client=newSocket(_serverURI.getHost(),_serverURI.getPort());
        client.setSoTimeout(10000);

        Assert.assertFalse(client.isClosed());

        OutputStream os=client.getOutputStream();
        InputStream is=client.getInputStream();

        os.write((
                "GET / HTTP/1.0\r\n"+
                "host: "+_serverURI.getHost()+":"+_serverURI.getPort()+"\r\n"+
                "connection: keep-alive\r\n"+
                "Connection: close\r\n"+
        "\r\n").getBytes("utf-8"));
        os.flush();

        String in = IO.toString(is);
        int offset=0;
        for (int i =0;i<20;i++)
        {
            offset=in.indexOf("Hello World",offset+1);
            Assert.assertTrue("" + i, offset > 0);
        }
    }

    @Test
    public void testMaxIdleWithWait() throws Exception
    {
        configureServer(new WaitHandler());
        Socket client=newSocket(_serverURI.getHost(),_serverURI.getPort());
        client.setSoTimeout(10000);

        Assert.assertFalse(client.isClosed());

        OutputStream os=client.getOutputStream();
        InputStream is=client.getInputStream();

        os.write((
                "GET / HTTP/1.0\r\n"+
                "host: "+_serverURI.getHost()+":"+_serverURI.getPort()+"\r\n"+
                "connection: keep-alive\r\n"+
                "Connection: close\r\n"+
        "\r\n").getBytes("utf-8"));
        os.flush();

        String in = IO.toString(is);
        int offset=in.indexOf("Hello World");
        Assert.assertTrue(offset > 0);
    }

    protected static class SlowResponseHandler extends AbstractHandler
    {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            baseRequest.setHandled(true);
            response.setStatus(200);
            OutputStream out = response.getOutputStream();

            for (int i=0;i<20;i++)
            {
                out.write("Hello World\r\n".getBytes());
                out.flush();
                try{Thread.sleep(50);}catch(Exception e){e.printStackTrace();}
            }
            out.close();
        }
    }

    protected static class WaitHandler extends AbstractHandler
    {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            baseRequest.setHandled(true);
            response.setStatus(200);
            OutputStream out = response.getOutputStream();
            try{Thread.sleep(2000);}catch(Exception e){e.printStackTrace();}
            out.write("Hello World\r\n".getBytes());
            out.flush();
        }
    }
}
