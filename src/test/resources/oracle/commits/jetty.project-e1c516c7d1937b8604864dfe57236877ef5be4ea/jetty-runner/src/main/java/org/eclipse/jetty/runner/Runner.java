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

package org.eclipse.jetty.runner;

import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ConnectorStatistics;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.ShutdownMonitor;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.StatisticsServlet;
import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;



/**
 * Runner
 *
 * Combine jetty classes into a single executable jar and run webapps based on the args to it.
 * 
 */
public class Runner
{
    private static final Logger LOG = Log.getLogger(Runner.class);

    public static final String[] __plusConfigurationClasses = new String[] {
            org.eclipse.jetty.webapp.WebInfConfiguration.class.getCanonicalName(),
            org.eclipse.jetty.webapp.WebXmlConfiguration.class.getCanonicalName(),
            org.eclipse.jetty.webapp.MetaInfConfiguration.class.getCanonicalName(),
            org.eclipse.jetty.webapp.FragmentConfiguration.class.getCanonicalName(),
            org.eclipse.jetty.plus.webapp.EnvConfiguration.class.getCanonicalName(),
            org.eclipse.jetty.plus.webapp.PlusConfiguration.class.getCanonicalName(),
            org.eclipse.jetty.annotations.AnnotationConfiguration.class.getCanonicalName(),
            org.eclipse.jetty.webapp.JettyWebXmlConfiguration.class.getCanonicalName(),
            org.eclipse.jetty.webapp.TagLibConfiguration.class.getCanonicalName() 
            };
    public static final String __containerIncludeJarPattern =  ".*/jetty-runner-[^/]*\\.jar$";
    public static final String __defaultContextPath = "/";
    public static final int __defaultPort = 8080;

    protected Server _server;
    protected URLClassLoader _classLoader;
    protected Classpath _classpath = new Classpath();
    protected ContextHandlerCollection _contexts;
    protected RequestLogHandler _logHandler;
    protected String _logFile;
    protected ArrayList<String> _configFiles;
    protected boolean _enableStats=false;
    protected String _statsPropFile;


    
    /**
     * Classpath
     *
     *
     */
    public class Classpath 
    {
        private  List<URL> _classpath = new ArrayList<URL>();
        
        public void addJars (Resource lib) throws MalformedURLException, IOException
        {
            if (lib == null || !lib.exists())
                throw new IllegalStateException ("No such lib: "+lib);
            
            String[] list = lib.list();
            if (list==null)
                return;

            for (String path : list)
            {
                if (".".equals(path) || "..".equals(path))
                    continue;

                Resource item = lib.addPath(path);

                if (item.isDirectory())
                    addJars(item);
                else
                {
                    if (path.toLowerCase().endsWith(".jar") ||
                        path.toLowerCase().endsWith(".zip"))
                    {
                        URL url = item.getURL();
                        _classpath.add(url);
                    }
                }
            }
        }
        
        
        public void addPath (Resource path)
        {
            if (path == null || !path.exists())
                throw new IllegalStateException ("No such path: "+path);
            _classpath.add(path.getURL());
        }
        
        
        public URL[] asArray ()
        {
            return _classpath.toArray(new URL[_classpath.size()]);
        }
    }
    
    
    

    /**
     * 
     */
    public Runner()
    {

    }


    /**
     * Generate helpful usage message and exit
     * 
     * @param error
     */
    public void usage(String error)
    {
        if (error!=null)
            System.err.println("ERROR: "+error);
        System.err.println("Usage: java [-Djetty.home=dir] -jar jetty-runner.jar [--help|--version] [ server opts] [[ context opts] context ...] ");
        System.err.println("Server Options:");
        System.err.println(" --version                          - display version and exit");
        System.err.println(" --log file                         - request log filename (with optional 'yyyy_mm_dd' wildcard");
        System.err.println(" --out file                         - info/warn/debug log filename (with optional 'yyyy_mm_dd' wildcard");
        System.err.println(" --port n                           - port to listen on (default 8080)");
        System.err.println(" --stop-port n                      - port to listen for stop command");
        System.err.println(" --stop-key n                       - security string for stop command (required if --stop-port is present)");
        System.err.println(" --jar file                         - a jar to be added to the classloader");
        System.err.println(" --lib dir                          - a directory of jars to be added to the classloader");
        System.err.println(" --classes dir                      - a directory of classes to be added to the classloader");
        System.err.println(" --stats [unsecure|realm.properties] - enable stats gathering servlet context");
        System.err.println(" [--config file]*n                   - each --config parameter specifies the name of a jetty xml config file to apply (in the order defined)");
        System.err.println("Context Options:");
        System.err.println(" --path /path                       - context path (default /)");
        System.err.println(" context                            - WAR file, web app dir or context xml file");
        System.exit(1);
    }

    
    /**
     * Generate version message and exit
     */
    public void version ()
    {
        System.err.println("org.eclipse.jetty.runner.Runner: "+Server.getVersion());
        System.exit(1);
    }
    
    
    
