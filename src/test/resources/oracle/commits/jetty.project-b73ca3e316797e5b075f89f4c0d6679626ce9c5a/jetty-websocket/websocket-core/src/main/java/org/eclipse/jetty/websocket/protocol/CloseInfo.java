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
package org.eclipse.jetty.websocket.protocol;

import java.nio.ByteBuffer;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.Utf8Appendable.NotUtf8Exception;
import org.eclipse.jetty.util.Utf8StringBuilder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.BadPayloadException;
import org.eclipse.jetty.websocket.api.ProtocolException;
import org.eclipse.jetty.websocket.api.StatusCode;

public class CloseInfo
{
    private static final Logger LOG = Log.getLogger(CloseInfo.class);
    private int statusCode;
    private String reason;

    public CloseInfo()
    {
        this(-1,null);
    }

    public CloseInfo(ByteBuffer payload, boolean validate)
    {
        this.statusCode = -1;
        this.reason = null;

        if ((payload == null) || (payload.remaining() == 0))
        {
            return; // nothing to do
        }

        ByteBuffer data = payload.slice();
        if ((data.remaining() == 1) && (validate))
        {
            throw new ProtocolException("Invalid 1 byte payload");
        }

        if (data.remaining() >= 2)
        {
            // Status Code
            statusCode = 0; // start with 0
            statusCode |= (data.get() & 0xFF) << 8;
            statusCode |= (data.get() & 0xFF);

            if(validate) {
                if ((statusCode < StatusCode.NORMAL) || (statusCode == StatusCode.UNDEFINED) || (statusCode == StatusCode.NO_CLOSE)
                        || (statusCode == StatusCode.NO_CODE) || ((statusCode > 1011) && (statusCode <= 2999)) || (statusCode >= 5000))
                {
                    throw new ProtocolException("Invalid close code: " + statusCode);
                }
            }

            if (data.remaining() > 0)
            {
                // Reason
                try
                {
                    Utf8StringBuilder utf = new Utf8StringBuilder();
                    utf.append(data);
                    reason = utf.toString();
                }
                catch (NotUtf8Exception e)
                {
                    if (validate)
                    {
                        throw new BadPayloadException("Invalid Close Reason",e);
                    }
                    else
                    {
                        LOG.warn(e);
                    }
                }
                catch (RuntimeException e)
                {
                    if (validate)
                    {
                        throw new ProtocolException("Invalid Close Reason",e);
                    }
                    else
                    {
                        LOG.warn(e);
                    }
                }
            }
        }
    }

    public CloseInfo(int statusCode)
    {
        this(statusCode, null);
    }

    public CloseInfo(int statusCode, String reason)
    {
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public CloseInfo(WebSocketFrame frame)
    {
        this(frame.getPayload(),false);
    }

    public CloseInfo(WebSocketFrame frame, boolean validate)
    {
        this(frame.getPayload(),validate);
    }

    public ByteBuffer asByteBuffer()
    {
        if (statusCode != (-1))
        {
            ByteBuffer buf = ByteBuffer.allocate(WebSocketFrame.MAX_CONTROL_PAYLOAD);
            buf.putChar((char)statusCode);
            if (StringUtil.isNotBlank(reason))
            {
                byte utf[] = StringUtil.getUtf8Bytes(reason);
                buf.put(utf,0,utf.length);
            }
            BufferUtil.flipToFlush(buf,0);
            return buf;
        }
        return null;
    }

    public WebSocketFrame asFrame()
    {
        WebSocketFrame frame = new WebSocketFrame(OpCode.CLOSE);
        frame.setFin(true);
        frame.setPayload(asByteBuffer());
        return frame;
    }

    public String getReason()
    {
        return reason;
    }

    public int getStatusCode()
    {
        return statusCode;
    }
}
