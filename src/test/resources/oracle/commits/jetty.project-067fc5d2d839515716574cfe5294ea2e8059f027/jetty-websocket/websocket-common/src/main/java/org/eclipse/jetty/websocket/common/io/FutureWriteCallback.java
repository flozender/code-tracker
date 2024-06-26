//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.websocket.common.io;

import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.WriteCallback;

/**
 * Allows events to a {@link WriteCallback} to drive a {@link Future} for the user.
 */
public class FutureWriteCallback extends FutureCallback implements WriteCallback
{
    private static final Logger LOG = Log.getLogger(FutureWriteCallback.class);

    @Override
    public void writeFailed(Throwable cause)
    {
        if (LOG.isDebugEnabled())
            LOG.debug(".writeFailed",cause);
        failed(cause);
    }

    @Override
    public void writeSuccess()
    {
        if (LOG.isDebugEnabled())
            LOG.debug(".writeSuccess");
        succeeded();
    }
}
