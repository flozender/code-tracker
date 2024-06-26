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
package org.eclipse.jetty.webapp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.UnavailableException;

import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.security.ConstraintAware;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlParser;

/* ------------------------------------------------------------------------------- */
/**
 * Configure by parsing default web.xml and web.xml
 * 
 * 
 */
public class WebXmlConfiguration implements Configuration
{
    protected WebAppContext _context;
    protected XmlParser _xmlParser;
    protected Object _filters;
    protected Object _filterMappings;
    protected Object _servlets;
    protected Object _servletMappings;
    protected Object _welcomeFiles;
    protected Object _constraintMappings;
    protected final Set<String> _roles = new HashSet<String>();
    protected Object _listeners;
    protected Map _errorPages;
    protected boolean _hasJSP;
    protected String _jspServletName;
    protected String _jspServletClass;
    protected boolean _defaultWelcomeFileList;
    protected SecurityHandler _securityHandler;
    protected ServletHandler _servletHandler;
    protected int _version;
    protected boolean _metaDataComplete = false;
    protected URL _webxml;
    
    

    public WebXmlConfiguration() throws ClassNotFoundException
    {
        // Get parser
        _xmlParser = webXmlParser();
    }

    public static XmlParser webXmlParser() throws ClassNotFoundException
    {
        XmlParser xmlParser=new XmlParser();
        //set up cache of DTDs and schemas locally        
        URL dtd22=Loader.getResource(Servlet.class,"javax/servlet/resources/web-app_2_2.dtd",true);
        URL dtd23=Loader.getResource(Servlet.class,"javax/servlet/resources/web-app_2_3.dtd",true);
        URL j2ee14xsd=Loader.getResource(Servlet.class,"javax/servlet/resources/j2ee_1_4.xsd",true);
        URL webapp24xsd=Loader.getResource(Servlet.class,"javax/servlet/resources/web-app_2_4.xsd",true);
        URL webapp25xsd=Loader.getResource(Servlet.class,"javax/servlet/resources/web-app_2_5.xsd",true);
        URL webapp30xsd=Loader.getResource(Servlet.class,"javax/servlet/resources/web-app_3_0.xsd",true);
        URL webcommon30xsd=Loader.getResource(Servlet.class,"javax/servlet/resources/web-common_3_0.xsd",true);
        URL webfragment30xsd=Loader.getResource(Servlet.class,"javax/servlet/resources/web-fragment_3_0.xsd",true);
        URL schemadtd=Loader.getResource(Servlet.class,"javax/servlet/resources/XMLSchema.dtd",true);
        URL xmlxsd=Loader.getResource(Servlet.class,"javax/servlet/resources/xml.xsd",true);
        URL webservice11xsd=Loader.getResource(Servlet.class,"javax/servlet/resources/j2ee_web_services_client_1_1.xsd",true);
        URL webservice12xsd=Loader.getResource(Servlet.class,"javax/servlet/resources/javaee_web_services_client_1_2.xsd",true);
        URL datatypesdtd=Loader.getResource(Servlet.class,"javax/servlet/resources/datatypes.dtd",true);

        URL jsp20xsd = null;
        URL jsp21xsd = null;

        try
        {
            Class jsp_page = Loader.loadClass(WebXmlConfiguration.class, "javax.servlet.jsp.JspPage");
            jsp20xsd = jsp_page.getResource("/javax/servlet/resources/jsp_2_0.xsd");
            jsp21xsd = jsp_page.getResource("/javax/servlet/resources/jsp_2_1.xsd");
        }
        catch (Exception e)
        {
            Log.ignore(e);
        }
        finally
        {
            if (jsp20xsd == null) jsp20xsd = Loader.getResource(Servlet.class, "javax/servlet/resources/jsp_2_0.xsd", true);
            if (jsp21xsd == null) jsp21xsd = Loader.getResource(Servlet.class, "javax/servlet/resources/jsp_2_1.xsd", true);
        }
        
        redirect(xmlParser,"web-app_2_2.dtd",dtd22);
        redirect(xmlParser,"-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN",dtd22);
        redirect(xmlParser,"web.dtd",dtd23);
        redirect(xmlParser,"web-app_2_3.dtd",dtd23);
        redirect(xmlParser,"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN",dtd23);
        redirect(xmlParser,"XMLSchema.dtd",schemadtd);
        redirect(xmlParser,"http://www.w3.org/2001/XMLSchema.dtd",schemadtd);
        redirect(xmlParser,"-//W3C//DTD XMLSCHEMA 200102//EN",schemadtd);
        redirect(xmlParser,"jsp_2_0.xsd",jsp20xsd);
        redirect(xmlParser,"http://java.sun.com/xml/ns/j2ee/jsp_2_0.xsd",jsp20xsd);
        redirect(xmlParser,"jsp_2_1.xsd",jsp21xsd);
        redirect(xmlParser,"http://java.sun.com/xml/ns/javaee/jsp_2_1.xsd",jsp21xsd);
        redirect(xmlParser,"j2ee_1_4.xsd",j2ee14xsd);
        redirect(xmlParser,"http://java.sun.com/xml/ns/j2ee/j2ee_1_4.xsd",j2ee14xsd);
        redirect(xmlParser,"web-app_2_4.xsd",webapp24xsd);
        redirect(xmlParser,"http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd",webapp24xsd);
        redirect(xmlParser,"web-app_2_5.xsd",webapp25xsd);
        redirect(xmlParser,"http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd",webapp25xsd);
        redirect(xmlParser,"web-app_3_0.xsd",webapp30xsd);
        redirect(xmlParser,"http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd",webapp30xsd);
        redirect(xmlParser,"web-common_3_0.xsd",webcommon30xsd);
        redirect(xmlParser,"http://java.sun.com/xml/ns/javaee/web-common_3_0.xsd",webcommon30xsd);
        redirect(xmlParser,"web-fragment_3_0.xsd",webfragment30xsd);
        redirect(xmlParser,"http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd",webfragment30xsd);
        redirect(xmlParser,"xml.xsd",xmlxsd);
        redirect(xmlParser,"http://www.w3.org/2001/xml.xsd",xmlxsd);
        redirect(xmlParser,"datatypes.dtd",datatypesdtd);
        redirect(xmlParser,"http://www.w3.org/2001/datatypes.dtd",datatypesdtd);
        redirect(xmlParser,"j2ee_web_services_client_1_1.xsd",webservice11xsd);
        redirect(xmlParser,"http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_1.xsd",webservice11xsd);
        redirect(xmlParser,"javaee_web_services_client_1_2.xsd",webservice12xsd);
        redirect(xmlParser,"http://www.ibm.com/webservices/xsd/javaee_web_services_client_1_2.xsd",webservice12xsd);
        
        return xmlParser;
    }

