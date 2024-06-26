//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.http.pathmap.MappedResource;
import org.eclipse.jetty.server.ResourceCache;
import org.eclipse.jetty.server.ResourceContentFactory;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;


/** 
 * The default servlet.
 * <p>
 * This servlet, normally mapped to /, provides the handling for static
 * content, OPTION and TRACE methods for the context.
 * The following initParameters are supported, these can be set either
 * on the servlet itself or as ServletContext initParameters with a prefix
 * of org.eclipse.jetty.servlet.Default. :
 * <pre>
 *  acceptRanges      If true, range requests and responses are
 *                    supported
 *
 *  dirAllowed        If true, directory listings are returned if no
 *                    welcome file is found. Else 403 Forbidden.
 *
 *  welcomeServlets   If true, attempt to dispatch to welcome files
 *                    that are servlets, but only after no matching static
 *                    resources could be found. If false, then a welcome
 *                    file must exist on disk. If "exact", then exact
 *                    servlet matches are supported without an existing file.
 *                    Default is true.
 *
 *                    This must be false if you want directory listings,
 *                    but have index.jsp in your welcome file list.
 *
 *  redirectWelcome   If true, welcome files are redirected rather than
 *                    forwarded to.
 *
 *  gzip              If set to true, then static content will be served as
 *                    gzip content encoded if a matching resource is
 *                    found ending with ".gz"
 *
 *  resourceBase      Set to replace the context resource base
 *
 *  resourceCache     If set, this is a context attribute name, which the servlet
 *                    will use to look for a shared ResourceCache instance.
 *
 *  relativeResourceBase
 *                    Set with a pathname relative to the base of the
 *                    servlet context root. Useful for only serving static content out
 *                    of only specific subdirectories.
 *
 *  pathInfoOnly      If true, only the path info will be applied to the resourceBase
 *
 *  stylesheet	      Set with the location of an optional stylesheet that will be used
 *                    to decorate the directory listing html.
 *
 *  etags             If True, weak etags will be generated and handled.
 *
 *  maxCacheSize      The maximum total size of the cache or 0 for no cache.
 *  maxCachedFileSize The maximum size of a file to cache
 *  maxCachedFiles    The maximum number of files to cache
 *
 *  useFileMappedBuffer
 *                    If set to true, it will use mapped file buffer to serve static content
 *                    when using NIO connector. Setting this value to false means that
 *                    a direct buffer will be used instead of a mapped file buffer.
 *                    This is set to false by default by this class, but may be overridden
 *                    by eg webdefault.xml 
 *
 *  cacheControl      If set, all static content will have this value set as the cache-control
 *                    header.
 *                    
 * otherGzipFileExtensions
 *                    Other file extensions that signify that a file is gzip compressed. Eg ".svgz"
 *
 *
 * </pre>
 *
 */
public class DefaultServlet extends HttpServlet implements ResourceFactory
{
    private static final Logger LOG = Log.getLogger(DefaultServlet.class);

    private static final long serialVersionUID = 4930458713846881193L;    

    private final ResourceService _resourceService;
    private ServletContext _servletContext;
    private ContextHandler _contextHandler;

    private boolean _welcomeServlets=false;
    private boolean _welcomeExactServlets=false;

    private Resource _resourceBase;
    private ResourceCache _cache;

    private MimeTypes _mimeTypes;
    private String[] _welcomes;
    private Resource _stylesheet;
    private boolean _useFileMappedBuffer=false;
    private String _relativeResourceBase;
    private ServletHandler _servletHandler;
    private ServletHolder _defaultHolder;

