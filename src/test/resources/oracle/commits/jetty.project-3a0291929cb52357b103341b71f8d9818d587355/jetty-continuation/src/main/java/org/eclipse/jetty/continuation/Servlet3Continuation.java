//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.continuation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;


/* ------------------------------------------------------------ */
/**
 * This implementation of Continuation is used by {@link ContinuationSupport}
 * when it detects that the application has been deployed in a non-jetty Servlet 3
 * server.
 */
public class Servlet3Continuation implements Continuation
{
    // Exception reused for all continuations
    // Turn on debug in ContinuationFilter to see real stack trace.
    private final static ContinuationThrowable __exception = new ContinuationThrowable();

    private final ServletRequest _request;
    private ServletResponse _response;
    private AsyncContext _context;
    private final List<AsyncListener> _listeners=new ArrayList<AsyncListener>();
    private volatile boolean _initial=true;
    private volatile boolean _resumed=false;
    private volatile boolean _expired=false;
    private volatile boolean _responseWrapped=false;

    private long _timeoutMs=-1;

    /* ------------------------------------------------------------ */
    public Servlet3Continuation(ServletRequest request)
    {
        _request=request;

        _listeners.add(new AsyncListener()
        {
            @Override
            public void onComplete(AsyncEvent event) throws IOException
            {
            }

            @Override
            public void onError(AsyncEvent event) throws IOException
            {
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException
            {
                event.getAsyncContext().addListener(this);
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException
            {
                _initial=false;
                event.getAsyncContext().dispatch();
            }
        });
    }

    /* ------------------------------------------------------------ */
    @Override
    public void addContinuationListener(final ContinuationListener listener)
    {
        AsyncListener wrapped = new AsyncListener()
        {
            @Override
            public void onComplete(final AsyncEvent event) throws IOException
            {
                listener.onComplete(Servlet3Continuation.this);
            }

            @Override
            public void onError(AsyncEvent event) throws IOException
            {
                listener.onComplete(Servlet3Continuation.this);
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException
            {
                event.getAsyncContext().addListener(this);
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException
            {
                _expired=true;
                listener.onTimeout(Servlet3Continuation.this);
            }
        };

        if (_context!=null)
            _context.addListener(wrapped);
        else
            _listeners.add(wrapped);
    }

    /* ------------------------------------------------------------ */
    @Override
    public void complete()
    {
        AsyncContext context=_context;
        if (context==null)
            throw new IllegalStateException();
        _context.complete();
    }

    /* ------------------------------------------------------------ */
    @Override
    public ServletResponse getServletResponse()
    {
        return _response;
    }

    /* ------------------------------------------------------------ */
    @Override
    public boolean isExpired()
    {
        return _expired;
    }

    /* ------------------------------------------------------------ */
    @Override
    public boolean isInitial()
    {
        return _initial&&_request.getDispatcherType()!=DispatcherType.ASYNC;
    }

    /* ------------------------------------------------------------ */
    @Override
    public boolean isResumed()
    {
        return _resumed;
    }

    /* ------------------------------------------------------------ */
    @Override
    public boolean isSuspended()
    {
        return _request.isAsyncStarted();
    }

    /* ------------------------------------------------------------ */
    public void keepWrappers()
    {
        _responseWrapped=true;
    }

    /* ------------------------------------------------------------ */
    @Override
    public void resume()
    {
        AsyncContext context=_context;
        if (context==null)
            throw new IllegalStateException();
        _resumed=true;
        _context.dispatch();
    }

    /* ------------------------------------------------------------ */
    @Override
    public void setTimeout(long timeoutMs)
    {
        _timeoutMs=timeoutMs;
        if (_context!=null)
            _context.setTimeout(timeoutMs);
    }

    /* ------------------------------------------------------------ */
    @Override
    public void suspend(ServletResponse response)
    {
        _response=response;
        _responseWrapped=response instanceof ServletResponseWrapper;
        _resumed=false;
        _expired=false;
        _context=_request.startAsync();
        _context.setTimeout(_timeoutMs);

        for (AsyncListener listener:_listeners)
            _context.addListener(listener);
        _listeners.clear();
    }

    /* ------------------------------------------------------------ */
    @Override
    public void suspend()
    {
        _resumed=false;
        _expired=false;
        _context=_request.startAsync();
        _context.setTimeout(_timeoutMs);

        for (AsyncListener listener:_listeners)
            _context.addListener(listener);
        _listeners.clear();
    }

    /* ------------------------------------------------------------ */
    @Override
    public boolean isResponseWrapped()
    {
        return _responseWrapped;
    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.continuation.Continuation#getAttribute(java.lang.String)
     */
    @Override
    public Object getAttribute(String name)
    {
        return _request.getAttribute(name);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.continuation.Continuation#removeAttribute(java.lang.String)
     */
    @Override
    public void removeAttribute(String name)
    {
        _request.removeAttribute(name);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.continuation.Continuation#setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void setAttribute(String name, Object attribute)
    {
        _request.setAttribute(name,attribute);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.continuation.Continuation#undispatch()
     */
    @Override
    public void undispatch()
    {
        if (isSuspended())
        {
            if (ContinuationFilter.__debug)
                throw new ContinuationThrowable();
            throw __exception;
        }
        throw new IllegalStateException("!suspended");
    }
}
