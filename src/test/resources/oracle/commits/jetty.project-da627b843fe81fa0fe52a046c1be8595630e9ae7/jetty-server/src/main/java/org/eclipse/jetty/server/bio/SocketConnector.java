// ========================================================================
// Copyright (c) 2003-2009 Mort Bay Consulting Pty. Ltd.
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
 
package org.eclipse.jetty.server.bio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.HttpException;
import org.eclipse.jetty.io.bio.SocketEndPoint;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.log.Log;


/* ------------------------------------------------------------------------------- */
/**  Socket Connector.
 * This connector implements a traditional blocking IO and threading model.
 * Normal JRE sockets are used and a thread is allocated per connection.
 * Buffers are managed so that large buffers are only allocated to active connections.
 * 
 * This Connector should only be used if NIO is not available.
 * 
 * @org.apache.xbean.XBean element="bioConnector" description="Creates a BIO based socket connector"
 * 
 * 
 */
public class SocketConnector extends AbstractConnector
{
    protected ServerSocket _serverSocket;
    protected Set _connections;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * 
     */
    public SocketConnector()
    {
    }

    /* ------------------------------------------------------------ */
    public Object getConnection()
    {
        return _serverSocket;
    }
    
    /* ------------------------------------------------------------ */
    public void open() throws IOException
    {
        // Create a new server socket and set to non blocking mode
        if (_serverSocket==null || _serverSocket.isClosed())
        _serverSocket= newServerSocket(getHost(),getPort(),getAcceptQueueSize());
        _serverSocket.setReuseAddress(getReuseAddress());
    }

    /* ------------------------------------------------------------ */
    protected ServerSocket newServerSocket(String host, int port,int backlog) throws IOException
    {
        ServerSocket ss= host==null?
            new ServerSocket(port,backlog):
            new ServerSocket(port,backlog,InetAddress.getByName(host));
       
        return ss;
    }
    
    /* ------------------------------------------------------------ */
    public void close() throws IOException
    {
        if (_serverSocket!=null)
            _serverSocket.close();
        _serverSocket=null;
    }

    /* ------------------------------------------------------------ */
    public void accept(int acceptorID)
    	throws IOException, InterruptedException
    {   
        Socket socket = _serverSocket.accept();
        configure(socket);
        
        Connection connection=new Connection(socket);
        connection.dispatch();
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Allows subclass to override Conection if required.
     */
    protected HttpConnection newHttpConnection(EndPoint endpoint) 
    {
        return new HttpConnection(this, endpoint, getServer());
    }

    /* ------------------------------------------------------------------------------- */
    public Buffer newBuffer(int size)
    {
        return new ByteArrayBuffer(size);
    }

    /* ------------------------------------------------------------------------------- */
    public void customize(EndPoint endpoint, Request request)
        throws IOException
    {
        Connection connection = (Connection)endpoint;
        if (connection._sotimeout!=_maxIdleTime)
        {
            connection._sotimeout=_maxIdleTime;
            ((Socket)endpoint.getTransport()).setSoTimeout(_maxIdleTime);
        }
              
        super.customize(endpoint, request);
    }

    /* ------------------------------------------------------------------------------- */
    public int getLocalPort()
    {
        if (_serverSocket==null || _serverSocket.isClosed())
            return -1;
        return _serverSocket.getLocalPort();
    }

    /* ------------------------------------------------------------------------------- */
    protected void doStart() throws Exception
    {
        _connections=new HashSet();
        super.doStart();
    }

    /* ------------------------------------------------------------------------------- */
    protected void doStop() throws Exception
    {
        super.doStop();
        Set set=null;

        synchronized(_connections)
        {
            set= new HashSet(_connections);
        }
        
        Iterator iter=set.iterator();
        while(iter.hasNext())
        {
            Connection connection = (Connection)iter.next();
            connection.close();
        }
    }

    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    protected class Connection extends SocketEndPoint implements Runnable
    {
        boolean _dispatched=false;
        HttpConnection _connection;
        int _sotimeout;
        protected Socket _socket;
        
        public Connection(Socket socket) throws IOException
        {
            super(socket);
            _connection = newHttpConnection(this);
            _sotimeout=socket.getSoTimeout();
            _socket=socket;
        }
        
        public void dispatch() throws InterruptedException, IOException
        {
            if (getThreadPool()==null || !getThreadPool().dispatch(this))
            {
                Log.warn("dispatch failed for {}",_connection);
                close();
            }
        }
        
        public int fill(Buffer buffer) throws IOException
        {
            int l = super.fill(buffer);
            if (l<0)
                close();
            return l;
        } 
        
        public void close() throws IOException
        {
            _connection.getRequest().getAsyncRequest().cancel();
            super.close();
        }

        public void run()
        {
            try
            {
                connectionOpened(_connection);
                synchronized(_connections)
                {
                    _connections.add(this);
                }
                
                while (isStarted() && !isClosed())
                {
                    if (_connection.isIdle())
                    {
                        if (getServer().getThreadPool().isLowOnThreads())
                        {
                            int lrmit = getLowResourceMaxIdleTime();
                            if (lrmit>=0 && _sotimeout!= lrmit)
                            {
                                _sotimeout=lrmit;
                                _socket.setSoTimeout(_sotimeout);
                            }
                        }
                    }                    
                    _connection.handle();
                }
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
            catch(Exception e)
            {
                Log.warn("handle failed?",e);
                try{close();}
                catch(IOException e2){Log.ignore(e2);}
            }
            finally
            { 
                connectionClosed(_connection);
                synchronized(_connections)
                {
                    _connections.remove(this);
                }
            }
        }
    }
}
