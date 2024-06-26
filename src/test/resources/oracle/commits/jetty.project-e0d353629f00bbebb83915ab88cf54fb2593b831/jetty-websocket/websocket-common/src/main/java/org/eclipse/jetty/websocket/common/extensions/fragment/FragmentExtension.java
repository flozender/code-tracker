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

package org.eclipse.jetty.websocket.common.extensions.fragment;


import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.common.OpCode;
import org.eclipse.jetty.websocket.common.WebSocketFrame;
import org.eclipse.jetty.websocket.common.extensions.AbstractExtension;

/**
 * Fragment Extension
 */
public class FragmentExtension extends AbstractExtension
{
    private int maxLength = -1;

    @Override
    public void incomingError(Throwable e)
    {
        // Pass thru
        nextIncomingError(e);
    }

    @Override
    public void incomingFrame(Frame frame)
    {
        // Pass thru
        nextIncomingFrame(frame);
    }

    @Override
    public void outgoingFrame(Frame frame, WriteCallback callback)
    {
        if (frame.getType().isControl())
        {
            // Cannot fragment Control Frames
            nextOutgoingFrame(frame,callback);
            return;
        }

        int length = frame.getPayloadLength();

        byte opcode = frame.getType().getOpCode(); // original opcode
        ByteBuffer payload = frame.getPayload().slice();
        int originalLimit = payload.limit();
        int currentPosition = payload.position();

        if (maxLength <= 0)
        {
            // output original frame
            nextOutgoingFrame(frame,callback);
            return;
        }

        boolean continuation = false;

        // break apart payload based on maxLength rules
        while (length > maxLength)
        {
            WebSocketFrame frag = new WebSocketFrame(frame);
            frag.setOpCode(opcode);
            frag.setFin(false); // always false here
            frag.setContinuation(continuation);
            payload.position(currentPosition);
            payload.limit(Math.min(payload.position() + maxLength,originalLimit));
            frag.setPayload(payload);

            // no callback for beginning and middle parts
            nextOutgoingFrame(frag,null);

            length -= maxLength;
            opcode = OpCode.CONTINUATION;
            continuation = true;
            currentPosition = payload.limit();
        }

        // write remaining
        WebSocketFrame frag = new WebSocketFrame(frame);
        frag.setOpCode(opcode);
        frag.setFin(frame.isFin()); // use original fin
        frag.setContinuation(continuation);
        payload.position(currentPosition);
        payload.limit(originalLimit);
        frag.setPayload(payload);

        nextOutgoingFrame(frag,callback);
    }

    @Override
    public void setConfig(ExtensionConfig config)
    {
        super.setConfig(config);

        maxLength = config.getParameter("maxLength",maxLength);
    }
}
