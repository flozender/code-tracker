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

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.common.WebSocketFrame;
import org.eclipse.jetty.websocket.server.blockhead.BlockheadClient;
import org.eclipse.jetty.websocket.server.examples.MyEchoServlet;
import org.eclipse.jetty.websocket.server.helper.IncomingFramesCapture;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChromeTest
{
    private static SimpleServletServer server;

    @BeforeClass
    public static void startServer() throws Exception
    {
        server = new SimpleServletServer(new MyEchoServlet());
        server.start();
    }

    @AfterClass
    public static void stopServer()
    {
        server.stop();
    }

    @Test
    public void testUpgradeWithWebkitDeflateExtension() throws Exception
    {
        BlockheadClient client = new BlockheadClient(server.getServerUri());
        try
        {
            client.addExtensions("x-webkit-deflate-frame");
            client.setProtocols("chat");
            client.connect();
            client.sendStandardRequest();
            String response = client.expectUpgradeResponse();
            Assert.assertThat("Response",response,containsString("x-webkit-deflate-frame"));

            // Generate text frame
            String msg = "this is an echo ... cho ... ho ... o";
            client.write(WebSocketFrame.text(msg));

            // Read frame (hopefully text frame)
            IncomingFramesCapture capture = client.readFrames(1,TimeUnit.MILLISECONDS,500);
            WebSocketFrame tf = capture.getFrames().get(0);
            Assert.assertThat("Text Frame.status code",tf.getPayloadAsUTF8(),is(msg));
        }
        finally
        {
            client.close();
        }
    }
}
