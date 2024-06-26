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

package org.eclipse.jetty.http2.frames;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.http2.generator.Generator;
import org.eclipse.jetty.http2.parser.Parser;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.junit.Assert;
import org.junit.Test;

public class PriorityGenerateParseTest
{
    private final ByteBufferPool byteBufferPool = new MappedByteBufferPool();

    @Test
    public void testGenerateParse() throws Exception
    {
        Generator generator = new Generator(byteBufferPool);

        int streamId = 13;
        int dependentStreamId = 17;
        int weight = 3;
        boolean exclusive = true;

        // Iterate a few times to be sure generator and parser are properly reset.
        final List<PriorityFrame> frames = new ArrayList<>();
        for (int i = 0; i < 2; ++i)
        {
            ByteBufferPool.Lease lease = generator.generatePriority(streamId, dependentStreamId, weight, exclusive);
            Parser parser = new Parser(byteBufferPool, new Parser.Listener.Adapter()
            {
                @Override
                public boolean onPriority(PriorityFrame frame)
                {
                    frames.add(frame);
                    return false;
                }
            });

            frames.clear();
            for (ByteBuffer buffer : lease.getByteBuffers())
            {
                while (buffer.hasRemaining())
                {
                    parser.parse(buffer);
                }
            }
        }

        Assert.assertEquals(1, frames.size());
        PriorityFrame frame = frames.get(0);
        Assert.assertEquals(streamId, frame.getStreamId());
        Assert.assertEquals(dependentStreamId, frame.getDependentStreamId());
        Assert.assertEquals(weight, frame.getWeight());
        Assert.assertEquals(exclusive, frame.isExclusive());
    }

    @Test
    public void testGenerateParseOneByteAtATime() throws Exception
    {
        Generator generator = new Generator(byteBufferPool);

        int streamId = 13;
        int dependentStreamId = 17;
        int weight = 3;
        boolean exclusive = true;

        final List<PriorityFrame> frames = new ArrayList<>();
        ByteBufferPool.Lease lease = generator.generatePriority(streamId, dependentStreamId, weight, exclusive);
        Parser parser = new Parser(byteBufferPool, new Parser.Listener.Adapter()
        {
            @Override
            public boolean onPriority(PriorityFrame frame)
            {
                frames.add(frame);
                return false;
            }
        });

        for (ByteBuffer buffer : lease.getByteBuffers())
        {
            while (buffer.hasRemaining())
            {
                parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
            }
        }

        Assert.assertEquals(1, frames.size());
        PriorityFrame frame = frames.get(0);
        Assert.assertEquals(streamId, frame.getStreamId());
        Assert.assertEquals(dependentStreamId, frame.getDependentStreamId());
        Assert.assertEquals(weight, frame.getWeight());
        Assert.assertEquals(exclusive, frame.isExclusive());
    }
}
