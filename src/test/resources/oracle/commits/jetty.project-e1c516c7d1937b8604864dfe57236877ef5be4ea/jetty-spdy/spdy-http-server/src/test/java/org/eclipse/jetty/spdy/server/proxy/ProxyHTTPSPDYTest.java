//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.spdy.server.proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.spdy.api.BytesDataInfo;
import org.eclipse.jetty.spdy.api.DataInfo;
import org.eclipse.jetty.spdy.api.GoAwayInfo;
import org.eclipse.jetty.spdy.api.GoAwayReceivedInfo;
import org.eclipse.jetty.spdy.api.PingInfo;
import org.eclipse.jetty.spdy.api.PingResultInfo;
import org.eclipse.jetty.spdy.api.PushInfo;
import org.eclipse.jetty.spdy.api.ReplyInfo;
import org.eclipse.jetty.spdy.api.RstInfo;
import org.eclipse.jetty.spdy.api.SPDY;
import org.eclipse.jetty.spdy.api.Session;
import org.eclipse.jetty.spdy.api.SessionFrameListener;
import org.eclipse.jetty.spdy.api.Stream;
import org.eclipse.jetty.spdy.api.StreamFrameListener;
import org.eclipse.jetty.spdy.api.StreamStatus;
import org.eclipse.jetty.spdy.api.SynInfo;
import org.eclipse.jetty.spdy.api.server.ServerSessionFrameListener;
import org.eclipse.jetty.spdy.client.SPDYClient;
import org.eclipse.jetty.spdy.server.SPDYServerConnectionFactory;
import org.eclipse.jetty.spdy.server.SPDYServerConnector;
import org.eclipse.jetty.spdy.server.http.HTTPSPDYHeader;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.Promise;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Ignore // TODO: make these tests pass
@RunWith(value = Parameterized.class)
public class ProxyHTTPSPDYTest
{
    @Rule
    public final TestWatcher testName = new TestWatcher()
    {

        @Override
        public void starting(Description description)
        {
            super.starting(description);
            System.err.printf("Running %s.%s()%n",
                    description.getClassName(),
                    description.getMethodName());
        }
    };
    private final short version;

    @Parameterized.Parameters
    public static Collection<Short[]> parameters()
    {
        return Arrays.asList(new Short[]{SPDY.V2}, new Short[]{SPDY.V3});
    }

    private SPDYClient.Factory factory;
    private Server server;
    private Server proxy;
    private ServerConnector proxyConnector;

    public ProxyHTTPSPDYTest(short version)
    {
        this.version = version;
    }

    protected InetSocketAddress startServer(ServerSessionFrameListener listener) throws Exception
    {
        server = new Server();
        SPDYServerConnector serverConnector = new SPDYServerConnector(server, listener);
        serverConnector.addConnectionFactory(new SPDYServerConnectionFactory(version, listener));
        serverConnector.setPort(0);
        server.addConnector(serverConnector);
        server.start();
        return new InetSocketAddress("localhost", serverConnector.getLocalPort());
    }

    protected InetSocketAddress startProxy(InetSocketAddress address) throws Exception
    {
        proxy = new Server();
        ProxyEngineSelector proxyEngineSelector = new ProxyEngineSelector();
        SPDYProxyEngine spdyProxyEngine = new SPDYProxyEngine(factory);
        proxyEngineSelector.putProxyEngine("spdy/" + version, spdyProxyEngine);
        proxyEngineSelector.putProxyServerInfo("localhost", new ProxyEngineSelector.ProxyServerInfo("spdy/" + version, address.getHostName(), address.getPort()));
        proxyConnector = new HTTPSPDYProxyServerConnector(server, proxyEngineSelector);
        proxyConnector.setPort(0);
        proxy.addConnector(proxyConnector);
        proxy.start();
        return new InetSocketAddress("localhost", proxyConnector.getLocalPort());
    }

    @Before
    public void init() throws Exception
    {
        factory = new SPDYClient.Factory();
        factory.start();
    }

