//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.osgi.boot;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.bindings.StandardDeployer;
import org.eclipse.jetty.deploy.graph.Node;
import org.eclipse.jetty.osgi.boot.utils.EventSender;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;


/**
 * OSGiDeployer
 *
 *
 */
public class OSGiDeployer extends StandardDeployer
{
    
    /* ------------------------------------------------------------ */
    public void processBinding(Node node, App app) throws Exception
    {
        //TODO  how to NOT send this event if its not a webapp!
        EventSender.getInstance().send(EventSender.DEPLOYING_EVENT, ((AbstractOSGiApp)app).getBundle(), app.getContextPath());
        try
        {
            super.processBinding(node,app);
            ((AbstractOSGiApp)app).registerAsOSGiService();
            EventSender.getInstance().send(EventSender.DEPLOYED_EVENT, ((AbstractOSGiApp)app).getBundle(), app.getContextPath());
        }
        catch (Exception e)
        {
            EventSender.getInstance().send(EventSender.FAILED_EVENT, ((AbstractOSGiApp)app).getBundle(), app.getContextPath()); 
            throw e;
        }
    }
}
