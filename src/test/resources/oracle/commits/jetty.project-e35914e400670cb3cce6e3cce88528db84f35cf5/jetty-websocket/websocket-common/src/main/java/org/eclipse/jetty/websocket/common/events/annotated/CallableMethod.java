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

package org.eclipse.jetty.websocket.common.events.annotated;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.WebSocketException;

/**
 * A Callable Method
 */
public class CallableMethod
{
    private static final Logger LOG = Log.getLogger(CallableMethod.class);
    protected final Class<?> pojo;
    protected final Method method;
    protected Class<?>[] paramTypes;

    public CallableMethod(Class<?> pojo, Method method)
    {
        this.pojo = pojo;
        this.method = method;
        this.paramTypes = method.getParameterTypes();
    }

    public void call(Object obj, Object... args)
    {
        if ((this.pojo == null) || (this.method == null))
        {
            LOG.warn("Cannot execute call: pojo={}, method={}",pojo,method);
            return; // no call event method determined
        }

        if (obj == null)
        {
            LOG.warn("Cannot call {} on null object",this.method);
            return;
        }

        if (args.length < paramTypes.length)
        {
            throw new IllegalArgumentException("Call arguments length [" + args.length + "] must always be greater than or equal to captured args length ["
                    + paramTypes.length + "]");
        }

        try
        {
            this.method.invoke(obj,args);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            String err = String.format("Cannot call method %s on %s with args: %s",method,pojo,args);
            throw new WebSocketException(err,e);
        }
    }

    public Method getMethod()
    {
        return method;
    }

    public Class<?>[] getParamTypes()
    {
        return paramTypes;
    }

    public Class<?> getPojo()
    {
        return pojo;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]",this.getClass().getSimpleName(),method.toGenericString());
    }
}