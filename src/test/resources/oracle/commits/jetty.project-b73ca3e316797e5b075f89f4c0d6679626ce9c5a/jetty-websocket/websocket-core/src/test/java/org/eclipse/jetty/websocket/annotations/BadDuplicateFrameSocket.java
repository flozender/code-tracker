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
package org.eclipse.jetty.websocket.annotations;

import org.eclipse.jetty.websocket.protocol.Frame;

@WebSocket
public class BadDuplicateFrameSocket
{
    /**
     * The get a frame
     */
    @OnWebSocketFrame
    public void frameMe(Frame frame)
    {
        /* ignore */
    }

    /**
     * This is a duplicate frame type (should throw an exception attempting to use)
     */
    @OnWebSocketFrame
    public void watchMe(Frame frame)
    {
        /* ignore */
    }
}
