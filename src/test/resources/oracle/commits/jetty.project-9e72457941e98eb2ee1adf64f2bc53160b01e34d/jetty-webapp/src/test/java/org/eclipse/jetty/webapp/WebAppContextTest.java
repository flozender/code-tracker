// ========================================================================
// Copyright (c) 2010 Mort Bay Consulting Pty. Ltd.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.Assert;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.junit.Test;

public class WebAppContextTest
{
    @Test
    public void testConfigurationClassesFromDefault ()
    {
        Server server = new Server();
        //test if no classnames set, its the defaults
        WebAppContext wac = new WebAppContext();
        assertNull(wac.getConfigurations());
        String[] classNames = wac.getConfigurationClasses();
        assertNotNull(classNames);

        //test if no classname set, and none from server its the defaults
        wac.setServer(server);
        assertTrue(Arrays.equals(classNames, wac.getConfigurationClasses()));
    }

    @Test
    public void testConfigurationClassesExplicit ()
    {
        String[] classNames = {"x.y.z"};

        Server server = new Server();
        server.setAttribute(WebAppContext.SERVER_CONFIG, classNames);

        //test an explicitly set classnames list overrides that from the server
        WebAppContext wac = new WebAppContext();
        String[] myClassNames = {"a.b.c", "d.e.f"};
        wac.setConfigurationClasses(myClassNames);
        wac.setServer(server);
        String[] names = wac.getConfigurationClasses();
        assertTrue(Arrays.equals(myClassNames, names));


        //test if no explicit classnames, they come from the server
        WebAppContext wac2 = new WebAppContext();
        wac2.setServer(server);
        assertTrue(Arrays.equals(classNames, wac2.getConfigurationClasses()));
    }

    @Test
    public void testConfigurationInstances ()
    {
        Configuration[] configs = {new WebInfConfiguration()};
        WebAppContext wac = new WebAppContext();
        wac.setConfigurations(configs);
        assertTrue(Arrays.equals(configs, wac.getConfigurations()));

        //test that explicit config instances override any from server
        String[] classNames = {"x.y.z"};
        Server server = new Server();
        server.setAttribute(WebAppContext.SERVER_CONFIG, classNames);
        wac.setServer(server);
        assertTrue(Arrays.equals(configs,wac.getConfigurations()));
    }
    
    /**
     * tests that the servlet context white list works
     * 
     * @throws Exception
     */
    @Test 
    public void testContextWhiteList() throws Exception
    {
        Server server = new Server(0);
        HandlerList handlers = new HandlerList();
        WebAppContext contextA = new WebAppContext(".", "/A"); 
        
        contextA.addServlet( ServletA.class, "/s");
        handlers.addHandler(contextA);
        WebAppContext contextB = new WebAppContext(".", "/B");
        
        contextB.addServlet(ServletB.class, "/s");
        contextB.setContextWhiteList(new String [] { "/doesnotexist", "/B/s" } );
        handlers.addHandler(contextB);
        
        server.setHandler(handlers);
        server.start();
        
        // context A should be able to get both A and B servlet contexts
        Assert.assertNotNull(contextA.getServletHandler().getServletContext().getContext("/A/s"));
        Assert.assertNotNull(contextA.getServletHandler().getServletContext().getContext("/B/s"));

        // context B has a contextWhiteList set and should only be able to get ones that are approved
        Assert.assertNull(contextB.getServletHandler().getServletContext().getContext("/A/s"));
        Assert.assertNotNull(contextB.getServletHandler().getServletContext().getContext("/B/s"));
    }
    
    class ServletA extends GenericServlet
    {
        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
        {
            this.getServletContext().getContext("/A/s");
        }      
    }
    
    class ServletB extends GenericServlet
    {
        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
        {
            this.getServletContext().getContext("/B/s");
        }      
    }
}
