//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.server.session;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ClassLoadingObjectInputStream;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;


/* ------------------------------------------------------------ */
/** 
 * HashSessionManager
 * 
 * An in-memory implementation of SessionManager.
 * <p>
 * This manager supports saving sessions to disk, either periodically or at shutdown.
 * Sessions can also have their content idle saved to disk to reduce the memory overheads of large idle sessions.
 * <p>
 * This manager will create it's own Timer instance to scavenge threads, unless it discovers a shared Timer instance
 * set as the "org.eclipse.jetty.server.session.timer" attribute of the ContextHandler.
 *
 */
public class HashSessionManager extends AbstractSessionManager
{
    final static Logger LOG = SessionHandler.LOG;

    protected final ConcurrentMap<String,HashedSession> _sessions=new ConcurrentHashMap<String,HashedSession>();
    private Scheduler _timer;
    private Scheduler.Task _task;
    long _scavengePeriodMs=30000;
    long _savePeriodMs=0; //don't do period saves by default
    long _idleSavePeriodMs = 0; // don't idle save sessions by default.
    private Scheduler.Task _saveTask;
    File _storeDir;
    private boolean _lazyLoad=false;
    private volatile boolean _sessionsLoaded=false;
    private boolean _deleteUnrestorableSessions=false;


    /**
     * Scavenger
     *
     */
    protected class Scavenger implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                scavenge();
            }
            finally
            {
                if (_timer != null && _timer.isRunning()) {
                    _task = _timer.schedule(this, _scavengePeriodMs, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * Saver
     *
     */
    protected class Saver implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                saveSessions(true);
            }
            catch (Exception e)
            {       
                LOG.warn(e);
            }
            finally
            {
                if (_timer != null && _timer.isRunning())
                    _saveTask = _timer.schedule(this, _savePeriodMs, TimeUnit.MILLISECONDS);
            }
        }        
    }


    /* ------------------------------------------------------------ */
    public HashSessionManager()
    {
        super();
    }

    /* ------------------------------------------------------------ */
    /**
     * @see AbstractSessionManager#doStart()
     */
    @Override
    public void doStart() throws Exception
    {
        //try shared scheduler from Server first
        _timer = getSessionHandler().getServer().getBean(Scheduler.class);
        if (_timer == null)
        {
            //try one passed into the context
            ServletContext context = ContextHandler.getCurrentContext();
            if (context!=null)
                _timer = (Scheduler)context.getAttribute("org.eclipse.jetty.server.session.timer");   
        }    
      
        if (_timer == null)
        {
            //make a scheduler if none useable
            _timer=new ScheduledExecutorScheduler(toString()+"Timer",true);
            addBean(_timer,true);
        }
        else
            addBean(_timer,false);
        
        super.doStart();

        setScavengePeriod(getScavengePeriod());

        if (_storeDir!=null)
        {
            if (!_storeDir.exists())
                _storeDir.mkdirs();

            if (!_lazyLoad)
                restoreSessions();
        }

        setSavePeriod(getSavePeriod());
    }

    /* ------------------------------------------------------------ */
    /**
     * @see AbstractSessionManager#doStop()
     */
    @Override
    public void doStop() throws Exception
    {
        // stop the scavengers
        synchronized(this)
        {
            if (_saveTask!=null)
                _saveTask.cancel();

            _saveTask=null;
            if (_task!=null)
                _task.cancel();

            _task=null;
            _timer=null;
        }
       

        // This will callback invalidate sessions - where we decide if we will save
        super.doStop();

        _sessions.clear();

    }

    /* ------------------------------------------------------------ */
    /**
     * @return the period in seconds at which a check is made for sessions to be invalidated.
     */
    public int getScavengePeriod()
    {
        return (int)(_scavengePeriodMs/1000);
    }


    /* ------------------------------------------------------------ */
    @Override
    public int getSessions()
    {
        int sessions=super.getSessions();
        if (LOG.isDebugEnabled())
        {
            if (_sessions.size()!=sessions)
                LOG.warn("sessions: "+_sessions.size()+"!="+sessions);
        }
        return sessions;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return seconds Idle period after which a session is saved
     */
    public int getIdleSavePeriod()
    {
      if (_idleSavePeriodMs <= 0)
        return 0;

      return (int)(_idleSavePeriodMs / 1000);
    }

    /* ------------------------------------------------------------ */
    /**
     * Configures the period in seconds after which a session is deemed idle and saved
     * to save on session memory.
     *
     * The session is persisted, the values attribute map is cleared and the session set to idled.
     *
     * @param seconds Idle period after which a session is saved
     */
    public void setIdleSavePeriod(int seconds)
    {
      _idleSavePeriodMs = seconds * 1000L;
    }

    /* ------------------------------------------------------------ */
    @Override
    public void setMaxInactiveInterval(int seconds)
    {
        super.setMaxInactiveInterval(seconds);
        if (_dftMaxIdleSecs>0&&_scavengePeriodMs>_dftMaxIdleSecs*1000L)
            setScavengePeriod((_dftMaxIdleSecs+9)/10);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param seconds the period is seconds at which sessions are periodically saved to disk
     */
    public void setSavePeriod (int seconds)
    {
        long period = (seconds * 1000L);
        if (period < 0)
            period=0;
        _savePeriodMs=period;

        if (_timer!=null)
        {
            synchronized (this)
            {
                if (_saveTask!=null)
                    _saveTask.cancel();
                _saveTask = null;
                if (_savePeriodMs > 0 && _storeDir!=null) //only save if we have a directory configured
                {
                    _saveTask = _timer.schedule(new Saver(),_savePeriodMs,TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the period in seconds at which sessions are periodically saved to disk
     */
    public int getSavePeriod ()
    {
        if (_savePeriodMs<=0)
            return 0;

        return (int)(_savePeriodMs/1000);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param seconds the period in seconds at which a check is made for sessions to be invalidated.
     */
    public void setScavengePeriod(int seconds)
    { 
        if (seconds==0)
            seconds=60;

        long old_period=_scavengePeriodMs;
        long period=seconds*1000L;
        if (period>60000)
            period=60000;
        if (period<1000)
            period=1000;

        _scavengePeriodMs=period;
    
        synchronized (this)
        {
            if (_timer!=null && (period!=old_period || _task==null))
            {
                if (_task!=null)
                {
                    _task.cancel();
                    _task = null;
                }

                _task = _timer.schedule(new Scavenger(),_scavengePeriodMs, TimeUnit.MILLISECONDS);
            }
        }
    }

    /* -------------------------------------------------------------- */
    /**
     * Find sessions that have timed out and invalidate them. This runs in the
     * SessionScavenger thread.
     */
    protected void scavenge()
    {
        //don't attempt to scavenge if we are shutting down
        if (isStopping() || isStopped())
            return;

        Thread thread=Thread.currentThread();
        ClassLoader old_loader=thread.getContextClassLoader();
        try
        {      
            if (_loader!=null)
                thread.setContextClassLoader(_loader);

            // For each session
            long now=System.currentTimeMillis();
            __log.debug("Scavenging sessions at {}", now); 
            
            for (Iterator<HashedSession> i=_sessions.values().iterator(); i.hasNext();)
            {
                HashedSession session=i.next();
                long idleTime=session.getMaxInactiveInterval()*1000L; 
                if (idleTime>0&&session.getAccessed()+idleTime<now)
                {
                    // Found a stale session, add it to the list
                    try
                    {
                        session.timeout();
                    }
                    catch (Exception e)
                    {
                        __log.warn("Problem scavenging sessions", e);
                    }
                }
                else if (_idleSavePeriodMs > 0 && session.getAccessed()+_idleSavePeriodMs < now)
                {
                    try
                    {
                        session.idle();
                    }
                    catch (Exception e)
                    {
                        __log.warn("Problem idling session "+ session.getId(), e);
                    }
                }
            }
        }       
        finally
        {
            thread.setContextClassLoader(old_loader);
        }
    }

    /* ------------------------------------------------------------ */
    @Override
    protected void addSession(AbstractSession session)
    {
        if (isRunning())
            _sessions.put(session.getClusterId(),(HashedSession)session);
    }

    /* ------------------------------------------------------------ */
    @Override
    public AbstractSession getSession(String idInCluster)
    {
        if ( _lazyLoad && !_sessionsLoaded)
        {
            try
            {
                restoreSessions();
            }
            catch(Exception e)
            {
                LOG.warn(e);
            }
        }

        Map<String,HashedSession> sessions=_sessions;
        if (sessions==null)
            return null;

        HashedSession session = sessions.get(idInCluster);

        if (session == null && _lazyLoad)
            session=restoreSession(idInCluster);
        if (session == null)
            return null;

        if (_idleSavePeriodMs!=0)
            session.deIdle();

        return session;
    }

    /* ------------------------------------------------------------ */
    @Override
    protected void shutdownSessions() throws Exception
    {   
        // Invalidate all sessions to cause unbind events
        ArrayList<HashedSession> sessions=new ArrayList<HashedSession>(_sessions.values());
        int loop=100;
        while (sessions.size()>0 && loop-->0)
        {
            // If we are called from doStop
            if (isStopping() && _storeDir != null && _storeDir.exists() && _storeDir.canWrite())
            {
                // Then we only save and remove the session from memory- it is not invalidated.
                for (HashedSession session : sessions)
                {
                    session.save(false);
                    _sessions.remove(session.getClusterId());
                }
            }
            else
            {
                for (HashedSession session : sessions)
                    session.invalidate();
            }

            // check that no new sessions were created while we were iterating
            sessions=new ArrayList<HashedSession>(_sessions.values());
        }
    }
    
    
    
    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.server.SessionManager#renewSessionId(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void renewSessionId(String oldClusterId, String oldNodeId, String newClusterId, String newNodeId)
    {
        try
        {
            Map<String,HashedSession> sessions=_sessions;
            if (sessions == null)
                return;

            HashedSession session = sessions.remove(oldClusterId);
            if (session == null)
                return;

            session.remove(); //delete any previously saved session
            session.setClusterId(newClusterId); //update ids
            session.setNodeId(newNodeId);
            session.save(); //save updated session: TODO consider only saving file if idled
            sessions.put(newClusterId, session);
            
            super.renewSessionId(oldClusterId, oldNodeId, newClusterId, newNodeId);
        }
        catch (Exception e)
        {
            LOG.warn(e);
        }
    }

    /* ------------------------------------------------------------ */
    @Override
    protected AbstractSession newSession(HttpServletRequest request)
    {
        return new HashedSession(this, request);
    }

    /* ------------------------------------------------------------ */
    protected AbstractSession newSession(long created, long accessed, String clusterId)
    {
        return new HashedSession(this, created,accessed, clusterId);
    }

    /* ------------------------------------------------------------ */
    @Override
    protected boolean removeSession(String clusterId)
    {
        return _sessions.remove(clusterId)!=null;
    }

    /* ------------------------------------------------------------ */
    public void setStoreDirectory (File dir) throws IOException
    { 
        // CanonicalFile is used to capture the base store directory in a way that will
        // work on Windows.  Case differences may through off later checks using this directory.
        _storeDir=dir.getCanonicalFile();
    }

    /* ------------------------------------------------------------ */
    public File getStoreDirectory ()
    {
        return _storeDir;
    }

    /* ------------------------------------------------------------ */
    public void setLazyLoad(boolean lazyLoad)
    {
        _lazyLoad = lazyLoad;
    }

    /* ------------------------------------------------------------ */
    public boolean isLazyLoad()
    {
        return _lazyLoad;
    }

    /* ------------------------------------------------------------ */
    public boolean isDeleteUnrestorableSessions()
    {
        return _deleteUnrestorableSessions;
    }

    /* ------------------------------------------------------------ */
    public void setDeleteUnrestorableSessions(boolean deleteUnrestorableSessions)
    {
        _deleteUnrestorableSessions = deleteUnrestorableSessions;
    }

    /* ------------------------------------------------------------ */
    public void restoreSessions () throws Exception
    {
        _sessionsLoaded = true;

        if (_storeDir==null || !_storeDir.exists())
        {
            return;
        }

        if (!_storeDir.canRead())
        {
            LOG.warn ("Unable to restore Sessions: Cannot read from Session storage directory "+_storeDir.getAbsolutePath());
            return;
        }

        String[] files = _storeDir.list();
        for (int i=0;files!=null&&i<files.length;i++)
        {
            restoreSession(files[i]);
        }
    }

    /* ------------------------------------------------------------ */
    protected synchronized HashedSession restoreSession(String idInCuster)
    {        
        File file = new File(_storeDir,idInCuster);

        Exception error = null;
        if (!file.exists())
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Not loading: {}",file);
            }
            return null;
        }
        
        try (FileInputStream in = new FileInputStream(file))
        {
            HashedSession session = restoreSession(in,null);
            addSession(session,false);
            session.didActivate();
            return session;
        }
        catch (Exception e)
        {
           error = e;
        }
        finally
        {
            if (error != null)
            {
                if (isDeleteUnrestorableSessions() && file.exists() && file.getParentFile().equals(_storeDir) )
                {
                    file.delete();
                    LOG.warn("Deleting file for unrestorable session {} {}",idInCuster,error);
                    __log.debug(error);
                }
                else
                {
                    __log.warn("Problem restoring session {} {}",idInCuster, error);
                    __log.debug(error);
                }
            }
            else
            {
                // delete successfully restored file
                file.delete();
            }
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    public void saveSessions(boolean reactivate) throws Exception
    {
        if (_storeDir==null || !_storeDir.exists())
        {
            return;
        }

        if (!_storeDir.canWrite())
        {
            LOG.warn ("Unable to save Sessions: Session persistence storage directory "+_storeDir.getAbsolutePath()+ " is not writeable");
            return;
        }

        for (HashedSession session : _sessions.values())
            session.save(reactivate);
    }
    

    /* ------------------------------------------------------------ */
    public HashedSession restoreSession (InputStream is, HashedSession session) throws Exception
    {
        DataInputStream di = new DataInputStream(is);

        String clusterId = di.readUTF();
        di.readUTF(); // nodeId

        long created = di.readLong();
        long accessed = di.readLong();
        int requests = di.readInt();

        if (session == null)
            session = (HashedSession)newSession(created, accessed, clusterId);
        
        session.setRequests(requests);

        // Attributes
        int size = di.readInt();

        restoreSessionAttributes(di, size, session);

        try
        {
            int maxIdle = di.readInt();
            session.setMaxInactiveInterval(maxIdle);
        }
        catch (IOException e)
        {
            LOG.debug("No maxInactiveInterval persisted for session "+clusterId);
            LOG.ignore(e);
        }

        return session;
    }


    @SuppressWarnings("resource")
    private void restoreSessionAttributes (InputStream is, int size, HashedSession session)
    throws Exception
    {
        if (size>0)
        {
            // input stream should not be closed here
            ClassLoadingObjectInputStream ois =  new ClassLoadingObjectInputStream(is);
            for (int i=0; i<size;i++)
            {
                String key = ois.readUTF();
                Object value = ois.readObject();
                session.setAttribute(key,value);
            }
        }
    }
}
