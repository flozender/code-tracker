// ========================================================================
// Copyright (c) 2000-2009 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.server.ssl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.http.security.Password;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.bio.SocketEndPoint;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;

/* ------------------------------------------------------------ */
/**
 * JSSE Socket Listener.
 * 
 * This specialization of HttpListener is an abstract listener that can be used as the basis for a
 * specific JSSE listener.
 * 
 * This is heavily based on the work from Court Demas, which in turn is based on the work from Forge
 * Research.
 * 
 * @org.apache.xbean.XBean element="sslSocketConnector" description="Creates an ssl socket connector"
 *
 * 
 * 
 * 
 * 
 */
public class SslSocketConnector extends SocketConnector
{
    /**
     * The name of the SSLSession attribute that will contain any cached information.
     */
    static final String CACHED_INFO_ATTR = CachedInfo.class.getName();

    /** Default value for the keystore location path. */
    public static final String DEFAULT_KEYSTORE = System.getProperty("user.home") + File.separator
            + ".keystore";

    /** String name of key password property. */
    public static final String KEYPASSWORD_PROPERTY = "jetty.ssl.keypassword";

    /** String name of keystore password property. */
    public static final String PASSWORD_PROPERTY = "jetty.ssl.password";

    /**
     * Return the chain of X509 certificates used to negotiate the SSL Session.
     * <p>
     * Note: in order to do this we must convert a javax.security.cert.X509Certificate[], as used by
     * JSSE to a java.security.cert.X509Certificate[],as required by the Servlet specs.
     * 
     * @param sslSession the javax.net.ssl.SSLSession to use as the source of the cert chain.
     * @return the chain of java.security.cert.X509Certificates used to negotiate the SSL
     *         connection. <br>
     *         Will be null if the chain is missing or empty.
     */
    private static X509Certificate[] getCertChain(SSLSession sslSession)
    {
        try
        {
            javax.security.cert.X509Certificate javaxCerts[] = sslSession.getPeerCertificateChain();
            if (javaxCerts == null || javaxCerts.length == 0)
                return null;

            int length = javaxCerts.length;
            X509Certificate[] javaCerts = new X509Certificate[length];

            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            for (int i = 0; i < length; i++)
            {
                byte bytes[] = javaxCerts[i].getEncoded();
                ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                javaCerts[i] = (X509Certificate) cf.generateCertificate(stream);
            }

            return javaCerts;
        }
        catch (SSLPeerUnverifiedException pue)
        {
            return null;
        }
        catch (Exception e)
        {
            Log.warn(Log.EXCEPTION, e);
            return null;
        }
    }

  

    /** Default value for the cipher Suites. */
    private String _excludeCipherSuites[] = null;
    
    /** Default value for the keystore location path. */
    private String _keystore=DEFAULT_KEYSTORE ;
    private String _keystoreType = "JKS"; // type of the key store
    
    /** Set to true if we require client certificate authentication. */
    private boolean _needClientAuth = false;
    private transient Password _password;
    private transient Password _keyPassword;
    private transient Password _trustPassword;
    private String _protocol= "TLS";
    private String _provider;
    private String _secureRandomAlgorithm; // cert algorithm
    private String _sslKeyManagerFactoryAlgorithm = (Security.getProperty("ssl.KeyManagerFactory.algorithm")==null?"SunX509":Security.getProperty("ssl.KeyManagerFactory.algorithm")); // cert algorithm
    private String _sslTrustManagerFactoryAlgorithm = (Security.getProperty("ssl.TrustManagerFactory.algorithm")==null?"SunX509":Security.getProperty("ssl.TrustManagerFactory.algorithm")); // cert algorithm
    
    private String _truststore;
    private String _truststoreType = "JKS"; // type of the key store

    /** Set to true if we would like client certificate authentication. */
    private boolean _wantClientAuth = false;
    private int _handshakeTimeout = 0; //0 means use maxIdleTime
    
    private SSLContext _context;


    /* ------------------------------------------------------------ */
    /**
     * Constructor.
     */
    public SslSocketConnector()
    {
        super();
    }


    /* ------------------------------------------------------------ */
    public void accept(int acceptorID)
        throws IOException, InterruptedException
    {   
        Socket socket = _serverSocket.accept();
        configure(socket);
        
        Connection connection=new SslConnection(socket);
        connection.dispatch();
    }
    
    /* ------------------------------------------------------------ */
    protected void configure(Socket socket)
        throws IOException
    {   
        super.configure(socket);
    }