    /**
     * Configure a jetty instance and deploy the webapps presented as args
     * 
     * @param args
     * @throws Exception
     */
    public void configure(String[] args) throws Exception
    {
        // handle classpath bits first so we can initialize the log mechanism.
        for (int i=0;i<args.length;i++)
        {
            if ("--lib".equals(args[i]))
            {
                Resource lib = Resource.newResource(args[++i]);
                if (!lib.exists() || !lib.isDirectory())
                    usage("No such lib directory "+lib);
                _classpath.addJars(lib);
            }
            else if ("--jar".equals(args[i]))
            {
                Resource jar = Resource.newResource(args[++i]);
                if (!jar.exists() || jar.isDirectory())
                    usage("No such jar "+jar);
                _classpath.addPath(jar);
            }
            else if ("--classes".equals(args[i]))
            {
                Resource classes = Resource.newResource(args[++i]);
                if (!classes.exists() || !classes.isDirectory())
                    usage("No such classes directory "+classes);
                _classpath.addPath(classes);
            }
            else if (args[i].startsWith("--"))
                i++;
        }

        initClassLoader();

        LOG.info("Runner");
        LOG.debug("Runner classpath {}",_classpath);

        String contextPath = __defaultContextPath;
        boolean contextPathSet = false;
        int port = __defaultPort;
        int stopPort = 0;
        String stopKey = null;

        boolean runnerServerInitialized = false;

        for (int i=0;i<args.length;i++)
        {
            if ("--port".equals(args[i]))
                port=Integer.parseInt(args[++i]);
            else if ("--stop-port".equals(args[i]))
                stopPort=Integer.parseInt(args[++i]);
            else if ("--stop-key".equals(args[i]))
                stopKey=args[++i];
            else if ("--log".equals(args[i]))
                _logFile=args[++i];
            else if ("--out".equals(args[i]))
            {
                String outFile=args[++i];
                PrintStream out = new PrintStream(new RolloverFileOutputStream(outFile,true,-1));
                LOG.info("Redirecting stderr/stdout to "+outFile);
                System.setErr(out);
                System.setOut(out);
            }
            else if ("--path".equals(args[i]))
            {
                contextPath=args[++i];
                contextPathSet=true;
            }
            else if ("--config".equals(args[i]))
            {
                if (_configFiles == null)
                    _configFiles = new ArrayList<String>();
                _configFiles.add(args[++i]);
            }
            else if ("--lib".equals(args[i]))
            {
                ++i;//skip
            }
            else if ("--jar".equals(args[i]))
            {
                ++i; //skip
            }
            else if ("--classes".equals(args[i]))
            {
                ++i;//skip
            }
            else if ("--stats".equals( args[i]))
            {
                _enableStats = true;
                _statsPropFile = args[++i];
                _statsPropFile = ("unsecure".equalsIgnoreCase(_statsPropFile)?null:_statsPropFile);
            }
            else // process contexts
            {
                if (!runnerServerInitialized) // log handlers not registered, server maybe not created, etc
                {
                    if (_server == null) // server not initialized yet
                    {
                        // build the server
                        _server = new Server();
                    }

                    //apply jetty config files if there are any
                    if (_configFiles != null)
                    {
                        for (String cfg:_configFiles)
                        {
                            XmlConfiguration xmlConfiguration = new XmlConfiguration(Resource.newResource(cfg).getURL());
                            xmlConfiguration.configure(_server);
                        }
                    }

                    //check that everything got configured, and if not, make the handlers
                    HandlerCollection handlers = (HandlerCollection) _server.getChildHandlerByClass(HandlerCollection.class);
                    if (handlers == null)
                    {
                        handlers = new HandlerCollection();
                        _server.setHandler(handlers);
                    }
                    
                    //check if contexts already configured
                    _contexts = (ContextHandlerCollection) handlers.getChildHandlerByClass(ContextHandlerCollection.class);
                    if (_contexts == null)
                    {
                        _contexts = new ContextHandlerCollection();
                        prependHandler(_contexts, handlers);
                    }
                    
                  
                    if (_enableStats)
                    {
                        //if no stats handler already configured
                        if (handlers.getChildHandlerByClass(StatisticsHandler.class) == null)
                        {
                            StatisticsHandler statsHandler = new StatisticsHandler();
                            
                            
                            Handler oldHandler = _server.getHandler();
                            statsHandler.setHandler(oldHandler);
                            _server.setHandler(statsHandler);
                         
                            
                            ServletContextHandler statsContext = new ServletContextHandler(_contexts, "/stats");
                            statsContext.addServlet(new ServletHolder(new StatisticsServlet()), "/");
                            statsContext.setSessionHandler(new SessionHandler());
                            if (_statsPropFile != null)
                            {
                                HashLoginService loginService = new HashLoginService("StatsRealm", _statsPropFile);
                                Constraint constraint = new Constraint();
                                constraint.setName("Admin Only");
                                constraint.setRoles(new String[]{"admin"});
                                constraint.setAuthenticate(true);

                                ConstraintMapping cm = new ConstraintMapping();
                                cm.setConstraint(constraint);
                                cm.setPathSpec("/*");

                                ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
                                securityHandler.setLoginService(loginService);
                                securityHandler.setConstraintMappings(Collections.singletonList(cm));
                                securityHandler.setAuthenticator(new BasicAuthenticator());
                                statsContext.setSecurityHandler(securityHandler);
                            }
                        }
                    }
                   
                    //ensure a DefaultHandler is present
                    if (handlers.getChildHandlerByClass(DefaultHandler.class) == null)
                    {
                        handlers.addHandler(new DefaultHandler());
                    }
                  
                    //ensure a log handler is present
                    _logHandler = (RequestLogHandler)handlers.getChildHandlerByClass( RequestLogHandler.class );
                    if ( _logHandler == null )
                    {
                        _logHandler = new RequestLogHandler();
                        handlers.addHandler( _logHandler );
                    }
                    

                    //check a connector is configured to listen on
                    Connector[] connectors = _server.getConnectors();
                    if (connectors == null || connectors.length == 0)
                    {
                        ServerConnector connector = new ServerConnector(_server);
                        connector.setPort(port);
                        _server.addConnector(connector);
                        if (_enableStats)
                            connector.addBean(new ConnectorStatistics());
                    }
                    else
                    {
                        if (_enableStats)
                        {
                            for (int j=0; j<connectors.length; j++)
                            {
                                ((AbstractConnector)connectors[j]).addBean(new ConnectorStatistics());
                            }
                        }
                    }

                    runnerServerInitialized = true;
                }

                // Create a context
                Resource ctx = Resource.newResource(args[i]);
                if (!ctx.exists())
                    usage("Context '"+ctx+"' does not exist");
                
                if (contextPathSet && !(contextPath.startsWith("/")))
                    contextPath = "/"+contextPath;

                // Configure the context
                if (!ctx.isDirectory() && ctx.toString().toLowerCase().endsWith(".xml"))
                {
                    // It is a context config file
                    XmlConfiguration xmlConfiguration=new XmlConfiguration(ctx.getURL());
                    xmlConfiguration.getIdMap().put("Server",_server);
                    ContextHandler handler=(ContextHandler)xmlConfiguration.configure();
                    if (contextPathSet)
                        handler.setContextPath(contextPath);
                    _contexts.addHandler(handler);                   
                    handler.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", __containerIncludeJarPattern); 
                }
                else
                {
                    // assume it is a WAR file
                    WebAppContext webapp = new WebAppContext(_contexts,ctx.toString(),contextPath);
                    webapp.setConfigurationClasses(__plusConfigurationClasses);
                    webapp.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                                        __containerIncludeJarPattern);
                }
                
                //reset
                contextPathSet = false;
                contextPath = __defaultContextPath;
            }
        }