    @After
    public void destroy() throws Exception
    {
        if (server != null)
        {
            server.stop();
            server.join();
        }
        if (proxy != null)
        {
            proxy.stop();
            proxy.join();
        }
        factory.stop();
    }

    @Test
    public void testClosingClientDoesNotCloseServer() throws Exception
    {
        final CountDownLatch closeLatch = new CountDownLatch(1);
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                Fields responseHeaders = new Fields();
                responseHeaders.put(HTTPSPDYHeader.VERSION.name(version), "HTTP/1.1");
                responseHeaders.put(HTTPSPDYHeader.STATUS.name(version), "200 OK");
                stream.reply(new ReplyInfo(responseHeaders, true), new Callback.Adapter());
                return null;
            }

            @Override
            public void onGoAway(Session session, GoAwayReceivedInfo goAwayInfo)
            {
                closeLatch.countDown();
            }
        }));

        Socket client = new Socket();
        client.connect(proxyAddress);
        OutputStream output = client.getOutputStream();

        String request = "" +
                "GET / HTTP/1.1\r\n" +
                "Host: localhost:" + proxyAddress.getPort() + "\r\n" +
                "\r\n";
        output.write(request.getBytes("UTF-8"));
        output.flush();

        InputStream input = client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String line = reader.readLine();
        Assert.assertTrue(line.contains(" 200"));
        while (line.length() > 0)
            line = reader.readLine();
        Assert.assertFalse(reader.ready());

        client.close();

        // Must not close, other clients may still be connected
        Assert.assertFalse(closeLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGETThenNoContentFromTwoClients() throws Exception
    {
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                Assert.assertTrue(synInfo.isClose());
                Fields requestHeaders = synInfo.getHeaders();
                Assert.assertNotNull(requestHeaders.get("via"));

                Fields responseHeaders = new Fields();
                responseHeaders.put(HTTPSPDYHeader.VERSION.name(version), "HTTP/1.1");
                responseHeaders.put(HTTPSPDYHeader.STATUS.name(version), "200 OK");
                ReplyInfo replyInfo = new ReplyInfo(responseHeaders, true);
                stream.reply(replyInfo, new Callback.Adapter());
                return null;
            }
        }));

        Socket client1 = new Socket();
        client1.connect(proxyAddress);
        OutputStream output1 = client1.getOutputStream();

        String request = "" +
                "GET / HTTP/1.1\r\n" +
                "Host: localhost:" + proxyAddress.getPort() + "\r\n" +
                "\r\n";
        output1.write(request.getBytes("UTF-8"));
        output1.flush();

        InputStream input1 = client1.getInputStream();
        BufferedReader reader1 = new BufferedReader(new InputStreamReader(input1, "UTF-8"));
        String line = reader1.readLine();
        Assert.assertTrue(line.contains(" 200"));
        while (line.length() > 0)
            line = reader1.readLine();
        Assert.assertFalse(reader1.ready());

        // Perform another request with another client
        Socket client2 = new Socket();
        client2.connect(proxyAddress);
        OutputStream output2 = client2.getOutputStream();

        output2.write(request.getBytes("UTF-8"));
        output2.flush();

        InputStream input2 = client2.getInputStream();
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(input2, "UTF-8"));
        line = reader2.readLine();
        Assert.assertTrue(line.contains(" 200"));
        while (line.length() > 0)
            line = reader2.readLine();
        Assert.assertFalse(reader2.ready());

        client1.close();
        client2.close();
    }

    @Test
    public void testGETThenSmallResponseContent() throws Exception
    {
        final byte[] data = "0123456789ABCDEF".getBytes("UTF-8");
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                Assert.assertTrue(synInfo.isClose());
                Fields requestHeaders = synInfo.getHeaders();
                Assert.assertNotNull(requestHeaders.get("via"));

                Fields responseHeaders = new Fields();
                responseHeaders.put(HTTPSPDYHeader.VERSION.name(version), "HTTP/1.1");
                responseHeaders.put(HTTPSPDYHeader.STATUS.name(version), "200 OK");
                ReplyInfo replyInfo = new ReplyInfo(responseHeaders, false);
                stream.reply(replyInfo, new Callback.Adapter());
                stream.data(new BytesDataInfo(data, true), new Callback.Adapter());

                return null;
            }
        }));

        Socket client = new Socket();
        client.connect(proxyAddress);
        OutputStream output = client.getOutputStream();

        String request = "" +
                "GET / HTTP/1.1\r\n" +
                "Host: localhost:" + proxyAddress.getPort() + "\r\n" +
                "\r\n";
        output.write(request.getBytes("UTF-8"));
        output.flush();

        InputStream input = client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String line = reader.readLine();
        Assert.assertTrue(line.contains(" 200 "));
        while (line.length() > 0)
            line = reader.readLine();
        for (byte datum : data)
            Assert.assertEquals(datum, reader.read());
        Assert.assertFalse(reader.ready());

        // Perform another request so that we are sure we reset the states of parsers and generators
        output.write(request.getBytes("UTF-8"));
        output.flush();

        line = reader.readLine();
        Assert.assertTrue(line.contains(" 200"));
        while (line.length() > 0)
            line = reader.readLine();
        for (byte datum : data)
            Assert.assertEquals(datum, reader.read());
        Assert.assertFalse(reader.ready());

        client.close();
    }

    @Test
    public void testPOSTWithSmallRequestContentThenRedirect() throws Exception
    {
        final byte[] data = "0123456789ABCDEF".getBytes("UTF-8");
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                return new StreamFrameListener.Adapter()
                {
                    @Override
                    public void onData(Stream stream, DataInfo dataInfo)
                    {
                        dataInfo.consume(dataInfo.length());
                        if (dataInfo.isClose())
                        {
                            Fields headers = new Fields();
                            headers.put(HTTPSPDYHeader.VERSION.name(version), "HTTP/1.1");
                            headers.put(HTTPSPDYHeader.STATUS.name(version), "303 See Other");
                            stream.reply(new ReplyInfo(headers, true), new Callback.Adapter());
                        }
                    }
                };
            }
        }));

        Socket client = new Socket();
        client.connect(proxyAddress);
        OutputStream output = client.getOutputStream();

        String request = "" +
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:" + proxyAddress.getPort() + "\r\n" +
                "Content-Length: " + data.length + "\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n";
        output.write(request.getBytes("UTF-8"));
        output.write(data);
        output.flush();

        InputStream input = client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String line = reader.readLine();
        Assert.assertTrue(line.contains(" 303"));
        while (line.length() > 0)
            line = reader.readLine();
        Assert.assertFalse(reader.ready());

        // Perform another request so that we are sure we reset the states of parsers and generators
        output.write(request.getBytes("UTF-8"));
        output.write(data);
        output.flush();

        line = reader.readLine();
        Assert.assertTrue(line.contains(" 303"));
        while (line.length() > 0)
            line = reader.readLine();
        Assert.assertFalse(reader.ready());

        client.close();
    }

    @Test
    public void testPOSTWithSmallRequestContentThenSmallResponseContent() throws Exception
    {
        final byte[] data = "0123456789ABCDEF".getBytes("UTF-8");
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                return new StreamFrameListener.Adapter()
                {
                    @Override
                    public void onData(Stream stream, DataInfo dataInfo)
                    {
                        dataInfo.consume(dataInfo.length());
                        if (dataInfo.isClose())
                        {
                            Fields responseHeaders = new Fields();
                            responseHeaders.put(HTTPSPDYHeader.VERSION.name(version), "HTTP/1.1");
                            responseHeaders.put(HTTPSPDYHeader.STATUS.name(version), "200 OK");
                            ReplyInfo replyInfo = new ReplyInfo(responseHeaders, false);
                            stream.reply(replyInfo, new Callback.Adapter());
                            stream.data(new BytesDataInfo(data, true), new Callback.Adapter());
                        }
                    }
                };
            }
        }));

        Socket client = new Socket();
        client.connect(proxyAddress);
        OutputStream output = client.getOutputStream();

        String request = "" +
                "POST / HTTP/1.1\r\n" +
                "Host: localhost:" + proxyAddress.getPort() + "\r\n" +
                "Content-Length: " + data.length + "\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n";
        output.write(request.getBytes("UTF-8"));
        output.write(data);
        output.flush();

        InputStream input = client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String line = reader.readLine();
        Assert.assertTrue(line.contains(" 200"));
        while (line.length() > 0)
            line = reader.readLine();
        for (byte datum : data)
            Assert.assertEquals(datum, reader.read());
        Assert.assertFalse(reader.ready());

        // Perform another request so that we are sure we reset the states of parsers and generators
        output.write(request.getBytes("UTF-8"));
        output.write(data);
        output.flush();

        line = reader.readLine();
        Assert.assertTrue(line.contains(" 200"));
        while (line.length() > 0)
            line = reader.readLine();
        for (byte datum : data)
            Assert.assertEquals(datum, reader.read());
        Assert.assertFalse(reader.ready());

        client.close();
    }

    @Test
    public void testSYNThenREPLY() throws Exception
    {
        final String header = "foo";
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                Fields requestHeaders = synInfo.getHeaders();
                Assert.assertNotNull(requestHeaders.get("via"));
                Assert.assertNotNull(requestHeaders.get(header));

                Fields responseHeaders = new Fields();
                responseHeaders.put(header, "baz");
                stream.reply(new ReplyInfo(responseHeaders, true), new Callback.Adapter());
                return null;
            }
        }));
        proxyConnector.addConnectionFactory(proxyConnector.getConnectionFactory("spdy/" + version));

        Session client = factory.newSPDYClient(version).connect(proxyAddress, null).get(5, TimeUnit.SECONDS);

        final CountDownLatch replyLatch = new CountDownLatch(1);
        Fields headers = new Fields();
        headers.put(HTTPSPDYHeader.HOST.name(version), "localhost:" + proxyAddress.getPort());
        headers.put(header, "bar");
        client.syn(new SynInfo(headers, true), new StreamFrameListener.Adapter()
        {
            @Override
            public void onReply(Stream stream, ReplyInfo replyInfo)
            {
                Fields headers = replyInfo.getHeaders();
                Assert.assertNotNull(headers.get(header));
                replyLatch.countDown();
            }
        });

        Assert.assertTrue(replyLatch.await(5, TimeUnit.SECONDS));

        client.goAway(new GoAwayInfo(5, TimeUnit.SECONDS));
    }

    @Test
    public void testSYNThenREPLYAndDATA() throws Exception
    {
        final byte[] data = "0123456789ABCDEF".getBytes("UTF-8");
        final String header = "foo";
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                Fields requestHeaders = synInfo.getHeaders();
                Assert.assertNotNull(requestHeaders.get("via"));
                Assert.assertNotNull(requestHeaders.get(header));

                Fields responseHeaders = new Fields();
                responseHeaders.put(header, "baz");
                stream.reply(new ReplyInfo(responseHeaders, false), new Callback.Adapter());
                stream.data(new BytesDataInfo(data, true), new Callback.Adapter());
                return null;
            }
        }));
        proxyConnector.addConnectionFactory(proxyConnector.getConnectionFactory("spdy/" + version));

        Session client = factory.newSPDYClient(version).connect(proxyAddress, null).get(5, TimeUnit.SECONDS);

        final CountDownLatch replyLatch = new CountDownLatch(1);
        final CountDownLatch dataLatch = new CountDownLatch(1);
        Fields headers = new Fields();
        headers.put(HTTPSPDYHeader.HOST.name(version), "localhost:" + proxyAddress.getPort());
        headers.put(header, "bar");
        client.syn(new SynInfo(headers, true), new StreamFrameListener.Adapter()
        {
            private final ByteArrayOutputStream result = new ByteArrayOutputStream();

            @Override
            public void onReply(Stream stream, ReplyInfo replyInfo)
            {
                Fields headers = replyInfo.getHeaders();
                Assert.assertNotNull(headers.get(header));
                replyLatch.countDown();
            }

            @Override
            public void onData(Stream stream, DataInfo dataInfo)
            {
                result.write(dataInfo.asBytes(true), 0, dataInfo.length());
                if (dataInfo.isClose())
                {
                    Assert.assertArrayEquals(data, result.toByteArray());
                    dataLatch.countDown();
                }
            }
        });

        Assert.assertTrue(replyLatch.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(dataLatch.await(5, TimeUnit.SECONDS));

        client.goAway(new GoAwayInfo(5, TimeUnit.SECONDS));
    }

    @Test
    public void testGETThenSPDYPushIsIgnored() throws Exception
    {
        final byte[] data = "0123456789ABCDEF".getBytes("UTF-8");
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                Fields responseHeaders = new Fields();
                responseHeaders.put(HTTPSPDYHeader.VERSION.name(version), "HTTP/1.1");
                responseHeaders.put(HTTPSPDYHeader.STATUS.name(version), "200 OK");

                Fields pushHeaders = new Fields();
                pushHeaders.put(HTTPSPDYHeader.URI.name(version), "/push");
                stream.push(new PushInfo(5, TimeUnit.SECONDS, pushHeaders, false), new Promise.Adapter<Stream>()
                {
                    @Override
                    public void succeeded(Stream pushStream)
                    {
                        pushStream.data(new BytesDataInfo(data, true), new Callback.Adapter());
                    }
                });

                stream.reply(new ReplyInfo(responseHeaders, true), new Callback.Adapter());
                return null;
            }
        }));

        Socket client = new Socket();
        client.connect(proxyAddress);
        OutputStream output = client.getOutputStream();

        String request = "" +
                "GET / HTTP/1.1\r\n" +
                "Host: localhost:" + proxyAddress.getPort() + "\r\n" +
                "\r\n";
        output.write(request.getBytes("UTF-8"));
        output.flush();

        client.setSoTimeout(1000);
        InputStream input = client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String line = reader.readLine();
        Assert.assertTrue(line.contains(" 200"));
        while (line.length() > 0)
            line = reader.readLine();
        Assert.assertFalse(reader.ready());

        client.close();
    }

    @Test
    public void testSYNThenSPDYPushIsReceived() throws Exception
    {
        final byte[] data = "0123456789ABCDEF".getBytes("UTF-8");
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                Fields responseHeaders = new Fields();
                responseHeaders.put(HTTPSPDYHeader.VERSION.name(version), "HTTP/1.1");
                responseHeaders.put(HTTPSPDYHeader.STATUS.name(version), "200 OK");
                stream.reply(new ReplyInfo(responseHeaders, false), new Callback.Adapter());

                Fields pushHeaders = new Fields();
                pushHeaders.put(HTTPSPDYHeader.URI.name(version), "/push");
                stream.push(new PushInfo(5, TimeUnit.SECONDS, pushHeaders, false), new Promise.Adapter<Stream>()
                {
                    @Override
                    public void succeeded(Stream pushStream)
                    {
                        pushStream.data(new BytesDataInfo(data, true), new Callback.Adapter());
                    }
                });

                stream.data(new BytesDataInfo(data, true), new Callback.Adapter());

                return null;
            }
        }));
        proxyConnector.addConnectionFactory(proxyConnector.getConnectionFactory("spdy/" + version));

        final CountDownLatch pushSynLatch = new CountDownLatch(1);
        final CountDownLatch pushDataLatch = new CountDownLatch(1);
        Session client = factory.newSPDYClient(version).connect(proxyAddress, new SessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                pushSynLatch.countDown();
                return new StreamFrameListener.Adapter()
                {
                    @Override
                    public void onData(Stream stream, DataInfo dataInfo)
                    {
                        dataInfo.consume(dataInfo.length());
                        if (dataInfo.isClose())
                            pushDataLatch.countDown();
                    }
                };
            }
        }).get(5, TimeUnit.SECONDS);

        Fields headers = new Fields();
        headers.put(HTTPSPDYHeader.HOST.name(version), "localhost:" + proxyAddress.getPort());
        final CountDownLatch replyLatch = new CountDownLatch(1);
        final CountDownLatch dataLatch = new CountDownLatch(1);
        client.syn(new SynInfo(headers, true), new StreamFrameListener.Adapter()
        {
            @Override
            public void onReply(Stream stream, ReplyInfo replyInfo)
            {
                replyLatch.countDown();
            }

            @Override
            public void onData(Stream stream, DataInfo dataInfo)
            {
                dataInfo.consume(dataInfo.length());
                if (dataInfo.isClose())
                    dataLatch.countDown();
            }
        });

        Assert.assertTrue(replyLatch.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(pushSynLatch.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(pushDataLatch.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(dataLatch.await(5, TimeUnit.SECONDS));

        client.goAway(new GoAwayInfo(5, TimeUnit.SECONDS));
    }

    @Test
    public void testPING() throws Exception
    {
        // PING is per hop, and it does not carry the information to which server to ping to
        // We just verify that it works

        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()));
        proxyConnector.addConnectionFactory(proxyConnector.getConnectionFactory("spdy/" + version));

        final CountDownLatch pingLatch = new CountDownLatch(1);
        Session client = factory.newSPDYClient(version).connect(proxyAddress, new SessionFrameListener.Adapter()
        {
            @Override
            public void onPing(Session session, PingResultInfo pingInfo)
            {
                pingLatch.countDown();
            }
        }).get(5, TimeUnit.SECONDS);

        client.ping(new PingInfo(5, TimeUnit.SECONDS));

        Assert.assertTrue(pingLatch.await(5, TimeUnit.SECONDS));

        client.goAway(new GoAwayInfo(5, TimeUnit.SECONDS));
    }

    @Test
    public void testGETThenReset() throws Exception
    {
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                Assert.assertTrue(synInfo.isClose());
                Fields requestHeaders = synInfo.getHeaders();
                Assert.assertNotNull(requestHeaders.get("via"));

                stream.getSession().rst(new RstInfo(stream.getId(), StreamStatus.REFUSED_STREAM), new Callback.Adapter());

                return null;
            }
        }));

        Socket client = new Socket();
        client.connect(proxyAddress);
        OutputStream output = client.getOutputStream();

        String request = "" +
                "GET / HTTP/1.1\r\n" +
                "Host: localhost:" + proxyAddress.getPort() + "\r\n" +
                "\r\n";
        output.write(request.getBytes("UTF-8"));
        output.flush();

        InputStream input = client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        Assert.assertNull(reader.readLine());

        client.close();
    }

    @Test
    public void testSYNThenReset() throws Exception
    {
        InetSocketAddress proxyAddress = startProxy(startServer(new ServerSessionFrameListener.Adapter()
        {
            @Override
            public StreamFrameListener onSyn(Stream stream, SynInfo synInfo)
            {
                Assert.assertTrue(synInfo.isClose());
                Fields requestHeaders = synInfo.getHeaders();
                Assert.assertNotNull(requestHeaders.get("via"));

                stream.getSession().rst(new RstInfo(stream.getId(), StreamStatus.REFUSED_STREAM), new Callback.Adapter());

                return null;
            }
        }));
        proxyConnector.addConnectionFactory(proxyConnector.getConnectionFactory("spdy/" + version));

        final CountDownLatch resetLatch = new CountDownLatch(1);
        Session client = factory.newSPDYClient(version).connect(proxyAddress, new SessionFrameListener.Adapter()
        {
            @Override
            public void onRst(Session session, RstInfo rstInfo)
            {
                resetLatch.countDown();
            }
        }).get(5, TimeUnit.SECONDS);

        Fields headers = new Fields();
        headers.put(HTTPSPDYHeader.HOST.name(version), "localhost:" + proxyAddress.getPort());
        client.syn(new SynInfo(headers, true), null);

        Assert.assertTrue(resetLatch.await(5, TimeUnit.SECONDS));

        client.goAway(new GoAwayInfo(5, TimeUnit.SECONDS));
    }
}