    /* ------------------------------------------------------------ */
    protected SSLServerSocketFactory createFactory() 
        throws Exception
    {
    	SSLContext context = _context;
    	if (context == null) {
	        if (_truststore==null)
	        {
	            _truststore=_keystore;
	            _truststoreType=_keystoreType;
	        }
	
	        KeyManager[] keyManagers = null;
	        InputStream keystoreInputStream = null;
	        if (_keystore != null)
	        	keystoreInputStream = Resource.newResource(_keystore).getInputStream();
	        KeyStore keyStore = KeyStore.getInstance(_keystoreType);
	        keyStore.load(keystoreInputStream, _password==null?null:_password.toString().toCharArray());
	
	        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(_sslKeyManagerFactoryAlgorithm);        
	        keyManagerFactory.init(keyStore,_keyPassword==null?null:_keyPassword.toString().toCharArray());
	        keyManagers = keyManagerFactory.getKeyManagers();
	
	        TrustManager[] trustManagers = null;
	        InputStream truststoreInputStream = null;
	        if (_truststore != null)
	        	truststoreInputStream = Resource.newResource(_truststore).getInputStream();
	        KeyStore trustStore = KeyStore.getInstance(_truststoreType);
	        trustStore.load(truststoreInputStream,_trustPassword==null?null:_trustPassword.toString().toCharArray());
	        
	        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(_sslTrustManagerFactoryAlgorithm);
	        trustManagerFactory.init(trustStore);
	        trustManagers = trustManagerFactory.getTrustManagers();
	        
	
	        SecureRandom secureRandom = _secureRandomAlgorithm==null?null:SecureRandom.getInstance(_secureRandomAlgorithm);
	
	        context = _provider==null?SSLContext.getInstance(_protocol):SSLContext.getInstance(_protocol, _provider);
	
	        context.init(keyManagers, trustManagers, secureRandom);
    	}
    	
        return context.getServerSocketFactory();
    }

    /* ------------------------------------------------------------ */
    /**
     * Allow the Listener a chance to customise the request. before the server does its stuff. <br>
     * This allows the required attributes to be set for SSL requests. <br>
     * The requirements of the Servlet specs are:
     * <ul>
     * <li> an attribute named "javax.servlet.request.ssl_id" of type String (since Spec 3.0).</li>
     * <li> an attribute named "javax.servlet.request.cipher_suite" of type String.</li>
     * <li> an attribute named "javax.servlet.request.key_size" of type Integer.</li>
     * <li> an attribute named "javax.servlet.request.X509Certificate" of type
     * java.security.cert.X509Certificate[]. This is an array of objects of type X509Certificate,
     * the order of this array is defined as being in ascending order of trust. The first
     * certificate in the chain is the one set by the client, the next is the one used to
     * authenticate the first, and so on. </li>
     * </ul>
     * 
     * @param endpoint The Socket the request arrived on. 
     *        This should be a {@link SocketEndPoint} wrapping a {@link SSLSocket}.
     * @param request HttpRequest to be customised.
     */
    public void customize(EndPoint endpoint, Request request)
        throws IOException
    {
        super.customize(endpoint, request);
        request.setScheme(HttpSchemes.HTTPS);
        
        SocketEndPoint socket_end_point = (SocketEndPoint)endpoint;
        SSLSocket sslSocket = (SSLSocket)socket_end_point.getTransport();
        
        try
        {
            SSLSession sslSession = sslSocket.getSession();
            String cipherSuite = sslSession.getCipherSuite();
            Integer keySize;
            String idStr;
            X509Certificate[] certs;

            CachedInfo cachedInfo = (CachedInfo) sslSession.getValue(CACHED_INFO_ATTR);
            if (cachedInfo != null)
            {
                keySize = cachedInfo.getKeySize();
                certs = cachedInfo.getCerts();
                idStr = cachedInfo.getIdStr();
            }
            else
            {
                keySize = new Integer(ServletSSL.deduceKeyLength(cipherSuite));
                certs = getCertChain(sslSession);
                byte[] idBytes = sslSession.getId();
                idStr = TypeUtil.toHexString(idBytes);
                cachedInfo = new CachedInfo(keySize, certs,idStr);
                sslSession.putValue(CACHED_INFO_ATTR, cachedInfo);
            }

            if (certs != null)
                request.setAttribute("javax.servlet.request.X509Certificate", certs);
            else if (_needClientAuth) // Sanity check
                throw new IllegalStateException("no client auth");
            
            request.setAttribute("javax.servlet.request.ssl_session_id", idStr);
            request.setAttribute("javax.servlet.request.cipher_suite", cipherSuite);
            request.setAttribute("javax.servlet.request.key_size", keySize);
        }
        catch (Exception e)
        {
            Log.warn(Log.EXCEPTION, e);
        }
    }

