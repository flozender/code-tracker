// ========================================================================
// Copyright (c) 2006-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.eclipse.jetty.server.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;

/* ------------------------------------------------------------ */
/** HandlerList.
 * This extension of {@link org.eclipse.jetty.server.server.handler.HandlerCollection} will call
 * each contained handler in turn until either an exception is thrown, the response 
 * is committed or a positive response status is set.
 */
public class HandlerList extends HandlerCollection
{
    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.jetty.server.server.EventHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response) 
        throws IOException, ServletException
    {
        Handler[] handlers = getHandlers();
        
        if (handlers!=null && isStarted())
        {
            Request base_request = HttpConnection.getCurrentConnection().getRequest();
            for (int i=0;i<handlers.length;i++)
            {
                handlers[i].handle(target,request, response);
                if ( base_request.isHandled())
                    return;
            }
        }
    }
}
