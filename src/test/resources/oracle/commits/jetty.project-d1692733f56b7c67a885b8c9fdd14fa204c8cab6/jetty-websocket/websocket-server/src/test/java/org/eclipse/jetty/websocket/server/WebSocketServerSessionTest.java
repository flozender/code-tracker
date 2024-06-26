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

package org.eclipse.jetty.websocket.server;

import static org.hamcrest.Matchers.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.toolchain.test.AdvancedRunner;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.common.WebSocketFrame;
import org.eclipse.jetty.websocket.server.blockhead.BlockheadClient;
import org.eclipse.jetty.websocket.server.helper.IncomingFramesCapture;
import org.eclipse.jetty.websocket.server.helper.SessionServlet;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing various aspects of the server side support for WebSocket {@link Session}
 */
@RunWith(AdvancedRunner.class)
public class WebSocketServerSessionTest
{
    private static SimpleServletServer server;

    @BeforeClass
    public static void startServer() throws Exception
    {
        server = new SimpleServletServer(new SessionServlet());
        server.start();
    }

    @AfterClass
    public static void stopServer()
    {
        server.stop();
    }

    @Test
    public void testDisconnect() throws Exception
    {
        URI uri = server.getServerUri().resolve("/test/disconnect");
        BlockheadClient client = new BlockheadClient(uri);
        try
        {
            client.connect();
            client.sendStandardRequest();
            client.expectUpgradeResponse();

            client.write(WebSocketFrame.text("harsh-disconnect"));

            client.awaitDisconnect(1,TimeUnit.SECONDS);
        }
        finally
        {
            client.close();
        }
    }

    @Test
    public void testUpgradeRequestResponse() throws Exception
    {
        URI uri = server.getServerUri().resolve("/test?snack=cashews&amount=handful&brand=off");
        BlockheadClient client = new BlockheadClient(uri);
        try
        {
            client.connect();
            client.sendStandardRequest();
            client.expectUpgradeResponse();

            // Ask the server socket for specific parameter map info
            client.write(WebSocketFrame.text("getParameterMap|snack"));
            client.write(WebSocketFrame.text("getParameterMap|amount"));
            client.write(WebSocketFrame.text("getParameterMap|brand"));
            client.write(WebSocketFrame.text("getParameterMap|cost")); // intentionall invalid

            // Read frame (hopefully text frame)
            IncomingFramesCapture capture = client.readFrames(4,TimeUnit.MILLISECONDS,500);
            WebSocketFrame tf = capture.getFrames().poll();
            Assert.assertThat("Parameter Map[snack]",tf.getPayloadAsUTF8(),is("[cashews]"));
            tf = capture.getFrames().poll();
            Assert.assertThat("Parameter Map[amount]",tf.getPayloadAsUTF8(),is("[handful]"));
            tf = capture.getFrames().poll();
            Assert.assertThat("Parameter Map[brand]",tf.getPayloadAsUTF8(),is("[off]"));
            tf = capture.getFrames().poll();
            Assert.assertThat("Parameter Map[cost]",tf.getPayloadAsUTF8(),is("<null>"));
        }
        finally
        {
            client.close();
        }
    }

}
