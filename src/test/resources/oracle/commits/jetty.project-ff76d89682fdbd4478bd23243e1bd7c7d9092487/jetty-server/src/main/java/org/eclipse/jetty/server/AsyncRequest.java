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

package org.eclipse.jetty.server;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.jetty.io.AsyncEndPoint;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.Timeout;

/* ------------------------------------------------------------ */
/** Asyncrhonous Request.
 * 
 * 
 *
 */
public class AsyncRequest implements AsyncContext
{
    // STATES:
    private static final int __IDLE=0;         // Idle request
    private static final int __DISPATCHED=1;   // Request dispatched to filter/servlet
    private static final int __SUSPENDING=2;   // Suspend called, but not yet returned to container
    private static final int __REDISPATCHING=3;// resumed while dispatched
    private static final int __SUSPENDED=4;    // Suspended and parked
    private static final int __UNSUSPENDING=5; // Has been scheduled
    private static final int __REDISPATCHED=6; // Request redispatched to filter/servlet
    private static final int __COMPLETING=7;   // complete while dispatched
    private static final int __UNCOMPLETED=8;  // Request is completable
    private static final int __COMPLETE=9;     // Request is complete
    
    // State table
    //                       __HANDLE      __UNHANDLE       __SUSPEND    __REDISPATCH   
    // IDLE */          {  __DISPATCHED,    __Illegal,      __Illegal,      __Illegal  },    
    // DISPATCHED */    {   __Illegal,  __UNCOMPLETED,   __SUSPENDING,       __Ignore  }, 
    // SUSPENDING */    {   __Illegal,    __SUSPENDED,      __Illegal,__REDISPATCHING  },
    // REDISPATCHING */ {   __Illegal,  _REDISPATCHED,      __Ignored,       __Ignore  },
    // COMPLETING */    {   __Illegal,  __UNCOMPLETED,      __Illegal,       __Illegal },
    // SUSPENDED */     {  __REDISPATCHED,  __Illegal,      __Illegal, __UNSUSPENDING  },
    // UNSUSPENDING */  {  __REDISPATCHED,  __Illegal,      __Illegal,       __Ignore  },
    // REDISPATCHED */  {   __Illegal,  __UNCOMPLETED,   __SUSPENDING,       __Ignore  },
    

    /* ------------------------------------------------------------ */
    protected HttpConnection _connection;
    protected Object _listeners;

    /* ------------------------------------------------------------ */
    private int _state;
    private boolean _initial;
    private long _timeoutMs;
    private AsyncEventState _event;

    /* ------------------------------------------------------------ */
    protected AsyncRequest()
    {
        _state=__IDLE;
        _initial=true;
    }
    
    /* ------------------------------------------------------------ */
    protected AsyncRequest(final HttpConnection connection)
    {
        this();
        if (connection!=null)
            setConnection(connection);
    }

    /* ------------------------------------------------------------ */
    protected void setConnection(final HttpConnection connection)
    {
        _connection=connection;
    }

    /* ------------------------------------------------------------ */
    public void setAsyncTimeout(long ms)
    {
        _timeoutMs=ms;
    } 

    /* ------------------------------------------------------------ */
    public long getAsyncTimeout()
    {
        return _timeoutMs;
    } 

