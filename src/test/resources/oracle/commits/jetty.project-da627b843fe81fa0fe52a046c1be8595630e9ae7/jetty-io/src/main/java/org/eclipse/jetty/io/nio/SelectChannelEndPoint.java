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

package org.eclipse.jetty.io.nio;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.eclipse.jetty.io.AsyncEndPoint;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.HttpException;
import org.eclipse.jetty.io.nio.SelectorManager.SelectSet;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.Timeout;

/* ------------------------------------------------------------ */
/**
 * An Endpoint that can be scheduled by {@link SelectorManager}.
 * 
 * 
 *
 */
public class SelectChannelEndPoint extends ChannelEndPoint implements Runnable, AsyncEndPoint
{
    protected SelectorManager _manager;
    protected SelectorManager.SelectSet _selectSet;
    protected boolean _dispatched = false;
    protected boolean _redispatched = false;
    protected boolean _writable = true; 
    protected SelectionKey _key;
    protected int _interestOps;
    protected boolean _readBlocked;
    protected boolean _writeBlocked;
    protected Connection _connection;
    private boolean _open;
    private Timeout.Task _idleTask = new IdleTask();

    /* ------------------------------------------------------------ */
    public Connection getConnection()
    {
        return _connection;
    }
    
    /* ------------------------------------------------------------ */
    public SelectChannelEndPoint(SocketChannel channel, SelectSet selectSet, SelectionKey key)
    {
        super(channel);

        _manager = selectSet.getManager();
        _selectSet = selectSet;
        _connection = _manager.newConnection(channel,this);
        _dispatched = false;
        _redispatched = false;
        _open=true;
        _manager.endPointOpened(this);
        
        _key = key;
        scheduleIdle();
    }