    /* ------------------------------------------------------------ */    
    public String[] getExcludeCipherSuites() {
        return _excludeCipherSuites;
    }

    /* ------------------------------------------------------------ */
    public String getKeystore()
    {
        return _keystore;
    }

    /* ------------------------------------------------------------ */
    public String getKeystoreType() 
    {
        return (_keystoreType);
    }

    /* ------------------------------------------------------------ */
    public boolean getNeedClientAuth()
    {
        return _needClientAuth;
    }

    /* ------------------------------------------------------------ */
    public String getProtocol() 
    {
        return _protocol;
    }

    /* ------------------------------------------------------------ */
    public String getProvider() {
	return _provider;
    }

    /* ------------------------------------------------------------ */
    public String getSecureRandomAlgorithm() 
    {
        return (this._secureRandomAlgorithm);
    }

    /* ------------------------------------------------------------ */
    public String getSslKeyManagerFactoryAlgorithm() 
    {
        return (this._sslKeyManagerFactoryAlgorithm);
    }

    /* ------------------------------------------------------------ */
    public String getSslTrustManagerFactoryAlgorithm() 
    {
        return (this._sslTrustManagerFactoryAlgorithm);
    }

    /* ------------------------------------------------------------ */
    public String getTruststore()
    {
        return _truststore;
    }

    /* ------------------------------------------------------------ */
    public String getTruststoreType()
    {
        return _truststoreType;
    }

    /* ------------------------------------------------------------ */
    public boolean getWantClientAuth()
    {
        return _wantClientAuth;
    }