    public DefaultServlet()
    {
        _resourceService = new ResourceService()
        {
            @Override
            protected String getWelcomeFile(String pathInContext)
            {
                if (_welcomes==null)
                    return null;

                String welcome_servlet=null;
                for (int i=0;i<_welcomes.length;i++)
                {
                    String welcome_in_context=URIUtil.addPaths(pathInContext,_welcomes[i]);
                    Resource welcome=getResource(welcome_in_context);
                    if (welcome!=null && welcome.exists())
                        return _welcomes[i];

                    if ((_welcomeServlets || _welcomeExactServlets) && welcome_servlet==null)
                    {
                        MappedResource<ServletHolder> entry=_servletHandler.getHolderEntry(welcome_in_context);
                        if (entry!=null && entry.getResource()!=_defaultHolder &&
                                (_welcomeServlets || (_welcomeExactServlets && entry.getPathSpec().getDeclaration().equals(welcome_in_context))))
                            welcome_servlet=welcome_in_context;

                    }
                }
                return welcome_servlet;
            }
            
            @Override
            protected void notFound(HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        };
    }
    
    /* ------------------------------------------------------------ */
    @Override
    public void init()
    throws UnavailableException
    {
        _servletContext=getServletContext();
        _contextHandler = initContextHandler(_servletContext);

        _mimeTypes = _contextHandler.getMimeTypes();

        _welcomes = _contextHandler.getWelcomeFiles();
        if (_welcomes==null)
            _welcomes=new String[] {"index.html","index.jsp"};

        _resourceService.setAcceptRanges(getInitBoolean("acceptRanges",_resourceService.isAcceptRanges()));
        _resourceService.setDirAllowed(getInitBoolean("dirAllowed",_resourceService.isDirAllowed()));
        _resourceService.setRedirectWelcome(getInitBoolean("redirectWelcome",_resourceService.isRedirectWelcome()));
        _resourceService.setGzip(getInitBoolean("gzip",_resourceService.isGzip()));
        _resourceService.setPathInfoOnly(getInitBoolean("pathInfoOnly",_resourceService.isPathInfoOnly()));
        _resourceService.setEtags(getInitBoolean("etags",_resourceService.isEtags()));
        
        if ("exact".equals(getInitParameter("welcomeServlets")))
        {
            _welcomeExactServlets=true;
            _welcomeServlets=false;
        }
        else
            _welcomeServlets=getInitBoolean("welcomeServlets", _welcomeServlets);

        _useFileMappedBuffer=getInitBoolean("useFileMappedBuffer",_useFileMappedBuffer);

        _relativeResourceBase = getInitParameter("relativeResourceBase");

        String rb=getInitParameter("resourceBase");
        if (rb!=null)
        {
            if (_relativeResourceBase!=null)
                throw new  UnavailableException("resourceBase & relativeResourceBase");
            try{_resourceBase=_contextHandler.newResource(rb);}
            catch (Exception e)
            {
                LOG.warn(Log.EXCEPTION,e);
                throw new UnavailableException(e.toString());
            }
        }

        String css=getInitParameter("stylesheet");
        try
        {
            if(css!=null)
            {
                _stylesheet = Resource.newResource(css);
                if(!_stylesheet.exists())
                {
                    LOG.warn("!" + css);
                    _stylesheet = null;
                }
            }
            if(_stylesheet == null)
            {
                _stylesheet = Resource.newResource(this.getClass().getResource("/jetty-dir.css"));
            }
        }
        catch(Exception e)
        {
            LOG.warn(e.toString());
            LOG.debug(e);
        }

        String cc=getInitParameter("cacheControl");
        if (cc!=null)
            _resourceService.setCacheControl(new PreEncodedHttpField(HttpHeader.CACHE_CONTROL,cc));
        
        
        String resourceCache = getInitParameter("resourceCache");
        int max_cache_size=getInitInt("maxCacheSize", -2);
        int max_cached_file_size=getInitInt("maxCachedFileSize", -2);
        int max_cached_files=getInitInt("maxCachedFiles", -2);
        if (resourceCache!=null)
        {
            if (max_cache_size!=-1 || max_cached_file_size!= -2 || max_cached_files!=-2)
                LOG.debug("ignoring resource cache configuration, using resourceCache attribute");
            if (_relativeResourceBase!=null || _resourceBase!=null)
                throw new UnavailableException("resourceCache specified with resource bases");
            _cache=(ResourceCache)_servletContext.getAttribute(resourceCache);
        }

        try
        {
            if (_cache==null && (max_cached_files!=-2 || max_cache_size!=-2 || max_cached_file_size!=-2))
            {
                _cache = new ResourceCache(null,this,_mimeTypes,_useFileMappedBuffer,_resourceService.isEtags(),_resourceService.isGzip());
                if (max_cache_size>=0)
                    _cache.setMaxCacheSize(max_cache_size);
                if (max_cached_file_size>=-1)
                    _cache.setMaxCachedFileSize(max_cached_file_size);
                if (max_cached_files>=-1)
                    _cache.setMaxCachedFiles(max_cached_files);
                _servletContext.setAttribute(resourceCache==null?"resourceCache":resourceCache,_cache);
            }
        }
        catch (Exception e)
        {
            LOG.warn(Log.EXCEPTION,e);
            throw new UnavailableException(e.toString());
        }

        HttpContent.Factory contentFactory=_cache;
        if (contentFactory==null)
        {
            contentFactory=new ResourceContentFactory(this,_mimeTypes,_resourceService.isGzip());
            if (resourceCache!=null)
                _servletContext.setAttribute(resourceCache,contentFactory);
        }
        _resourceService.setContentFactory(contentFactory);
        
        List<String> gzip_equivalent_file_extensions = new ArrayList<String>();
        String otherGzipExtensions = getInitParameter("otherGzipFileExtensions");
        if (otherGzipExtensions != null)
        {
            //comma separated list
            StringTokenizer tok = new StringTokenizer(otherGzipExtensions,",",false);
            while (tok.hasMoreTokens())
            {
                String s = tok.nextToken().trim();
                gzip_equivalent_file_extensions.add((s.charAt(0)=='.'?s:"."+s));
            }
        }
        else
        {
            //.svgz files are gzipped svg files and must be served with Content-Encoding:gzip
            gzip_equivalent_file_extensions.add(".svgz");   
        }
        _resourceService.setGzipEquivalentFileExtensions(gzip_equivalent_file_extensions);

        
        _servletHandler= _contextHandler.getChildHandlerByClass(ServletHandler.class);
        for (ServletHolder h :_servletHandler.getServlets())
            if (h.getServletInstance()==this)
                _defaultHolder=h;

        if (LOG.isDebugEnabled())
            LOG.debug("resource base = "+_resourceBase);
    }

    /**
     * Compute the field _contextHandler.<br>
     * In the case where the DefaultServlet is deployed on the HttpService it is likely that
     * this method needs to be overwritten to unwrap the ServletContext facade until we reach
     * the original jetty's ContextHandler.
     * @param servletContext The servletContext of this servlet.
     * @return the jetty's ContextHandler for this servletContext.
     */
    protected ContextHandler initContextHandler(ServletContext servletContext)
    {
        ContextHandler.Context scontext=ContextHandler.getCurrentContext();
        if (scontext==null)
        {
            if (servletContext instanceof ContextHandler.Context)
                return ((ContextHandler.Context)servletContext).getContextHandler();
            else
                throw new IllegalArgumentException("The servletContext " + servletContext + " " +
                    servletContext.getClass().getName() + " is not " + ContextHandler.Context.class.getName());
        }
        else
            return ContextHandler.getCurrentContext().getContextHandler();
    }

    /* ------------------------------------------------------------ */
    @Override
    public String getInitParameter(String name)
    {
        String value=getServletContext().getInitParameter("org.eclipse.jetty.servlet.Default."+name);
        if (value==null)
            value=super.getInitParameter(name);
        return value;
    }

    /* ------------------------------------------------------------ */
    private boolean getInitBoolean(String name, boolean dft)
    {
        String value=getInitParameter(name);
        if (value==null || value.length()==0)
            return dft;
        return (value.startsWith("t")||
                value.startsWith("T")||
                value.startsWith("y")||
                value.startsWith("Y")||
                value.startsWith("1"));
    }

    /* ------------------------------------------------------------ */
    private int getInitInt(String name, int dft)
    {
        String value=getInitParameter(name);
        if (value==null)
            value=getInitParameter(name);
        if (value!=null && value.length()>0)
            return Integer.parseInt(value);
        return dft;
    }

    /* ------------------------------------------------------------ */
    /** get Resource to serve.
     * Map a path to a resource. The default implementation calls
     * HttpContext.getResource but derived servlets may provide
     * their own mapping.
     * @param pathInContext The path to find a resource for.
     * @return The resource to serve.
     */
    @Override
    public Resource getResource(String pathInContext)
    {
        Resource r=null;
        if (_relativeResourceBase!=null)
            pathInContext=URIUtil.addPaths(_relativeResourceBase,pathInContext);

        try
        {
            if (_resourceBase!=null)
            {
                r = _resourceBase.addPath(pathInContext);
                if (!_contextHandler.checkAlias(pathInContext,r))
                    r=null;
            }
            else if (_servletContext instanceof ContextHandler.Context)
            {
                r = _contextHandler.getResource(pathInContext);
            }
            else
            {
                URL u = _servletContext.getResource(pathInContext);
                r = _contextHandler.newResource(u);
            }

            if (LOG.isDebugEnabled())
                LOG.debug("Resource "+pathInContext+"="+r);
        }
        catch (IOException e)
        {
            LOG.ignore(e);
        }

        if((r==null || !r.exists()) && pathInContext.endsWith("/jetty-dir.css"))
            r=_stylesheet;

        return r;
    }

    /* ------------------------------------------------------------ */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        _resourceService.doGet(request,response);
    }

    /* ------------------------------------------------------------ */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        doGet(request,response);
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doTrace(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /* ------------------------------------------------------------ */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        resp.setHeader("Allow", "GET,HEAD,POST,OPTIONS");
    }


    /* ------------------------------------------------------------ */
    /*
     * @see javax.servlet.Servlet#destroy()
     */
    @Override
    public void destroy()
    {
        if (_cache!=null)
            _cache.flushCache();
        super.destroy();
    }

}