    /* ------------------------------------------------------------ */
    public AsyncEventState getAsyncEventState()
    {
        return _event;
    } 
   
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#isInitial()
     */
    public boolean isInitial()
    {
        synchronized(this)
        {
            return _initial;
        }
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#isSuspended()
     */
    public boolean isSuspended()
    {
        synchronized(this)
        {
            switch(_state)
            {
                case __SUSPENDING:
                case __REDISPATCHING:
                case __COMPLETING:
                case __SUSPENDED:
                    return true;
                    
                default:
                    return false;   
            }
        }
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return getStatusString();
    }

    /* ------------------------------------------------------------ */
    public String getStatusString()
    {
        synchronized (this)
        {
            return
            ((_state==__IDLE)?"IDLE":
                (_state==__DISPATCHED)?"DISPATCHED":
                    (_state==__SUSPENDING)?"SUSPENDING":
                        (_state==__SUSPENDED)?"SUSPENDED":
                            (_state==__REDISPATCHING)?"REDISPATCHING":
                                (_state==__UNSUSPENDING)?"UNSUSPENDING":
                                    (_state==__REDISPATCHED)?"REDISPATCHED":
                                        (_state==__COMPLETING)?"COMPLETING":
                                            (_state==__UNCOMPLETED)?"UNCOMPLETED":
                                                (_state==__COMPLETE)?"COMPLETE":
                                                    ("UNKNOWN?"+_state))+
            (_initial?",initial":"");
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#resume()
     */
    protected boolean handling()
    {
        synchronized (this)
        {
            switch(_state)
            {
                case __DISPATCHED:
                case __REDISPATCHED:
                case __COMPLETE:
                    throw new IllegalStateException(this.getStatusString());

                case __IDLE:
                    _initial=true;
                    _state=__DISPATCHED;
                    return true;

                case __SUSPENDING:
                case __REDISPATCHING:
                    throw new IllegalStateException(this.getStatusString());

                case __COMPLETING:
                    _state=__UNCOMPLETED;
                    return false;

                case __SUSPENDED:
                    cancelTimeout();
                case __UNSUSPENDING:
                    _state=__REDISPATCHED;
                    return true;

                default:
                    throw new IllegalStateException(""+_state);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#suspend(long)
     */
    protected void suspend(final ServletContext context,
            final ServletRequest request,
            final ServletResponse response)
    {
        synchronized (this)
        {
            _event=new AsyncEventState(context,request,response);
            
            switch(_state)
            {
                case __DISPATCHED:
                case __REDISPATCHED:
                    _state=__SUSPENDING;
                    return;

                case __IDLE:
                    throw new IllegalStateException(this.getStatusString());

                case __SUSPENDING:
                case __REDISPATCHING:
                    return;

                case __COMPLETING:
                case __SUSPENDED:
                case __UNSUSPENDING:
                case __COMPLETE:
                    throw new IllegalStateException(this.getStatusString());

                default:
                    throw new IllegalStateException(""+_state);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Signal that the HttpConnection has finished handling the request.
     * For blocking connectors, this call may block if the request has
     * been suspended (startAsync called).
     * @return true if handling is complete, false if the request should 
     * be handled again (eg because of a resume that happened before unhandle was called)
     */
    protected boolean unhandle()
    {
        synchronized (this)
        {
            switch(_state)
            {
                case __REDISPATCHED:
                case __DISPATCHED:
                    _state=__UNCOMPLETED;
                    return true;

                case __IDLE:
                    throw new IllegalStateException(this.getStatusString());

                case __SUSPENDING:
                    _initial=false;
                    _state=__SUSPENDED;
                    scheduleTimeout(); // could block and change state.
                    if (_state==__SUSPENDED)
                        return true;
                    else if (_state==__COMPLETING)
                    {
                        _state=__UNCOMPLETED;
                        return true;
                    }
                    _initial=false;
                    _state=__REDISPATCHED;
                    return false; 

                case __REDISPATCHING:
                    _initial=false;
                    _state=__REDISPATCHED;
                    return false; 

                case __COMPLETING:
                    _initial=false;
                    _state=__UNCOMPLETED;
                    return true;

                case __SUSPENDED:
                case __UNSUSPENDING:
                default:
                    throw new IllegalStateException(this.getStatusString());
            }
        }
    }

    /* ------------------------------------------------------------ */
    public void dispatch()
    {
        boolean dispatch=false;
        synchronized (this)
        {
            switch(_state)
            {
                case __REDISPATCHED:
                case __DISPATCHED:
                case __IDLE:
                case __REDISPATCHING:
                case __COMPLETING:
                case __COMPLETE:
                case __UNCOMPLETED:
                    return;
                    
                case __SUSPENDING:
                    _state=__REDISPATCHING;
                    return;

                case __SUSPENDED:
                    dispatch=true;
                    _state=__UNSUSPENDING;
                    break;
                    
                case __UNSUSPENDING:
                    return;
                    
                default:
                    throw new IllegalStateException(this.getStatusString());
            }
        }
        
        if (dispatch)
        {
            cancelTimeout();
            scheduleDispatch();
        }
    }

    /* ------------------------------------------------------------ */
    protected void expired()
    {
        synchronized (this)
        {
            switch(_state)
            {
                case __SUSPENDING:
                case __SUSPENDED:
                    break;
                default:
                    return;
            }
        }
        
        if (_listeners!=null)
        {
            for(int i=0;i<LazyList.size(_listeners);i++)
            {
                try
                {
                    AsyncListener listener=((AsyncListener)LazyList.get(_listeners,i));
                    listener.onTimeout(_event);
                }
                catch(Exception e)
                {
                    Log.warn(e);
                }
            }
        }
        
        synchronized (this)
        {
            switch(_state)
            {
                case __SUSPENDING:
                case __SUSPENDED:
                    if (false) // TODO 
                        dispatch();
                    else
                        complete();
                default:
                    return;
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#complete()
     */
    public void complete()
    {
        // just like resume, except don't set _resumed=true;
        boolean dispatch=false;
        synchronized (this)
        {
            switch(_state)
            {
                case __IDLE:
                case __COMPLETE:
                case __REDISPATCHING:
                case __COMPLETING:
                case __UNSUSPENDING:
                    return;
                    
                case __DISPATCHED:
                case __REDISPATCHED:
                    throw new IllegalStateException(this.getStatusString());

                case __SUSPENDING:
                    _state=__COMPLETING;
                    return;
                    
                case __SUSPENDED:
                    _state=__COMPLETING;
                    dispatch=true;
                    break;
                    
                default:
                    throw new IllegalStateException(this.getStatusString());
            }
        }
        
        if (dispatch)
        {
            cancelTimeout();
            scheduleDispatch();
        }
    }

    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#complete()
     */
    protected void doComplete()
    {
        synchronized (this)
        {
            switch(_state)
            {
                case __UNCOMPLETED:
                    _state=__COMPLETE;
                    break;
                    
                default:
                    throw new IllegalStateException(this.getStatusString());
            }
        }

        if (_listeners!=null)
        {
            for(int i=0;i<LazyList.size(_listeners);i++)
            {
                try
                {
                    ((AsyncListener)LazyList.get(_listeners,i)).onComplete(_event);
                }
                catch(Exception e)
                {
                    Log.warn(e);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    protected void recycle()
    {
        synchronized (this)
        {
            switch(_state)
            {
                case __DISPATCHED:
                case __REDISPATCHED:
                    throw new IllegalStateException(getStatusString());
                default:
                    _state=__IDLE;
            }
            _initial = true;
            cancelTimeout();
            _event=null;
            _timeoutMs=60000L; // TODO configure
            _listeners=null;
        }
    }    
    
    /* ------------------------------------------------------------ */
    public void cancel()
    {
        synchronized (this)
        {
            _state=__COMPLETE;
            _initial = false;
            cancelTimeout();
            _event=null;
            _listeners=null;
        }
    }

    /* ------------------------------------------------------------ */
    protected void scheduleDispatch()
    {
        EndPoint endp=_connection.getEndPoint();
        if (!endp.isBlocking())
        {
            ((AsyncEndPoint)endp).dispatch();
        }
    }

    /* ------------------------------------------------------------ */
    protected void scheduleTimeout()
    {
        EndPoint endp=_connection.getEndPoint();
        if (endp.isBlocking())
        {
            synchronized(this)
            {
                long expire_at = System.currentTimeMillis()+_timeoutMs;
                long wait=_timeoutMs;
                while (_timeoutMs>0 && wait>0)
                {
                    try
                    {
                        this.wait(wait);
                    }
                    catch (InterruptedException e)
                    {
                        Log.ignore(e);
                    }
                    wait=expire_at-System.currentTimeMillis();
                }

                if (_timeoutMs>0 && wait<=0)
                    expired();
            }            
        }
        else
            _connection.scheduleTimeout(_event._timeout,_timeoutMs);
    }

    /* ------------------------------------------------------------ */
    protected void cancelTimeout()
    {
        EndPoint endp=_connection.getEndPoint();
        if (endp.isBlocking())
        {
            synchronized(this)
            {
                _timeoutMs=0;
                this.notifyAll();
            }
        }
        else if (_event!=null)
            _connection.cancelTimeout(_event._timeout);
    }

    /* ------------------------------------------------------------ */
    public boolean isCompleting()
    {
        return _state==__COMPLETING;
    }
    
    /* ------------------------------------------------------------ */
    boolean isUncompleted()
    {
        return _state==__UNCOMPLETED;
    } 
    
    /* ------------------------------------------------------------ */
    public boolean isComplete()
    {
        return _state==__COMPLETE;
    }


    /* ------------------------------------------------------------ */
    public boolean isAsyncStarted()
    {
        switch(_state)
        {
            case __SUSPENDING:
            case __REDISPATCHING:
            case __UNSUSPENDING:
            case __SUSPENDED:
                return true;
                
            default:
            return false;
        }
    }


    /* ------------------------------------------------------------ */
    public boolean isAsync()
    {
        switch(_state)
        {
            case __IDLE:
            case __DISPATCHED:
                return false;
                
            default:
            return true;
        }
    }

    /* ------------------------------------------------------------ */
    public void dispatch(ServletContext context, String path)
    {
        _event._dispatchContext=context;
        _event._path=path;
        dispatch();
    }

    /* ------------------------------------------------------------ */
    public void dispatch(String path)
    {
        _event._path=path;
        dispatch();
    }

    /* ------------------------------------------------------------ */
    public ServletRequest getRequest()
    {
        if (_event!=null)
            return _event.getRequest();
        return _connection.getRequest();
    }

    /* ------------------------------------------------------------ */
    public ServletResponse getResponse()
    {
        if (_event!=null)
            return _event.getResponse();
        return _connection.getResponse();
    }

    /* ------------------------------------------------------------ */
    public void start(Runnable run)
    {
        ((Context)_event.getServletContext()).getContextHandler().handle(run);
    }

    /* ------------------------------------------------------------ */
    public boolean hasOriginalRequestAndResponse()
    {
        return (_event!=null && _event.getRequest()==_connection._request && _event.getResponse()==_connection._response);
    }

    /* ------------------------------------------------------------ */
    public ContextHandler getContextHandler()
    {
        if (_event!=null)
            return ((Context)_event.getServletContext()).getContextHandler();
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public class AsyncEventState extends AsyncEvent
    {
        final Timeout.Task _timeout;
        final ServletContext _suspendedContext;
        ServletContext _dispatchContext;
        String _path;
        
        public AsyncEventState(ServletContext context, ServletRequest request, ServletResponse response)
        {
            super(request,response);
            _suspendedContext=context;
            _timeout= new Timeout.Task()
            {
                public void expired()
                {
                    AsyncRequest.this.expired();
                }
            };
        }
        
        public ServletContext getSuspendedContext()
        {
            return _suspendedContext;
        }
        
        public ServletContext getDispatchContext()
        {
            return _dispatchContext;
        }
        
        public ServletContext getServletContext()
        {
            return _dispatchContext==null?_suspendedContext:_dispatchContext;
        }
        
        public String getPath()
        {
            return _path;
        }   
    }
}
