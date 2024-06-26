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

package org.eclipse.jetty.websocket.jsr356;

import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.eclipse.jetty.websocket.jsr356.metadata.EncoderMetadata;

/**
 * Expose a configured {@link Encoder} instance along with its associated {@link EncoderMetadata}
 */
public class EncoderWrapper implements Configurable
{
    private final Encoder encoder;
    private final EncoderMetadata metadata;

    public EncoderWrapper(Encoder encoder, EncoderMetadata metadata)
    {
        this.encoder = encoder;
        this.metadata = metadata;
    }

    public Encoder getEncoder()
    {
        return encoder;
    }

    public EncoderMetadata getMetadata()
    {
        return metadata;
    }

    @Override
    public void init(EndpointConfig config)
    {
        this.encoder.init(config);
    }
}
