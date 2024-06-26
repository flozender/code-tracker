// ========================================================================
// Copyright (c) 2009 Mort Bay Consulting Pty. Ltd.
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

import java.util.List;

import org.eclipse.jetty.util.resource.Resource;

/**
 * FragmentConfiguration
 * 
 * 
 * 
 * Process web-fragments in jars
 */
public class FragmentConfiguration implements Configuration
{
    public final static String FRAGMENT_RESOURCES="org.eclipse.jetty.webFragments";
    
    public void preConfigure(WebAppContext context) throws Exception
    {
        if (!context.isConfigurationDiscovered())
            return;

        MetaData metaData = (MetaData)context.getAttribute(MetaData.METADATA); 
        if (metaData == null)
            throw new IllegalStateException("No metadata");

        //find all web-fragment.xmls
        findWebFragments(context, metaData);
        
    }
    
    public void configure(WebAppContext context) throws Exception
    { 
        if (!context.isConfigurationDiscovered())
            return;
        
        MetaData metaData = (MetaData)context.getAttribute(MetaData.METADATA); 
        if (metaData == null)
            throw new IllegalStateException("No metadata");
        
        //order the fragments
        metaData.orderFragments(); 
    }

    public void deconfigure(WebAppContext context) throws Exception
    {
       
    }

    public void postConfigure(WebAppContext context) throws Exception
    {
        context.setAttribute(FRAGMENT_RESOURCES, null);
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Look for any web-fragment.xml fragments in META-INF of jars in WEB-INF/lib
     * 
     * @throws Exception
     */
    public void findWebFragments (final WebAppContext context, final MetaData metaData) throws Exception
    {
        List<Resource> frags = (List<Resource>)context.getAttribute(FRAGMENT_RESOURCES);
        if (frags!=null)
        {
            for (Resource frag : frags)
            {
                metaData.addFragment(frag, Resource.newResource("jar:"+frag.getURL()+"!/META-INF/web-fragment.xml"));
            }
        }
    }
}