    /* ------------------------------------------------------------ */
    /** Called by selectSet to schedule handling
     * 
     */
    public void schedule() throws IOException
    {
        // If threads are blocked on this
        synchronized (this)
        {
            // If there is no key, then do nothing
            if (_key == null || !_key.isValid())
            {
                _readBlocked=false;
                _writeBlocked=false;
                this.notifyAll();
                return;
            }
            
            // If there are threads dispatched reading and writing
            if (_readBlocked || _writeBlocked)
            {
                // assert _dispatched;
                if (_readBlocked && _key.isReadable())
                    _readBlocked=false;
                if (_writeBlocked && _key.isWritable())
                    _writeBlocked=false;

                // wake them up is as good as a dispatched.
                this.notifyAll();
                
                // we are not interested in further selecting
                _key.interestOps(0);
                return;
            }
            
            // Otherwise if we are still dispatched
            if (!isReadyForDispatch())
            {
                // we are not interested in further selecting
                _key.interestOps(0);
                return;
            }
            
            // Remove writeable op
            if ((_key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE && (_key.interestOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE)
            {
                // Remove writeable op
                _interestOps = _key.interestOps() & ~SelectionKey.OP_WRITE;
                _key.interestOps(_interestOps);
                _writable = true; // Once writable is in ops, only removed with dispatch.
            }

            if (!dispatch())
                updateKey();
        }
    }
        
    /* ------------------------------------------------------------ */
    public boolean dispatch() 
    {
        synchronized(this)
        {
            if (_dispatched)
            {
                _redispatched=true;
                return true;
            }

            _dispatched = _manager.dispatch((Runnable)this);   
            if(!_dispatched)
                Log.warn("Dispatched Failed!");
            return _dispatched;
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Called when a dispatched thread is no longer handling the endpoint. The selection key
     * operations are updated.
     */
    protected boolean undispatch()
    {
        synchronized (this)
        {
            if (_redispatched)
            {
                _redispatched=false;
                return false;
            }
            _dispatched = false;
            updateKey();
        }
        return true;
    }

    /* ------------------------------------------------------------ */
    public void scheduleIdle()
    {
        _selectSet.scheduleIdle(_idleTask);
    }

    /* ------------------------------------------------------------ */
    public void cancelIdle()
    {
        _selectSet.cancelIdle(_idleTask);
    }

    /* ------------------------------------------------------------ */
    protected void idleExpired()
    {
        try
        {
            close();
        }
        catch (IOException e)
        {
            Log.ignore(e);
        }
    }

    /* ------------------------------------------------------------ */
    /*
     */
    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
    {
        int l = super.flush(header, buffer, trailer);
        _writable = l!=0;
        return l;
    }

    /* ------------------------------------------------------------ */
    /*
     */
    public int flush(Buffer buffer) throws IOException
    {
        int l = super.flush(buffer);
        _writable = l!=0;
        return l;
    }

    /* ------------------------------------------------------------ */
    public boolean isReadyForDispatch()
    {
        return !_dispatched;
    }
    
    /* ------------------------------------------------------------ */
    /*
     * Allows thread to block waiting for further events.
     */
    public boolean blockReadable(long timeoutMs) throws IOException
    {
        synchronized (this)
        {
            long start=_selectSet.getNow();
            try
            {   
                _readBlocked=true;
                while (isOpen() && _readBlocked)
                {
                    try
                    {
                        updateKey();
                        this.wait(timeoutMs);

                        if (_readBlocked && timeoutMs<(_selectSet.getNow()-start))
                            return false;
                    }
                    catch (InterruptedException e)
                    {
                        Log.warn(e);
                    }
                }
            }
            finally
            {
                _readBlocked=false;
            }
        }
        return true;
    }

    /* ------------------------------------------------------------ */
    /*
     * Allows thread to block waiting for further events.
     */
    public boolean blockWritable(long timeoutMs) throws IOException
    {
        synchronized (this)
        {
            long start=_selectSet.getNow();
            try
            {   
                _writeBlocked=true;
                while (isOpen() && _writeBlocked)
                {
                    try
                    {
                        updateKey();
                        this.wait(timeoutMs);

                        if (_writeBlocked && timeoutMs<(_selectSet.getNow()-start))
                            return false;
                    }
                    catch (InterruptedException e)
                    {
                        Log.warn(e);
                    }
                }
            }
            finally
            {
                _writeBlocked=false;
            }
        }
        return true;
    }

    /* ------------------------------------------------------------ */
    public void setWritable(boolean writable)
    {
        _writable=writable;
    }
    
    /* ------------------------------------------------------------ */
    public void scheduleWrite()
    {
        _writable=false;
        updateKey();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Updates selection key. Adds operations types to the selection key as needed. No operations
     * are removed as this is only done during dispatch. This method records the new key and
     * schedules a call to doUpdateKey to do the keyChange
     */
    private void updateKey()
    {
        synchronized (this)
        {
        
            int ops=-1;
            if (getChannel().isOpen())
            {
                _interestOps = 
                    ((!_dispatched || _readBlocked)  ? SelectionKey.OP_READ  : 0) 
                |   ((!_writable   || _writeBlocked) ? SelectionKey.OP_WRITE : 0);
                try
                {
                    ops = ((_key!=null && _key.isValid())?_key.interestOps():-1);
                }
                catch(Exception e)
                {
                    _key=null;
                    Log.ignore(e);
                }
            }
            if(_interestOps == ops && getChannel().isOpen())
                return;
            
        }
        _selectSet.addChange(this);
        _selectSet.wakeup();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Synchronize the interestOps with the actual key. Call is scheduled by a call to updateKey
     */
    void doUpdateKey()
    {
        synchronized (this)
        {
            if (getChannel().isOpen())
            {
                if (_interestOps>0)
                {
                    if (_key==null || !_key.isValid())
                    {
                        SelectableChannel sc = (SelectableChannel)getChannel();
                        if (sc.isRegistered())
                        {
                            updateKey();   
                        }
                        else
                        {
                            try
                            {
                                _key=((SelectableChannel)getChannel()).register(_selectSet.getSelector(),_interestOps,this);
                            }
                            catch (Exception e)
                            {
                                Log.ignore(e);
                                if (_key!=null && _key.isValid())
                                {
                                    _key.cancel();
                                }
                                cancelIdle();
                                if (_open)
                                    _manager.endPointClosed(this);
                                _open=false;
                                _key = null;
                            }
                        }
                    }
                    else
                    {
                        _key.interestOps(_interestOps);
                    }
                }
                else
                {
                    if (_key.isValid())
                        _key.interestOps(0);
                    else
                        _key=null;
                }
            }
            else    
            {
                if (_key!=null && _key.isValid())
                    _key.cancel(); 
                
                cancelIdle();
                if (_open)
                    _manager.endPointClosed(this);
                _open=false;
                _key = null;
            }
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     */
    public void run()
    {
        
        boolean dispatched=true;
        do
        {
            try
            {
                _connection.handle();
            }
            catch (ClosedChannelException e)
            {
                Log.ignore(e);
            }
            catch (EofException e)
            {
                Log.debug("EOF", e);
                try{close();}
                catch(IOException e2){Log.ignore(e2);}
            }
            catch (HttpException e)
            {
                Log.debug("BAD", e);
                try{close();}
                catch(IOException e2){Log.ignore(e2);}
            }
            catch (Throwable e)
            {
                Log.warn("handle failed", e);
                try{close();}
                catch(IOException e2){Log.ignore(e2);}
            }
            finally
            {
                dispatched=!undispatch();
            }   
        }
        while(dispatched);
    }

    /* ------------------------------------------------------------ */
    /*
     * @see org.eclipse.io.nio.ChannelEndPoint#close()
     */
    public void close() throws IOException
    {
        try
        {
            super.close();
        }
        catch (IOException e)
        {
            Log.ignore(e);
        }   
        finally
        {
            updateKey();
        }
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "SCEP@" + hashCode() + "[d=" + _dispatched + ",io=" + 
        ((SelectionKey.OP_ACCEPT&_interestOps)!=0?"A":"")+
        ((SelectionKey.OP_CONNECT&_interestOps)!=0?"C":"")+
        ((SelectionKey.OP_READ&_interestOps)!=0?"R":"")+
        ((SelectionKey.OP_WRITE&_interestOps)!=0?"W":"")+
        ",w=" + _writable + ",b=" + _readBlocked + "|" + _writeBlocked + "]";
    }

    /* ------------------------------------------------------------ */
    public Timeout.Task getTimeoutTask()
    {
        return _idleTask;
    }

    /* ------------------------------------------------------------ */
    public SelectSet getSelectSet()
    {
        return _selectSet;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public class IdleTask extends Timeout.Task 
    {
        /* ------------------------------------------------------------ */
        /*
         * @see org.eclipse.thread.Timeout.Task#expire()
         */
        public void expired()
        {
            idleExpired();
        }

        public String toString()
        {
            return "TimeoutTask:" + SelectChannelEndPoint.this.toString();
        }

    }

}
