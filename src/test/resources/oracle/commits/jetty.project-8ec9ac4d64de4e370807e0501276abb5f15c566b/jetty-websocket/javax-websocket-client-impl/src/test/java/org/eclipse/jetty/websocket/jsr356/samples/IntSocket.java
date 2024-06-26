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

package org.eclipse.jetty.websocket.jsr356.samples;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;
import javax.websocket.WebSocketClient;
import javax.websocket.WebSocketMessage;

@WebSocketClient(decoders = { DualDecoder.class })
public class IntSocket
{
    @WebSocketMessage
    public void onInt(Session session, int value)
    {
        try
        {
            session.getRemote().sendObject(value);
        }
        catch (IOException | EncodeException e)
        {
            e.printStackTrace();
        }
    }
}