    /* ------------------------------------------------------------------------------- */
    private static void redirect(XmlParser parser, String resource, URL source)
    {
        if (source != null) parser.redirectEntity(resource, source);
    }

    /* ------------------------------------------------------------------------------- */
    public void setWebAppContext(WebAppContext context)
    {
        _context = context;
    }

    /* ------------------------------------------------------------------------------- */
    public WebAppContext getWebAppContext()
    {
        return _context;
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Configure ClassPath.
     */
    public void configureClassLoader() throws Exception
    {
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Process webdefaults.xml
     * 
     * @see org.eclipse.jetty.server.server.webapp.Configuration#configureDefaults()
     */
    public void configureDefaults() throws Exception
    {
        // cannot configure if the context is already started
        if (_context.isStarted())
        {
            if (Log.isDebugEnabled())
            {
                Log.debug("Cannot configure webapp after it is started");
            }
            return;
        }
        String defaultsDescriptor = getWebAppContext().getDefaultsDescriptor();
        if (defaultsDescriptor != null && defaultsDescriptor.length() > 0)
        {
            Resource dftResource = Resource.newSystemResource(defaultsDescriptor);
            if (dftResource == null) dftResource = _context.newResource(defaultsDescriptor);

            // don't initialize the version and therefore the metadata from
            // webdefault.xml
            XmlParser.Node config = null;
            config = _xmlParser.parse(dftResource.getURL().toString());
            initialize(config);
            _defaultWelcomeFileList = _welcomeFiles != null;
        }
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Process web.xml
     * 
     * @see org.eclipse.jetty.server.server.webapp.Configuration#configureWebApp()
     */
    public void configureWebApp() throws Exception
    {
        // cannot configure if the context is already started
        if (_context.isStarted())
        {
            if (Log.isDebugEnabled()) Log.debug("Cannot configure webapp after it is started");
            return;
        }

        _webxml = findWebXml();
        if (_webxml != null) configure(_webxml.toString());

        String overrideDescriptor = getWebAppContext().getOverrideDescriptor();
        if (overrideDescriptor != null && overrideDescriptor.length() > 0)
        {
            Resource orideResource = Resource.newSystemResource(overrideDescriptor);
            if (orideResource == null) orideResource = _context.newResource(overrideDescriptor);
            _xmlParser.setValidating(false);
            configure(orideResource.getURL().toString());
        }

        // TODO is this before or after the overrides?
        configureWebFragments();
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Look for any web.xml fragments in META-INF of jars in WEB-INF/lib
     * 
     * @throws Exception
     */
    public void configureWebFragments() throws Exception
    {
        Log.debug("metadata-complete " + _metaDataComplete);

        // if metadata-complete is true in web.xml, do not search for fragments
        if (_metaDataComplete) return;

        // either there is no web.xml, or it set metadata-complete to false, so
        // we need to look for fragments in WEB-INF/lib
        // Check to see if a specific search pattern has been set.
        String tmp = (String) _context.getInitParameter("org.eclipse.jetty.webapp.WebXmlFragmentPattern");
        Pattern webFragPattern = (tmp == null ? null : Pattern.compile(tmp));

        JarScanner fragScanner = new JarScanner()
        {
            public void processEntry(URL jarUrl, JarEntry entry)
            {
                try
                {
                    String name = entry.getName();
                    if (name.toLowerCase().equals("meta-inf/web.xml"))
                    {
                        Resource webXmlFrag = _context.newResource("jar:" + jarUrl + "!/" + name);
                        Log.debug("web.xml fragment found {}", webXmlFrag);
                        // Process web.xml
                        // web-fragment
                        // servlet
                        // servlet-mapping
                        // filter
                        // filter-mapping
                        // listener
                        XmlParser.Node config = null;
                        config = _xmlParser.parse(webXmlFrag.toString());
                        initialize(config);
                    }
                }
                catch (Exception e)
                {
                    Log.warn("Problem processing jar entry " + entry, e);
                }
            }
        };
        fragScanner.scan(webFragPattern, Thread.currentThread().getContextClassLoader(), true, false);
    }

    /* ------------------------------------------------------------------------------- */
    protected URL findWebXml() throws IOException, MalformedURLException
    {
        String descriptor = getWebAppContext().getDescriptor();
        if (descriptor != null)
        {
            Resource web = _context.newResource(descriptor);
            if (web.exists() && !web.isDirectory()) return web.getURL();
        }

        Resource web_inf = getWebAppContext().getWebInf();
        if (web_inf != null && web_inf.isDirectory())
        {
            // do web.xml file
            Resource web = web_inf.addPath("web.xml");
            if (web.exists()) return web.getURL();
            Log.debug("No WEB-INF/web.xml in " + getWebAppContext().getWar() + ". Serving files and default/dynamic servlets only");
        }
        return null;
    }

    /* ------------------------------------------------------------------------------- */
    public void configure(String webXml) throws Exception
    {
        XmlParser.Node config = null;
        config = _xmlParser.parse(webXml);
        initializeVersion(config);
        initialize(config);
    }

    /* ------------------------------------------------------------------------------- */
    public void deconfigureWebApp() throws Exception
    {
        // TODO preserve any configuration that pre-existed.

        _servletHandler = getWebAppContext().getServletHandler();
        _securityHandler = (SecurityHandler)getWebAppContext().getSecurityHandler();
       
        _servletHandler.setFilters(null);
        _servletHandler.setFilterMappings(null);
        _servletHandler.setServlets(null);
        _servletHandler.setServletMappings(null);

        getWebAppContext().setEventListeners(null);
        getWebAppContext().setWelcomeFiles(null);
        if (_securityHandler instanceof ConstraintAware) ((ConstraintAware) _securityHandler).setConstraintMappings(null, _roles);

        if (getWebAppContext().getErrorHandler() instanceof ErrorPageErrorHandler)
            ((ErrorPageErrorHandler) getWebAppContext().getErrorHandler()).setErrorPages(null);

        
        // TODO remove classpaths from classloader
    }

    /* ------------------------------------------------------------ */
    protected void initializeVersion(XmlParser.Node config)
    {
        String version = config.getAttribute("version", "DTD");
        if ("2.5".equals(version))
            _version = 25;
        else if ("2.4".equals(version))
            _version = 24;
        else if ("3.0".equals(version))
            _version = 30;
        else if ("DTD".equals(version))
        {
            _version = 23;
            String dtd = _xmlParser.getDTD();
            if (dtd != null && dtd.indexOf("web-app_2_2") >= 0) _version = 22;
        }

        if (_version < 25)
            _metaDataComplete = true; // does not apply before 2.5
        else
            _metaDataComplete = Boolean.valueOf((String) config.getAttribute("metadata-complete", "false")).booleanValue();

        Log.debug("Calculated metadatacomplete = " + _metaDataComplete + " with version=" + version);

        _context.setAttribute("metadata-complete", String.valueOf(_metaDataComplete));
    }

    /* ------------------------------------------------------------ */
    protected void initialize(XmlParser.Node config) throws ClassNotFoundException, UnavailableException
    {
        _servletHandler = getWebAppContext().getServletHandler();
        _securityHandler = (SecurityHandler)getWebAppContext().getSecurityHandler();
        
        // Get any existing servlets and mappings.
        _filters = LazyList.array2List(_servletHandler.getFilters());
        _filterMappings = LazyList.array2List(_servletHandler.getFilterMappings());
        _servlets = LazyList.array2List(_servletHandler.getServlets());
        _servletMappings = LazyList.array2List(_servletHandler.getServletMappings());

        _listeners = LazyList.array2List(getWebAppContext().getEventListeners());
        _welcomeFiles = LazyList.array2List(getWebAppContext().getWelcomeFiles());

        if (_securityHandler instanceof ConstraintAware)
        {
            _constraintMappings = LazyList.array2List(((ConstraintAware) _securityHandler).getConstraintMappings());
            
            if (((ConstraintAware) _securityHandler).getRoles() != null)
            {
                _roles.addAll(((ConstraintAware) _securityHandler).getRoles());
            }
        }

        _errorPages = getWebAppContext().getErrorHandler() instanceof ErrorPageErrorHandler ? ((ErrorPageErrorHandler) getWebAppContext().getErrorHandler())
                                                                                                                                                            .getErrorPages() : null;

        Iterator iter = config.iterator();
        XmlParser.Node node = null;
        while (iter.hasNext())
        {
            try
            {
                Object o = iter.next();
                if (!(o instanceof XmlParser.Node)) continue;
                node = (XmlParser.Node) o;
                String name = node.getTag();
                initWebXmlElement(name, node);
            }
            catch (ClassNotFoundException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Log.warn("Configuration problem at " + node, e);
                throw new UnavailableException("Configuration problem");
            }
        }

        _servletHandler.setFilters((FilterHolder[]) LazyList.toArray(_filters, FilterHolder.class));
        _servletHandler.setFilterMappings((FilterMapping[]) LazyList.toArray(_filterMappings, FilterMapping.class));
        _servletHandler.setServlets((ServletHolder[]) LazyList.toArray(_servlets, ServletHolder.class));
        _servletHandler.setServletMappings((ServletMapping[]) LazyList.toArray(_servletMappings, ServletMapping.class));

        getWebAppContext().setEventListeners((EventListener[]) LazyList.toArray(_listeners, EventListener.class));
        getWebAppContext().setWelcomeFiles((String[]) LazyList.toArray(_welcomeFiles, String.class));
        // TODO jaspi check this
        if (_securityHandler instanceof ConstraintAware)
        {
            ((ConstraintAware) _securityHandler).setConstraintMappings((ConstraintMapping[]) LazyList.toArray(_constraintMappings,
                                                                                                              ConstraintMapping.class),
                                                                                                               _roles);
        }

        if (_errorPages != null && getWebAppContext().getErrorHandler() instanceof ErrorPageErrorHandler)
            ((ErrorPageErrorHandler) getWebAppContext().getErrorHandler()).setErrorPages(_errorPages);

    }

    /* ------------------------------------------------------------ */
    /**
     * Handle web.xml element. This method is called for each top level element
     * within the web.xml file. It may be specialized by derived WebAppHandlers
     * to provide additional configuration and handling.
     * 
     * @param element The element name
     * @param node The node containing the element.
     */
    protected void initWebXmlElement(String element, XmlParser.Node node) throws Exception
    {
        if ("display-name".equals(element))
            initDisplayName(node);
        else if ("description".equals(element))
        {
        }
        else if ("context-param".equals(element))
            initContextParam(node);
        else if ("servlet".equals(element))
            initServlet(node);
        else if ("servlet-mapping".equals(element))
            initServletMapping(node);
        else if ("session-config".equals(element))
            initSessionConfig(node);
        else if ("mime-mapping".equals(element))
            initMimeConfig(node);
        else if ("welcome-file-list".equals(element))
            initWelcomeFileList(node);
        else if ("locale-encoding-mapping-list".equals(element))
            initLocaleEncodingList(node);
        else if ("error-page".equals(element))
            initErrorPage(node);
        else if ("taglib".equals(element))
            initTagLib(node);
        else if ("jsp-config".equals(element))
            initJspConfig(node);
        else if ("resource-ref".equals(element))
        {
            if (Log.isDebugEnabled()) Log.debug("No implementation: " + node);
        }
        else if ("security-constraint".equals(element))
            initSecurityConstraint(node);
        else if ("login-config".equals(element))
            initLoginConfig(node);
        else if ("security-role".equals(element))
            initSecurityRole(node);
        else if ("filter".equals(element))
            initFilter(node);
        else if ("filter-mapping".equals(element))
            initFilterMapping(node);
        else if ("listener".equals(element))
            initListener(node);
        else if ("distributable".equals(element))
            initDistributable(node);
        else if ("web-fragment".equals(element))
        {
        }
        else
        {
            if (Log.isDebugEnabled())
            {
                Log.debug("Element {} not handled in {}", element, this);
                Log.debug(node.toString());
            }
        }
    }

    /* ------------------------------------------------------------ */
    protected void initDisplayName(XmlParser.Node node)
    {
        getWebAppContext().setDisplayName(node.toString(false, true));
    }

    /* ------------------------------------------------------------ */
    protected void initContextParam(XmlParser.Node node)
    {
        String name = node.getString("param-name", false, true);
        String value = node.getString("param-value", false, true);
        if (Log.isDebugEnabled()) Log.debug("ContextParam: " + name + "=" + value);
        getWebAppContext().getInitParams().put(name, value);
    }

    /* ------------------------------------------------------------ */
    protected void initFilter(XmlParser.Node node)
    {
        String name = node.getString("filter-name", false, true);
        FilterHolder holder = _servletHandler.getFilter(name);
        if (holder == null)
        {
            holder = _servletHandler.newFilterHolder();
            holder.setName(name);
            _filters = LazyList.add(_filters, holder);
        }

        String filter_class = node.getString("filter-class", false, true);
        if (filter_class != null) holder.setClassName(filter_class);

        Iterator iter = node.iterator("init-param");
        while (iter.hasNext())
        {
            XmlParser.Node paramNode = (XmlParser.Node) iter.next();
            String pname = paramNode.getString("param-name", false, true);
            String pvalue = paramNode.getString("param-value", false, true);
            holder.setInitParameter(pname, pvalue);
        }

        String async=node.getString("async-support",false,true);
        if (async!=null)
            holder.setAsyncSupported(Boolean.valueOf(async));
    }

    /* ------------------------------------------------------------ */
    protected void initFilterMapping(XmlParser.Node node)
    {
        String filter_name = node.getString("filter-name", false, true);

        FilterMapping mapping = new FilterMapping();

        mapping.setFilterName(filter_name);

        ArrayList paths = new ArrayList();
        Iterator iter = node.iterator("url-pattern");
        while (iter.hasNext())
        {
            String p = ((XmlParser.Node) iter.next()).toString(false, true);
            p = normalizePattern(p);
            paths.add(p);
        }
        mapping.setPathSpecs((String[]) paths.toArray(new String[paths.size()]));

        ArrayList names = new ArrayList();
        iter = node.iterator("servlet-name");
        while (iter.hasNext())
        {
            String n = ((XmlParser.Node) iter.next()).toString(false, true);
            names.add(n);
        }
        mapping.setServletNames((String[]) names.toArray(new String[names.size()]));

        int dispatcher=FilterMapping.DEFAULT;
        iter=node.iterator("dispatcher");
        while(iter.hasNext())
        {
            String d=((XmlParser.Node)iter.next()).toString(false,true);
            dispatcher|=FilterMapping.dispatch(d);
        }
        mapping.setDispatches(dispatcher);

        _filterMappings = LazyList.add(_filterMappings, mapping);
    }

    /* ------------------------------------------------------------ */
    protected String normalizePattern(String p)
    {
        if (p != null && p.length() > 0 && !p.startsWith("/") && !p.startsWith("*")) return "/" + p;
        return p;
    }

    /* ------------------------------------------------------------ */
    protected void initServlet(XmlParser.Node node)
    {
        String id = node.getAttribute("id");

        // initialize holder
        String servlet_name = node.getString("servlet-name", false, true);
        ServletHolder holder = _servletHandler.getServlet(servlet_name);
        if (holder == null)
        {
            holder = _servletHandler.newServletHolder();
            holder.setName(servlet_name);
            _servlets = LazyList.add(_servlets, holder);
        }

        // init params
        Iterator iParamsIter = node.iterator("init-param");
        while (iParamsIter.hasNext())
        {
            XmlParser.Node paramNode = (XmlParser.Node) iParamsIter.next();
            String pname = paramNode.getString("param-name", false, true);
            String pvalue = paramNode.getString("param-value", false, true);
            holder.setInitParameter(pname, pvalue);
        }

        String servlet_class = node.getString("servlet-class", false, true);

        // Handle JSP
        if (id != null && id.equals("jsp"))
        {
            _jspServletName = servlet_name;
            _jspServletClass = servlet_class;
            try
            {
                Loader.loadClass(this.getClass(), servlet_class);
                _hasJSP = true;
            }
            catch (ClassNotFoundException e)
            {
                Log.info("NO JSP Support for {}, did not find {}", _context.getContextPath(), servlet_class);
                _hasJSP = false;
                _jspServletClass = servlet_class = "org.eclipse.jetty.servlet.NoJspServlet";
            }
            if (holder.getInitParameter("scratchdir") == null)
            {
                File tmp = getWebAppContext().getTempDirectory();
                File scratch = new File(tmp, "jsp");
                if (!scratch.exists()) scratch.mkdir();
                holder.setInitParameter("scratchdir", scratch.getAbsolutePath());

                if ("?".equals(holder.getInitParameter("classpath")))
                {
                    String classpath = getWebAppContext().getClassPath();
                    Log.debug("classpath=" + classpath);
                    if (classpath != null) holder.setInitParameter("classpath", classpath);
                }
            }
        }
        if (servlet_class != null) holder.setClassName(servlet_class);

        // Handler JSP file
        String jsp_file = node.getString("jsp-file", false, true);
        if (jsp_file != null)
        {
            holder.setForcedPath(jsp_file);
            holder.setClassName(_jspServletClass);
        }

        // handle startup
        XmlParser.Node startup = node.get("load-on-startup");
        if (startup != null)
        {
            String s = startup.toString(false, true).toLowerCase();
            if (s.startsWith("t"))
            {
                Log.warn("Deprecated boolean load-on-startup.  Please use integer");
                holder.setInitOrder(1);
            }
            else
            {
                int order = 0;
                try
                {
                    if (s != null && s.trim().length() > 0) order = Integer.parseInt(s);
                }
                catch (Exception e)
                {
                    Log.warn("Cannot parse load-on-startup " + s + ". Please use integer");
                    Log.ignore(e);
                }
                holder.setInitOrder(order);
            }
        }

        Iterator sRefsIter = node.iterator("security-role-ref");
        while (sRefsIter.hasNext())
        {
            XmlParser.Node securityRef = (XmlParser.Node) sRefsIter.next();
            String roleName = securityRef.getString("role-name", false, true);
            String roleLink = securityRef.getString("role-link", false, true);
            if (roleName != null && roleName.length() > 0 && roleLink != null && roleLink.length() > 0)
            {
                if (Log.isDebugEnabled()) Log.debug("link role " + roleName + " to " + roleLink + " for " + this);
                holder.setUserRoleLink(roleName, roleLink);
            }
            else
            {
                Log.warn("Ignored invalid security-role-ref element: " + "servlet-name=" + holder.getName() + ", " + securityRef);
            }
        }

        XmlParser.Node run_as = node.get("run-as");
        if (run_as != null)
        {
            String roleName = run_as.getString("role-name", false, true);
            if (roleName != null)
                holder.setRunAsRole(roleName);
        }

        String async=node.getString("async-support",false,true);
        if (async!=null)
            holder.setAsyncSupported(Boolean.valueOf(async));
    }

    /* ------------------------------------------------------------ */
    protected void initServletMapping(XmlParser.Node node)
    {
        String servlet_name = node.getString("servlet-name", false, true);
        ServletMapping mapping = new ServletMapping();
        mapping.setServletName(servlet_name);

        ArrayList paths = new ArrayList();
        Iterator iter = node.iterator("url-pattern");
        while (iter.hasNext())
        {
            String p = ((XmlParser.Node) iter.next()).toString(false, true);
            p = normalizePattern(p);
            paths.add(p);
        }
        mapping.setPathSpecs((String[]) paths.toArray(new String[paths.size()]));

        _servletMappings = LazyList.add(_servletMappings, mapping);
    }

    /* ------------------------------------------------------------ */
    protected void initListener(XmlParser.Node node)
    {
        String className = node.getString("listener-class", false, true);
        Object listener = null;
        try
        {
            Class listenerClass = getWebAppContext().loadClass(className);
            listener = newListenerInstance(listenerClass);
            if (!(listener instanceof EventListener))
            {
                Log.warn("Not an EventListener: " + listener);
                return;
            }
            _listeners = LazyList.add(_listeners, listener);
        }
        catch (Exception e)
        {
            Log.warn("Could not instantiate listener " + className, e);
            return;
        }
    }

    /* ------------------------------------------------------------ */
    protected Object newListenerInstance(Class clazz) throws InstantiationException, IllegalAccessException
    {
        return clazz.newInstance();
    }

    /* ------------------------------------------------------------ */
    protected void initDistributable(XmlParser.Node node)
    {
        // the element has no content, so its simple presence
        // indicates that the webapp is distributable...
        WebAppContext wac = getWebAppContext();
        if (!wac.isDistributable()) wac.setDistributable(true);
    }

    /* ------------------------------------------------------------ */
    protected void initSessionConfig(XmlParser.Node node)
    {
        XmlParser.Node tNode = node.get("session-timeout");
        if (tNode != null)
        {
            int timeout = Integer.parseInt(tNode.toString(false, true));
            getWebAppContext().getSessionHandler().getSessionManager().setMaxInactiveInterval(timeout * 60);
        }
    }

    /* ------------------------------------------------------------ */
    protected void initMimeConfig(XmlParser.Node node)
    {
        String extension = node.getString("extension", false, true);
        if (extension != null && extension.startsWith(".")) extension = extension.substring(1);
        String mimeType = node.getString("mime-type", false, true);
        getWebAppContext().getMimeTypes().addMimeMapping(extension, mimeType);
    }

    /* ------------------------------------------------------------ */
    protected void initWelcomeFileList(XmlParser.Node node)
    {
        if (_defaultWelcomeFileList) _welcomeFiles = null; // erase welcome
                                                            // files from
                                                            // default web.xml

        _defaultWelcomeFileList = false;
        Iterator iter = node.iterator("welcome-file");
        while (iter.hasNext())
        {
            XmlParser.Node indexNode = (XmlParser.Node) iter.next();
            String welcome = indexNode.toString(false, true);
            _welcomeFiles = LazyList.add(_welcomeFiles, welcome);
        }
    }

    /* ------------------------------------------------------------ */
    protected void initLocaleEncodingList(XmlParser.Node node)
    {
        Iterator iter = node.iterator("locale-encoding-mapping");
        while (iter.hasNext())
        {
            XmlParser.Node mapping = (XmlParser.Node) iter.next();
            String locale = mapping.getString("locale", false, true);
            String encoding = mapping.getString("encoding", false, true);
            getWebAppContext().addLocaleEncoding(locale, encoding);
        }
    }

    /* ------------------------------------------------------------ */
    protected void initErrorPage(XmlParser.Node node)
    {
        String error = node.getString("error-code", false, true);
        if (error == null || error.length() == 0) error = node.getString("exception-type", false, true);
        String location = node.getString("location", false, true);

        if (_errorPages == null) _errorPages = new HashMap();
        _errorPages.put(error, location);
    }

    /* ------------------------------------------------------------ */
    protected void initTagLib(XmlParser.Node node)
    {
        String uri = node.getString("taglib-uri", false, true);
        String location = node.getString("taglib-location", false, true);

        getWebAppContext().setResourceAlias(uri, location);
    }

    /* ------------------------------------------------------------ */
    protected void initJspConfig(XmlParser.Node node)
    {
        for (int i = 0; i < node.size(); i++)
        {
            Object o = node.get(i);
            if (o instanceof XmlParser.Node && "taglib".equals(((XmlParser.Node) o).getTag())) initTagLib((XmlParser.Node) o);
        }

        // Map URLs from jsp property groups to JSP servlet.
        // this is more JSP stupidness creaping into the servlet spec
        Iterator iter = node.iterator("jsp-property-group");
        Object paths = null;
        while (iter.hasNext())
        {
            XmlParser.Node group = (XmlParser.Node) iter.next();
            Iterator iter2 = group.iterator("url-pattern");
            while (iter2.hasNext())
            {
                String url = ((XmlParser.Node) iter2.next()).toString(false, true);
                url = normalizePattern(url);
                paths = LazyList.add(paths, url);
            }
        }

        if (LazyList.size(paths) > 0)
        {
            String jspName = getJSPServletName();
            if (jspName != null)
            {
                ServletMapping mapping = new ServletMapping();
                mapping.setServletName(jspName);
                mapping.setPathSpecs(LazyList.toStringArray(paths));
                _servletMappings = LazyList.add(_servletMappings, mapping);
            }
        }
    }

    /* ------------------------------------------------------------ */
    protected void initSecurityConstraint(XmlParser.Node node)
    {
        Constraint scBase = new Constraint();

        try
        {
            XmlParser.Node auths = node.get("auth-constraint");

            if (auths != null)
            {
                scBase.setAuthenticate(true);
                // auth-constraint
                Iterator iter = auths.iterator("role-name");
                Object roles = null;
                while (iter.hasNext())
                {
                    String role = ((XmlParser.Node) iter.next()).toString(false, true);
                    roles = LazyList.add(roles, role);
                }
                scBase.setRoles(LazyList.toStringArray(roles));
            }

            XmlParser.Node data = node.get("user-data-constraint");
            if (data != null)
            {
                data = data.get("transport-guarantee");
                String guarantee = data.toString(false, true).toUpperCase();
                if (guarantee == null || guarantee.length() == 0 || "NONE".equals(guarantee))
                    scBase.setDataConstraint(Constraint.DC_NONE);
                else if ("INTEGRAL".equals(guarantee))
                    scBase.setDataConstraint(Constraint.DC_INTEGRAL);
                else if ("CONFIDENTIAL".equals(guarantee))
                    scBase.setDataConstraint(Constraint.DC_CONFIDENTIAL);
                else
                {
                    Log.warn("Unknown user-data-constraint:" + guarantee);
                    scBase.setDataConstraint(Constraint.DC_CONFIDENTIAL);
                }
            }
            Iterator iter = node.iterator("web-resource-collection");
            while (iter.hasNext())
            {
                XmlParser.Node collection = (XmlParser.Node) iter.next();
                String name = collection.getString("web-resource-name", false, true);
                Constraint sc = (Constraint) scBase.clone();
                sc.setName(name);

                Iterator iter2 = collection.iterator("url-pattern");
                while (iter2.hasNext())
                {
                    String url = ((XmlParser.Node) iter2.next()).toString(false, true);
                    url = normalizePattern(url);

                    Iterator iter3 = collection.iterator("http-method");
                    if (iter3.hasNext())
                    {
                        while (iter3.hasNext())
                        {
                            String method = ((XmlParser.Node) iter3.next()).toString(false, true);
                            ConstraintMapping mapping = new ConstraintMapping();
                            mapping.setMethod(method);
                            mapping.setPathSpec(url);
                            mapping.setConstraint(sc);
                            _constraintMappings = LazyList.add(_constraintMappings, mapping);
                        }
                    }
                    else
                    {
                        ConstraintMapping mapping = new ConstraintMapping();
                        mapping.setPathSpec(url);
                        mapping.setConstraint(sc);
                        _constraintMappings = LazyList.add(_constraintMappings, mapping);
                    }
                }
            }
        }
        catch (CloneNotSupportedException e)
        {
            Log.warn(e);
        }

    }

    /* ------------------------------------------------------------ */
    protected void initLoginConfig(XmlParser.Node node) throws Exception
    {
        XmlParser.Node method = node.get("auth-method");
        if (method != null)
        {
            XmlParser.Node name = node.get("realm-name");
            _securityHandler.setRealmName(name == null ? "default" : name.toString(false, true));
            _securityHandler.setAuthMethod(method.toString(false, true));
            
            
            if (Constraint.__FORM_AUTH.equals(_securityHandler.getAuthMethod()))
            {  
                XmlParser.Node formConfig = node.get("form-login-config");
                if (formConfig != null)
                {
                    String loginPageName = null;
                    XmlParser.Node loginPage = formConfig.get("form-login-page");
                    if (loginPage != null) 
                        loginPageName = loginPage.toString(false, true);
                    String errorPageName = null;
                    XmlParser.Node errorPage = formConfig.get("form-error-page");
                    if (errorPage != null) 
                        errorPageName = errorPage.toString(false, true);
                    _securityHandler.setInitParameter(FormAuthenticator.__FORM_LOGIN_PAGE,loginPageName);
                    _securityHandler.setInitParameter(FormAuthenticator.__FORM_ERROR_PAGE,errorPageName);
                }
                else
                {
                    throw new IllegalArgumentException("!form-login-config");
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    protected void initSecurityRole(XmlParser.Node node)
    {
        XmlParser.Node roleNode = node.get("role-name");
        String role = roleNode.toString(false, true);
        _roles.add(role);
    }

    /* ------------------------------------------------------------ */
    protected String getJSPServletName()
    {
        if (_jspServletName == null)
        {
            Map.Entry entry = _context.getServletHandler().getHolderEntry("test.jsp");
            if (entry != null)
            {
                ServletHolder holder = (ServletHolder) entry.getValue();
                _jspServletName = holder.getName();
            }
        }
        return _jspServletName;
    }
}
