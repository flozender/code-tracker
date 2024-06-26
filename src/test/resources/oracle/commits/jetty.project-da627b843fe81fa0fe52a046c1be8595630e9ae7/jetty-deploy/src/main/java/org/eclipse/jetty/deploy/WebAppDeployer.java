// ========================================================================
// Copyright (c) 2006-2009 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.deploy;

import java.util.ArrayList;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Web Application Deployer.
 * 
 * The class searches a directory for and deploys standard web application.
 * At startup, the directory specified by {@link #setWebAppDir(String)} is searched 
 * for subdirectories (excluding hidden and CVS) or files ending with ".zip"
 * or "*.war".  For each webapp discovered is passed to a new instance
 * of {@link WebAppContext} (or a subclass specified by {@link #getContexts()}.
 * {@link ContextHandlerCollection#getContextClass()}
 * 
 * This deployer does not do hot deployment or undeployment. Nor does
 * it support per webapplication configuration. For these features 
 * see {@link ContextDeployer}.
 * 
 * @see {@link ContextDeployer}
 */
public class WebAppDeployer extends AbstractLifeCycle
{
    private HandlerContainer _contexts;
    private String _webAppDir;
    private String _defaultsDescriptor;
    private String[] _configurationClasses;
    private boolean _extract;
    private boolean _parentLoaderPriority;
    private boolean _allowDuplicates;
    private ArrayList _deployed;

    public String[] getConfigurationClasses()
    {
        return _configurationClasses;
    }

    public void setConfigurationClasses(String[] configurationClasses)
    {
        _configurationClasses=configurationClasses;
    }

    public HandlerContainer getContexts()
    {
        return _contexts;
    }

    public void setContexts(HandlerContainer contexts)
    {
        _contexts=contexts;
    }

    public String getDefaultsDescriptor()
    {
        return _defaultsDescriptor;
    }

    public void setDefaultsDescriptor(String defaultsDescriptor)
    {
        _defaultsDescriptor=defaultsDescriptor;
    }

    public boolean isExtract()
    {
        return _extract;
    }

    public void setExtract(boolean extract)
    {
        _extract=extract;
    }

    public boolean isParentLoaderPriority()
    {
        return _parentLoaderPriority;
    }

    public void setParentLoaderPriority(boolean parentPriorityClassLoading)
    {
        _parentLoaderPriority=parentPriorityClassLoading;
    }

    public String getWebAppDir()
    {
        return _webAppDir;
    }

    public void setWebAppDir(String dir)
    {
        _webAppDir=dir;
    }

    public boolean getAllowDuplicates()
    {
        return _allowDuplicates;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param allowDuplicates If false, do not deploy webapps that have already been deployed or duplicate context path
     */
    public void setAllowDuplicates(boolean allowDuplicates)
    {
        _allowDuplicates=allowDuplicates;
    }

    /* ------------------------------------------------------------ */
    /**
     * @throws Exception 
     */
    public void doStart() throws Exception
    {
        _deployed=new ArrayList();
        
        scan();
        
    }
    
    /* ------------------------------------------------------------ */
    /** Scan for webapplications.
     * 
     * @throws Exception
     */
    public void scan() throws Exception
    {
        if (_contexts==null)
            throw new IllegalArgumentException("No HandlerContainer");

        Resource r=Resource.newResource(_webAppDir);
        if (!r.exists())
            throw new IllegalArgumentException("No such webapps resource "+r);

        if (!r.isDirectory())
            throw new IllegalArgumentException("Not directory webapps resource "+r);

        String[] files=r.list();

        files: for (int f=0; files!=null&&f<files.length; f++)
        {
            String context=files[f];

            if (context.equalsIgnoreCase("CVS/")||context.equalsIgnoreCase("CVS")||context.startsWith("."))
                continue;

            Resource app=r.addPath(r.encode(context));

            if (context.toLowerCase().endsWith(".war")||context.toLowerCase().endsWith(".jar"))
            {
                context=context.substring(0,context.length()-4);
                Resource unpacked=r.addPath(context);
                if (unpacked!=null&&unpacked.exists()&&unpacked.isDirectory())
                    continue;
            }
            else if (!app.isDirectory())
                continue;

            if (context.equalsIgnoreCase("root")||context.equalsIgnoreCase("root/"))
                context=URIUtil.SLASH;
            else
                context="/"+context;
            if (context.endsWith("/")&&context.length()>0)
                context=context.substring(0,context.length()-1);

            // Check the context path has not already been added or the webapp itself is not already deployed
            if (!_allowDuplicates)
            {
                Handler[] installed=_contexts.getChildHandlersByClass(ContextHandler.class);
                for (int i=0; i<installed.length; i++)
                {
                    ContextHandler c=(ContextHandler)installed[i];
        
                    if (context.equals(c.getContextPath()))
                        continue files;
                    
                   String path;
                   if (c instanceof WebAppContext)
                       path = ((WebAppContext)c).getWar();
                   else
                       path = (c.getBaseResource()==null?"":c.getBaseResource().getFile().getAbsolutePath());

                    if (path.equals(app.getFile().getAbsolutePath()))
                        continue files;
   
                }
            }

            // create a webapp
            WebAppContext wah=null;
            if (_contexts instanceof ContextHandlerCollection && 
                WebAppContext.class.isAssignableFrom(((ContextHandlerCollection)_contexts).getContextClass()))
            {
                try
                {
                    wah=(WebAppContext)((ContextHandlerCollection)_contexts).getContextClass().newInstance();
                }
                catch (Exception e)
                {
                    throw new Error(e);
                }
            }
            else
            {
                wah=new WebAppContext();
            }
            
            // configure it
            wah.setContextPath(context);
            if (_configurationClasses!=null)
                wah.setConfigurationClasses(_configurationClasses);
            if (_defaultsDescriptor!=null)
                wah.setDefaultsDescriptor(_defaultsDescriptor);
            wah.setExtractWAR(_extract);
            wah.setWar(app.toString());
            wah.setParentLoaderPriority(_parentLoaderPriority);
            // add it
            _contexts.addHandler(wah);
            _deployed.add(wah);
            
            if (_contexts.isStarted())
                _contexts.start();  // TODO Multi exception
        }
    }
    
    public void doStop() throws Exception
    {
        for (int i=_deployed.size();i-->0;)
        {
            ContextHandler wac = (ContextHandler)_deployed.get(i);
            wac.stop();// TODO Multi exception
        }
    }
}
