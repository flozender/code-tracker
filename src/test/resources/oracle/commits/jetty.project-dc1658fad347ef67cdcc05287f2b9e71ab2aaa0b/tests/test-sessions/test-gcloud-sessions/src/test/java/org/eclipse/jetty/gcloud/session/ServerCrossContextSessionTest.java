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


package org.eclipse.jetty.gcloud.session;

import org.eclipse.jetty.server.session.AbstractServerCrossContextSessionTest;
import org.eclipse.jetty.server.session.AbstractTestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * ServerCrossContextSessionTest
 *
 *
 */
public class ServerCrossContextSessionTest extends AbstractServerCrossContextSessionTest
{
    @AfterClass
    public static void teardown () throws Exception
    {
        GCloudTestSuite.__testSupport.deleteSessions();
    }
    /** 
     * @see org.eclipse.jetty.server.session.AbstractServerCrossContextSessionTest#createServer(int)
     */
    @Override
    public AbstractTestServer createServer(int port)
    {
        return  new GCloudTestServer(port); 
    }

    @Test
    @Override
    public void testCrossContextDispatch() throws Exception
    {
        super.testCrossContextDispatch();
    }

    
}