    /* ------------------------------------------------------------ */
    /**
     * By default, we're confidential, given we speak SSL. But, if we've been told about an
     * confidential port, and said port is not our port, then we're not. This allows separation of
     * listeners providing INTEGRAL versus CONFIDENTIAL constraints, such as one SSL listener
     * configured to require client certs providing CONFIDENTIAL, whereas another SSL listener not
     * requiring client certs providing mere INTEGRAL constraints.
     */
    public boolean isConfidential(Request request)
    {
        final int confidentialPort = getConfidentialPort();
        return confidentialPort == 0 || confidentialPort == request.getServerPort();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * By default, we're integral, given we speak SSL. But, if we've been told about an integral
     * port, and said port is not our port, then we're not. This allows separation of listeners
     * providing INTEGRAL versus CONFIDENTIAL constraints, such as one SSL listener configured to
     * require client certs providing CONFIDENTIAL, whereas another SSL listener not requiring
     * client certs providing mere INTEGRAL constraints.
     */
    public boolean isIntegral(Request request)
    {
        final int integralPort = getIntegralPort();
        return integralPort == 0 || integralPort == request.getServerPort();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param addr The {@link SocketAddress address} that this server should listen on 
     * @param backlog See {@link ServerSocket#bind(java.net.SocketAddress, int)}
     * @return A new {@link ServerSocket socket object} bound to the supplied address with all other
     * settings as per the current configuration of this connector. 
     * @see #setWantClientAuth
     * @see #setNeedClientAuth
     * @see #setCipherSuites
     * @exception IOException
     */

    /* ------------------------------------------------------------ */
    protected ServerSocket newServerSocket(String host, int port,int backlog) throws IOException
    {
        SSLServerSocketFactory factory = null;
        SSLServerSocket socket = null;

        try
        {
            factory = createFactory();

            socket = (SSLServerSocket) (host==null?
                            factory.createServerSocket(port,backlog):
                            factory.createServerSocket(port,backlog,InetAddress.getByName(host)));

            if (_wantClientAuth)
                socket.setWantClientAuth(_wantClientAuth);
            if (_needClientAuth)
                socket.setNeedClientAuth(_needClientAuth);

            if (_excludeCipherSuites != null && _excludeCipherSuites.length >0) 
            {
                List excludedCSList = Arrays.asList(_excludeCipherSuites);
                String[] enabledCipherSuites = socket.getEnabledCipherSuites();
            	List enabledCSList = new ArrayList(Arrays.asList(enabledCipherSuites));
            	Iterator exIter = excludedCSList.iterator();

                while (exIter.hasNext())
            	{
            	    String cipherName = (String)exIter.next();
                    if (enabledCSList.contains(cipherName))
                    {
                        enabledCSList.remove(cipherName);
                    }
            	}
                enabledCipherSuites = (String[])enabledCSList.toArray(new String[enabledCSList.size()]);

                socket.setEnabledCipherSuites(enabledCipherSuites);
            }
            
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Log.warn(Log.EXCEPTION, e);
            throw new IOException("Could not create JsseListener: " + e.toString());
        }
        return socket;
    }

    /* ------------------------------------------------------------ */
    /** 
     * 
     */
    public void setExcludeCipherSuites(String[] cipherSuites) {
        this._excludeCipherSuites = cipherSuites;
    }

    /* ------------------------------------------------------------ */
    public void setKeyPassword(String password)
    {
        _keyPassword = Password.getPassword(KEYPASSWORD_PROPERTY,password,null);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param keystore The resource path to the keystore, or null for built in keystores.
     */
    public void setKeystore(String keystore)
    {
        _keystore = keystore;
    }

    /* ------------------------------------------------------------ */
    public void setKeystoreType(String keystoreType) 
    {
        _keystoreType = keystoreType;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the value of the needClientAuth property
     * 
     * @param needClientAuth true iff we require client certificate authentication.
     */
    public void setNeedClientAuth(boolean needClientAuth)
    {
        _needClientAuth = needClientAuth;
    }
    
    /* ------------------------------------------------------------ */
    public void setPassword(String password)
    {
        _password = Password.getPassword(PASSWORD_PROPERTY,password,null);
    }
    
    /* ------------------------------------------------------------ */
    public void setTrustPassword(String password)
    {
        _trustPassword = Password.getPassword(PASSWORD_PROPERTY,password,null);
    }

    /* ------------------------------------------------------------ */
    public void setProtocol(String protocol) 
    {
        _protocol = protocol;
    }

    /* ------------------------------------------------------------ */
    public void setProvider(String _provider) {
	this._provider = _provider;
    }

    /* ------------------------------------------------------------ */
    public void setSecureRandomAlgorithm(String algorithm) 
    {
        this._secureRandomAlgorithm = algorithm;
    }

    /* ------------------------------------------------------------ */
    public void setSslKeyManagerFactoryAlgorithm(String algorithm) 
    {
        this._sslKeyManagerFactoryAlgorithm = algorithm;
    }
    
    /* ------------------------------------------------------------ */
    public void setSslTrustManagerFactoryAlgorithm(String algorithm) 
    {
        this._sslTrustManagerFactoryAlgorithm = algorithm;
    }


    public void setTruststore(String truststore)
    {
        _truststore = truststore;
    }
    

    public void setTruststoreType(String truststoreType)
    {
        _truststoreType = truststoreType;
    }
    
    public void setSslContext(SSLContext sslContext)
    {
    	_context = sslContext;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the value of the _wantClientAuth property. This property is used when
     * {@link #newServerSocket(SocketAddress, int) opening server sockets}.
     * 
     * @param wantClientAuth true iff we want client certificate authentication.
     * @see SSLServerSocket#setWantClientAuth
     */
    public void setWantClientAuth(boolean wantClientAuth)
    {
        _wantClientAuth = wantClientAuth;
    }

    /**
     * Set the time in milliseconds for so_timeout during ssl handshaking
     * @param msec a non-zero value will be used to set so_timeout during
     * ssl handshakes. A zero value means the maxIdleTime is used instead.
     */
    public void setHandshakeTimeout (int msec)
    {
        _handshakeTimeout = msec;
    }
    
    
    public int getHandshakeTimeout ()
    {
        return _handshakeTimeout;
    }
    /**
     * Simple bundle of information that is cached in the SSLSession. Stores the effective keySize
     * and the client certificate chain.
     */
    private class CachedInfo
    {
        private X509Certificate[] _certs;
        private Integer _keySize;
        private String _idStr;
 

        CachedInfo(Integer keySize, X509Certificate[] certs,String id)
        {
            this._keySize = keySize;
            this._certs = certs;
            this._idStr = id;
        }

        X509Certificate[] getCerts()
        {
            return _certs;
        }

        Integer getKeySize()
        {
            return _keySize;
        }
        
        String getIdStr ()
        {
            return _idStr;
        }
    }
    
    
    public class SslConnection extends Connection
    {
        public SslConnection(Socket socket) throws IOException
        {
            super(socket);
        }
        
        public void run()
        {
            try
            {
                int handshakeTimeout = getHandshakeTimeout();
                int oldTimeout = _socket.getSoTimeout();
                if (handshakeTimeout > 0)            
                    _socket.setSoTimeout(handshakeTimeout);

                ((SSLSocket)_socket).startHandshake();

                if (handshakeTimeout>0)
                    _socket.setSoTimeout(oldTimeout);

                super.run();
            }
            catch (SSLException e)
            {
                Log.debug(e); 
                try{close();}
                catch(IOException e2){Log.ignore(e2);}
            }
            catch (IOException e)
            {
                Log.debug(e);
                try{close();}
                catch(IOException e2){Log.ignore(e2);}
            } 
        }
    }

}
