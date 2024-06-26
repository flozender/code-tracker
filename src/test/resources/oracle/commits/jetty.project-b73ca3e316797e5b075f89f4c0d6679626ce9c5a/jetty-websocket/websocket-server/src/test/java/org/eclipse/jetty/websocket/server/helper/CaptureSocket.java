// ========================================================================
// Copyright 2011-2012 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
//
//     The Eclipse Public License is available at
//     http://www.eclipse.org/legal/epl-v10.html
//
//     The Apache License v2.0 is available at
//     http://www.opensource.org/licenses/apache2.0.php
//
// You may elect to redistribute this code under either of these licenses.
//========================================================================
package org.eclipse.jetty.websocket.server.helper;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class CaptureSocket extends WebSocketAdapter
{
    private final CountDownLatch latch = new CountDownLatch(1);
    public List<String> messages;

    public CaptureSocket()
    {
        messages = new ArrayList<String>();
    }

    public boolean awaitConnected(long timeout) throws InterruptedException
    {
        return latch.await(timeout,TimeUnit.MILLISECONDS);
    }

    public void onClose(int closeCode, String message)
    {
    }

    public void onOpen(Connection connection)
    {
        latch.countDown();
    }

    @Override
    public void onWebSocketText(String message)
    {
        // System.out.printf("Received Message \"%s\" [size %d]%n", message, message.length());
        messages.add(message);
    }
}
