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
import org.eclipse.jetty.http2.frames.PushPromiseFrame;
import org.eclipse.jetty.http2.hpack.HpackEncoder;
import org.eclipse.jetty.http2.hpack.MetaData;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;

public class PushPromiseGenerator extends FrameGenerator
{
    private final HpackEncoder encoder;

    public PushPromiseGenerator(HeaderGenerator headerGenerator, HpackEncoder encoder)
    {
        super(headerGenerator);
        this.encoder = encoder;
    }

    @Override
    public void generate(ByteBufferPool.Lease lease, Frame frame)
    {
        PushPromiseFrame pushPromiseFrame = (PushPromiseFrame)frame;
        generatePushPromise(lease, pushPromiseFrame.getStreamId(), pushPromiseFrame.getPromisedStreamId(), pushPromiseFrame.getMetaData());
    }

    public void generatePushPromise(ByteBufferPool.Lease lease, int streamId, int promisedStreamId, MetaData metaData)
    {
        if (streamId < 0)
            throw new IllegalArgumentException("Invalid stream id: " + streamId);
        if (promisedStreamId < 0)
            throw new IllegalArgumentException("Invalid promised stream id: " + promisedStreamId);

        encoder.encode(metaData, lease);

        long length = lease.getTotalLength();
        if (length > Frame.MAX_LENGTH)
            throw new IllegalArgumentException("Invalid headers, too big");

        // Space for the promised streamId.
        length += 4;

        int flags = Flag.END_HEADERS;

        ByteBuffer header = generateHeader(lease, FrameType.PUSH_PROMISE, (int)length, flags, streamId);
        header.putInt(promisedStreamId);

        BufferUtil.flipToFlush(header, 0);
        lease.prepend(header, true);
    }
}
