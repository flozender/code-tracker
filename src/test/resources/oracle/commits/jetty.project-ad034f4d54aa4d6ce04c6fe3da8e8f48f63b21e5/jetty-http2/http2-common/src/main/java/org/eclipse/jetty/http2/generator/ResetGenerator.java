//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.http2.generator;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http2.frames.Flag;
import org.eclipse.jetty.http2.frames.Frame;
import org.eclipse.jetty.http2.frames.FrameType;
import org.eclipse.jetty.http2.frames.ResetFrame;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;

public class ResetGenerator extends FrameGenerator
{
    public ResetGenerator(HeaderGenerator headerGenerator)
    {
        super(headerGenerator);
    }

    @Override
    public void generate(ByteBufferPool.Lease lease, Frame frame, Callback callback)
    {
        ResetFrame resetFrame = (ResetFrame)frame;
        generateReset(lease, resetFrame.getStreamId(), resetFrame.getError());
    }

    public void generateReset(ByteBufferPool.Lease lease, int streamId, int error)
    {
        if (streamId < 0)
            throw new IllegalArgumentException("Invalid stream id: " + streamId);

        ByteBuffer header = generateHeader(lease, FrameType.RST_STREAM, 4, Flag.NONE, streamId);

        header.putInt(error);

        BufferUtil.flipToFlush(header, 0);
        lease.append(header, true);
    }
}
