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

package org.eclipse.jetty.server;

public class ChannelHttpServer
{
    public static void main(String[] s) throws Exception
    {
        Server server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);
        server.setHandler(new DumpHandler());
        server.start();
        server.dumpStdErr();
        server.join();
    }
}
