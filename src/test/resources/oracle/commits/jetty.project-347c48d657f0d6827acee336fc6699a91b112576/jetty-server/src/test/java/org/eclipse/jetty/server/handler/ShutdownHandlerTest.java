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

package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.Assert;
import org.junit.Test;

public class ShutdownHandlerTest
{
    private Server server;
    private ServerConnector connector;
    private String shutdownToken = "asdlnsldgnklns";

    public void start(HandlerWrapper wrapper) throws Exception
    {
        server = new Server();
        connector = new ServerConnector(server);
        server.addConnector(connector);
        Handler shutdown = new ShutdownHandler(shutdownToken);
        Handler handler = shutdown;
        if (wrapper != null)
        {
            wrapper.setHandler(shutdown);
            handler = wrapper;
        }
        server.setHandler(handler);
        server.start();
    }

    @Test
    public void testShutdownServerWithCorrectTokenAndIP() throws Exception
    {
        start(null);

        CountDownLatch stopLatch = new CountDownLatch(1);
        server.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener()
        {
            @Override
            public void lifeCycleStopped(LifeCycle event)
            {
                stopLatch.countDown();
            }
        });

        HttpTester.Response response = shutdown(shutdownToken);
        Assert.assertEquals(HttpStatus.OK_200, response.getStatus());

        Assert.assertTrue(stopLatch.await(5, TimeUnit.SECONDS));
        Assert.assertEquals(AbstractLifeCycle.STOPPED, server.getState());
    }

    @Test
    public void testWrongToken() throws Exception
    {
        start(null);

        HttpTester.Response response = shutdown("wrongToken");
        Assert.assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatus());

        Thread.sleep(1000);
        Assert.assertEquals(AbstractLifeCycle.STARTED, server.getState());
    }

    @Test
    public void testShutdownRequestNotFromLocalhost() throws Exception
    {
        start(new HandlerWrapper()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setRemoteAddr(new InetSocketAddress("192.168.0.1", 12345));
                super.handle(target, baseRequest, request, response);
            }
        });

        HttpTester.Response response = shutdown(shutdownToken);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED_401, response.getStatus());

        Thread.sleep(1000);
        Assert.assertEquals(AbstractLifeCycle.STARTED, server.getState());
    }

    private HttpTester.Response shutdown(String shutdownToken) throws IOException
    {
        try (Socket socket = new Socket("localhost", connector.getLocalPort()))
        {
            String request = "" +
                    "POST /shutdown?token=" + shutdownToken + " HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "\r\n";
            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes(StandardCharsets.UTF_8));
            output.flush();

            HttpTester.Input input = HttpTester.from(socket.getInputStream());
            return HttpTester.parseResponse(input);
        }
    }
}
