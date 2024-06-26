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

package org.eclipse.jetty.websocket.jsr356.annotations;

import java.nio.ByteBuffer;

import javax.websocket.OnMessage;
import javax.websocket.Session;

import org.eclipse.jetty.websocket.common.events.annotated.InvalidSignatureException;
import org.eclipse.jetty.websocket.jsr356.annotations.Param.Role;
import org.eclipse.jetty.websocket.jsr356.decoders.ByteArrayDecoder;
import org.eclipse.jetty.websocket.jsr356.decoders.ByteBufferDecoder;

/**
 * Param handling for static Binary &#064;{@link OnMessage} parameters.
 */
public class JsrParamIdBinary extends JsrParamIdOnMessage implements IJsrParamId
{
    public static final IJsrParamId INSTANCE = new JsrParamIdBinary();

    @Override
    public boolean process(Param param, JsrCallable callable) throws InvalidSignatureException
    {
        // Session parameter (optional)
        if (param.type.isAssignableFrom(Session.class))
        {
            param.bind(Role.SESSION);
            return true;
        }

        if (param.type.isAssignableFrom(ByteBuffer.class))
        {
            param.bind(Role.MESSAGE_BINARY);
            callable.setDecoder(ByteBufferDecoder.INSTANCE);
            return true;
        }

        if (param.type.isAssignableFrom(byte[].class))
        {
            param.bind(Role.MESSAGE_BINARY);
            callable.setDecoder(ByteArrayDecoder.INSTANCE);
            return true;
        }

        // Boolean (for indicating partial message support)
        if (param.type.isAssignableFrom(Boolean.TYPE))
        {
            param.bind(Role.MESSAGE_PARTIAL_FLAG);
            return true;
        }
        return false;
    }
}