        if (_server==null)
            usage("No Contexts defined");
        _server.setStopAtShutdown(true);

        switch ((stopPort > 0 ? 1 : 0) + (stopKey != null ? 2 : 0))
        {
            case 1:
                usage("Must specify --stop-key when --stop-port is specified");
                break;
                
            case 2:
                usage("Must specify --stop-port when --stop-key is specified");
                break;
                
            case 3:
                ShutdownMonitor monitor = ShutdownMonitor.getInstance();
                monitor.setPort(stopPort);
                monitor.setKey(stopKey);
                monitor.setExitVm(true);
                break;
        }

        if (_logFile!=null)
        {
            NCSARequestLog requestLog = new NCSARequestLog(_logFile);
            requestLog.setExtended(false);
            _logHandler.setRequestLog(requestLog);
        }
    }
    
    
    /**
     * @param handler
     * @param handlers
     */
    protected void prependHandler (Handler handler, HandlerCollection handlers)
    {
        if (handler == null || handlers == null)
            return;
        
       Handler[] existing = handlers.getChildHandlers();
       Handler[] children = new Handler[existing.length + 1];
       children[0] = handler;
       System.arraycopy(existing, 0, children, 1, existing.length);
       handlers.setHandlers(children);
    }

    
    

    /**
     * @throws Exception
     */
    public void run() throws Exception
    {
        _server.start();
        _server.join();
    }


    /**
     * Establish a classloader with custom paths (if any)
     */
    protected void initClassLoader()
    {
        URL[] paths = _classpath.asArray();
             
        if (_classLoader==null && paths !=null && paths.length > 0)
        {
            ClassLoader context=Thread.currentThread().getContextClassLoader();

            if (context==null)
                _classLoader=new URLClassLoader(paths);
            else
                _classLoader=new URLClassLoader(paths, context);

            Thread.currentThread().setContextClassLoader(_classLoader);
        }
    }




    /**
     * @param args
     */
    public static void main(String[] args)
    {
        Runner runner = new Runner();

        try
        {
            if (args.length>0&&args[0].equalsIgnoreCase("--help"))
            {
                runner.usage(null);
            }
            else if (args.length>0&&args[0].equalsIgnoreCase("--version"))
            {
                runner.version();
            }
            else
            {
                runner.configure(args);
                runner.run();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            runner.usage(null);
        }
    }
}
