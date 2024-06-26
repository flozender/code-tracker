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

package org.eclipse.jetty.websocket.jsr356.endpoints;

import javax.websocket.EndpointConfig;

/**
 * Associate a JSR Endpoint with its optional {@link EndpointConfig}
 */
public class ConfiguredEndpoint
{
    private Object endpoint;
    private EndpointConfig config;

    public ConfiguredEndpoint(Object endpoint, EndpointConfig config)
    {
        this.endpoint = endpoint;
        this.config = config;
    }

    public EndpointConfig getConfig()
    {
        return config;
    }

    public Object getEndpoint()
    {
        return endpoint;
    }
}
