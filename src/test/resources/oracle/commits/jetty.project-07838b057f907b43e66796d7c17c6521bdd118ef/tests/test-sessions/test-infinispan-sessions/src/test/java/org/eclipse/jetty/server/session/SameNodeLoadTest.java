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


package org.eclipse.jetty.server.session;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * SameNodeLoadTest
 *
 *
 */
public class SameNodeLoadTest extends AbstractSameNodeLoadTest
{

    public static InfinispanTestSupport __testSupport;

    
    
    @BeforeClass
    public static void setup () throws Exception
    {
        __testSupport = new InfinispanTestSupport();
        __testSupport.setup();
    }
    
    @AfterClass
    public static void teardown () throws Exception
    {
       __testSupport.teardown();
    }
    
    /** 
     * @see org.eclipse.jetty.server.session.AbstractSameNodeLoadTest#createServer(int)
     */
    @Override
    public AbstractTestServer createServer(int port)
    {
        InfinispanTestSessionServer server = new InfinispanTestSessionServer(port, __testSupport.getCache());
        return server;
    }

    @Override
    public void testLoad() throws Exception
    {
        super.testLoad();
    }

}
