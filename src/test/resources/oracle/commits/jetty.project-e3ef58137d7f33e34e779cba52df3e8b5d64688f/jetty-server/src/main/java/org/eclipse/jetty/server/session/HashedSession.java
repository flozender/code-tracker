//
//  ========================================================================
//  Copyright (c) 1995-2017 Mort Bay Consulting Pty. Ltd.
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HashedSession extends MemSession
{
    private static final Logger LOG = Log.getLogger(HashedSession.class);

    private final HashSessionManager _hashSessionManager;

    /** Whether the session has been saved because it has been deemed idle;
     * in which case its attribute map will have been saved and cleared. */
    private transient boolean _idled = false;

    /** Whether there has already been an attempt to save this session
     * which has failed.  If there has, there will be no more save attempts
     * for this session.  This is to stop the logs being flooded with errors
     * due to serialization failures that are most likely caused by user
     * data stored in the session that is not serializable. */
    private transient boolean _saveFailed = false;
    
    /**
    * Last time session was saved to prevent periodic saves to sessions
    * that have not changed
    */
    private transient long _lastSaved = 0;
    
    /**
     * True if an attempt has been made to de-idle a session and it failed. Once
     * true, the session will not be attempted to be de-idled again.
     */
    private transient boolean _deIdleFailed = false;

    /* ------------------------------------------------------------- */
    protected HashedSession(HashSessionManager hashSessionManager, HttpServletRequest request)
    {
        super(hashSessionManager,request);
        _hashSessionManager = hashSessionManager;
    }

    /* ------------------------------------------------------------- */
    protected HashedSession(HashSessionManager hashSessionManager, long created, long accessed, String clusterId)
    {
        super(hashSessionManager,created, accessed, clusterId);
        _hashSessionManager = hashSessionManager;
    }

    /* ------------------------------------------------------------- */
    protected void checkValid()
    {
        if (!_deIdleFailed && _hashSessionManager._idleSavePeriodMs!=0)
            deIdle();
        super.checkValid();
    }

    /* ------------------------------------------------------------- */
    @Override
    public void setMaxInactiveInterval(int secs)
    {
        super.setMaxInactiveInterval(secs);
        if (getMaxInactiveInterval()>0&&(getMaxInactiveInterval()*1000L/10)<_hashSessionManager._scavengePeriodMs)
            _hashSessionManager.setScavengePeriod((secs+9)/10);
    }

    /* ------------------------------------------------------------ */
    @Override
    protected void doInvalidate()
    throws IllegalStateException
    {
        super.doInvalidate();
        remove();
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * Remove from the disk
     */
    synchronized void remove ()
    {
        if (_hashSessionManager._storeDir!=null && getId()!=null)
        {
            String id=getId();
            File f = new File(_hashSessionManager._storeDir, id);
            f.delete();
        }
    }

    /* ------------------------------------------------------------ */
    synchronized void save(boolean reactivate)
    throws Exception
    {
        // Only idle the session if not already idled and no previous save/idle has failed
        if (!isIdled() && !_saveFailed)
        {
            if (LOG.isDebugEnabled())
                LOG.debug("Saving {} {}",super.getId(),reactivate);

            try
            {
                willPassivate();
                save();
                if (reactivate)
                    didActivate();
                else
                    clearAttributes();
            }
            catch (Exception e)
            {       
                LOG.warn("Problem saving session " + super.getId(), e);
                _idled=false; // assume problem was before _values.clear();
            }
        }
    }
    
    
    
    synchronized void save ()
    throws Exception
    {   
        File file = null;
        if (!_saveFailed && _hashSessionManager._storeDir != null && _lastSaved < getAccessed())
        {
            file = new File(_hashSessionManager._storeDir, super.getId());
            if (file.exists())
            {
                file.delete();
            }

            try(FileOutputStream fos = new FileOutputStream(file,false))
            {
                _lastSaved = System.currentTimeMillis();
                save(fos);
            }
            catch (Exception e)
            {
                saveFailed(); // We won't try again for this session
                if (file != null) 
                    file.delete(); // No point keeping the file if we didn't save the whole session
                throw e;             
            }
        }
    }
    
    
    /* ------------------------------------------------------------ */
    public synchronized void save(OutputStream os)  throws IOException
    {
        DataOutputStream out = new DataOutputStream(os);
        out.writeUTF(getClusterId());
        out.writeUTF(getNodeId());
        out.writeLong(getCreationTime());
        out.writeLong(getAccessed());

        /* Don't write these out, as they don't make sense to store because they
         * either they cannot be true or their value will be restored in the
         * Session constructor.
         */
        //out.writeBoolean(_invalid);
        //out.writeBoolean(_doInvalidate);
        //out.writeBoolean( _newSession);
        out.writeInt(getRequests());
        out.writeInt(getAttributes());
        ObjectOutputStream oos = new ObjectOutputStream(out);
        Enumeration<String> e=getAttributeNames();
        while(e.hasMoreElements())
        {
            String key=e.nextElement();
            oos.writeUTF(key);
            oos.writeObject(doGet(key));
        }
        
        out.writeInt(getMaxInactiveInterval());
    }

    /* ------------------------------------------------------------ */
    public synchronized void deIdle()
    {
        if (isIdled() && !_deIdleFailed)
        {
            // Access now to prevent race with idling period
            access(System.currentTimeMillis());

            if (LOG.isDebugEnabled())
                LOG.debug("De-idling " + super.getId());

            FileInputStream fis = null;

            try
            {
                File file = new File(_hashSessionManager._storeDir, super.getId());
                if (!file.exists() || !file.canRead())
                    throw new FileNotFoundException(file.getName());

                fis = new FileInputStream(file);
                _idled = false;
                _hashSessionManager.restoreSession(fis, this);
                IO.close(fis); 
                
                didActivate();

                // If we are doing period saves, then there is no point deleting at this point 
                if (_hashSessionManager._savePeriodMs == 0)
                    file.delete();
            }
            catch (Exception e)
            {
                deIdleFailed();
                LOG.warn("Problem de-idling session " + super.getId(), e);
                if (fis != null) IO.close(fis);//Must ensure closed before invalidate
                invalidate();
            }
        }
    }


    /* ------------------------------------------------------------ */
    /**
     * Idle the session to reduce session memory footprint.
     *
     * The session is idled by persisting it, then clearing the session values attribute map and finally setting
     * it to an idled state.
     * @throws Exception if unable to save session
     */
    public synchronized void idle()
    throws Exception
    {
        save(false);
        _idled = true;
    }

    /* ------------------------------------------------------------ */
    public synchronized boolean isIdled()
    {
      return _idled;
    }

    /* ------------------------------------------------------------ */
    public synchronized boolean isSaveFailed()
    {
        return _saveFailed;
    }

    /* ------------------------------------------------------------ */
    public synchronized void saveFailed()
    {
        _saveFailed = true;
    }

    /* ------------------------------------------------------------ */
    public synchronized void deIdleFailed()
    {
        _deIdleFailed = true;
    }
    
    /* ------------------------------------------------------------ */
    public synchronized boolean isDeIdleFailed()
    {
        return _deIdleFailed;
    }
}
