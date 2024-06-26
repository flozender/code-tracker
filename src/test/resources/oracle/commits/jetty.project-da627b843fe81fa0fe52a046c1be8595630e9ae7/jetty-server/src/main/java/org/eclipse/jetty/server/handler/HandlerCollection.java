// ========================================================================
// Copyright (c) 2004-2009 Mort Bay Consulting Pty. Ltd.
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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.MultiException;

/* ------------------------------------------------------------ */
/** A collection of handlers.  
 * <p>
 * For derived HandlerCollections, the order or manner of calling
 * the contained handlers is not defined. The default implementations
 * calls all handlers in list order, regardless of 
 * the response status or exceptions.
 * <p>
 *  
 * 
 * @org.apache.xbean.XBean
 */
public class HandlerCollection extends AbstractHandlerContainer
{
    private Handler[] _handlers;

    /* ------------------------------------------------------------ */
    public HandlerCollection()
    {
        super();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the handlers.
     */
    public Handler[] getHandlers()
    {
        return _handlers;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     * @param handlers The handlers to set.
     */
    public void setHandlers(Handler[] handlers)
    {
        Handler [] old_handlers = _handlers==null?null:(Handler[])_handlers.clone();
        
        if (getServer()!=null)
            getServer().getContainer().update(this, old_handlers, handlers, "handler");
        
        Server server = getServer();
        MultiException mex = new MultiException();
        for (int i=0;handlers!=null && i<handlers.length;i++)
        {
            if (handlers[i].getServer()!=server)
                handlers[i].setServer(server);
        }

        // quasi atomic.... so don't go doing this under load on a SMP system.
        _handlers = handlers;

        for (int i=0;old_handlers!=null && i<old_handlers.length;i++)
        {
            if (old_handlers[i]!=null)
            {
                try
                {
                    if (old_handlers[i].isStarted())
                        old_handlers[i].stop();
                }
                catch (Throwable e)
                {
                    mex.add(e);
                }
            }
        }
                
        mex.ifExceptionThrowRuntime();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.jetty.server.server.EventHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response) 
        throws IOException, ServletException
    {
        if (_handlers!=null && isStarted())
        {
            MultiException mex=null;
            
            for (int i=0;i<_handlers.length;i++)
            {
                try
                {
                    _handlers[i].handle(target,request, response);
                }
                catch(IOException e)
                {
                    throw e;
                }
                catch(RuntimeException e)
                {
                    throw e;
                }
                catch(Exception e)
                {
                    if (mex==null)
                        mex=new MultiException();
                    mex.add(e);
                }
            }
            if (mex!=null)
            {
                if (mex.size()==1)
                    throw new ServletException(mex.getThrowable(0));
                else
                    throw new ServletException(mex);
            }
            
        }    
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.jetty.server.server.handler.AbstractHandler#doStart()
     */
    protected void doStart() throws Exception
    {
        MultiException mex=new MultiException();
        if (_handlers!=null)
        {
            for (int i=0;i<_handlers.length;i++)
                try{_handlers[i].start();}catch(Throwable e){mex.add(e);}
        }
        super.doStart();
        mex.ifExceptionThrow();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.eclipse.jetty.server.server.handler.AbstractHandler#doStop()
     */
    protected void doStop() throws Exception
    {
        MultiException mex=new MultiException();
        try { super.doStop(); } catch(Throwable e){mex.add(e);}
        if (_handlers!=null)
        {
            for (int i=_handlers.length;i-->0;)
                try{_handlers[i].stop();}catch(Throwable e){mex.add(e);}
        }
        mex.ifExceptionThrow();
    }
    
    /* ------------------------------------------------------------ */
    public void setServer(Server server)
    {
        Server old_server=getServer();
        
        super.setServer(server);

        Handler[] h=getHandlers();
        for (int i=0;h!=null && i<h.length;i++)
            h[i].setServer(server);
        
        if (server!=null && server!=old_server)
            server.getContainer().update(this, null,_handlers, "handler");
        
    }

    /* ------------------------------------------------------------ */
    /* Add a handler.
     * This implementation adds the passed handler to the end of the existing collection of handlers. 
     * @see org.eclipse.jetty.server.server.HandlerContainer#addHandler(org.eclipse.jetty.server.server.Handler)
     */
    public void addHandler(Handler handler)
    {
        setHandlers((Handler[])LazyList.addToArray(getHandlers(), handler, Handler.class));
    }
    
    /* ------------------------------------------------------------ */
    public void removeHandler(Handler handler)
    {
        Handler[] handlers = getHandlers();
        
        if (handlers!=null && handlers.length>0 )
            setHandlers((Handler[])LazyList.removeFromArray(handlers, handler));
    }

    /* ------------------------------------------------------------ */
    protected Object expandChildren(Object list, Class byClass)
    {
        Handler[] handlers = getHandlers();
        for (int i=0;handlers!=null && i<handlers.length;i++)
            list=expandHandler(handlers[i], list, byClass);
        return list;
    }


}
