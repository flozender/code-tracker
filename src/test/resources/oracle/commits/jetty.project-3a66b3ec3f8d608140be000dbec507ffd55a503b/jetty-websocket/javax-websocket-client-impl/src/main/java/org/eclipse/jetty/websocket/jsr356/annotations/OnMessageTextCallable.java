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

import java.io.Reader;
import java.lang.reflect.Method;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.OnMessage;
import javax.websocket.Session;

import org.eclipse.jetty.websocket.jsr356.annotations.Param.Role;

/**
 * Callable for {@link OnMessage} annotated methods with a whole or partial text messages.
 * <p>
 * Not for use with {@link Reader} based {@link OnMessage} method objects.
 * 
 * @see Decoder.Text
 */
public class OnMessageTextCallable extends OnMessageCallable
{
    private Decoder.Text<?> textDecoder;

    public OnMessageTextCallable(Class<?> pojo, Method method)
    {
        super(pojo,method);
    }

    /**
     * Copy Constructor
     */
    public OnMessageTextCallable(OnMessageCallable copy)
    {
        super(copy);
    }

    public void call(Object endpoint, String str, boolean partialFlag) throws DecodeException
    {
        super.args[idxMessageObject] = textDecoder.decode(str);
        if (idxPartialMessageFlag >= 0)
        {
            super.args[idxPartialMessageFlag] = partialFlag;
        }
        super.call(endpoint,super.args);
    }

    @Override
    public void init(Session session)
    {
        idxMessageObject = findIndexForRole(Role.MESSAGE_TEXT);
        assertRoleRequired(idxMessageObject,"Text Message Object");
        assertDecoderRequired();
        textDecoder = (Decoder.Text<?>)getDecoder();
        super.init(session);
    }
}
