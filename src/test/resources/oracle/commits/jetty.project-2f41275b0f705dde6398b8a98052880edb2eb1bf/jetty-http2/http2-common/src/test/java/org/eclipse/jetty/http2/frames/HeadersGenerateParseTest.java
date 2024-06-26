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

import org.eclipse.jetty.http.FinalMetaData;
import org.eclipse.jetty.http.HostPortHttpField;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.generator.HeaderGenerator;
import org.eclipse.jetty.http2.generator.HeadersGenerator;
import org.eclipse.jetty.http2.hpack.HpackEncoder;
import org.eclipse.jetty.http2.parser.Parser;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.junit.Assert;
import org.junit.Test;

public class HeadersGenerateParseTest
{
    private final ByteBufferPool byteBufferPool = new MappedByteBufferPool();

    @Test
    public void testGenerateParse() throws Exception
    {
        HeadersGenerator generator = new HeadersGenerator(new HeaderGenerator(), new HpackEncoder());

        int streamId = 13;
        HttpFields fields = new HttpFields();
        fields.put("Accept", "text/html");
        fields.put("User-Agent", "Jetty");
        MetaData.Request metaData = new FinalMetaData.Request(HttpVersion.HTTP_2, HttpScheme.HTTP, "GET", new HostPortHttpField("localhost:8080"), "/path", fields);

        final List<HeadersFrame> frames = new ArrayList<>();
        Parser parser = new Parser(byteBufferPool, new Parser.Listener.Adapter()
        {
            @Override
            public boolean onHeaders(HeadersFrame frame)
            {
                frames.add(frame);
                return false;
            }
        }, 4096, 8192);

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i)
        {
            ByteBufferPool.Lease lease = new ByteBufferPool.Lease(byteBufferPool);
            generator.generateHeaders(lease, streamId, metaData, false);

            frames.clear();
            for (ByteBuffer buffer : lease.getByteBuffers())
            {
                while (buffer.hasRemaining())
                {
                    parser.parse(buffer);
                }
            }

            Assert.assertEquals(1, frames.size());
            HeadersFrame frame = frames.get(0);
            Assert.assertEquals(streamId, frame.getStreamId());
            Assert.assertTrue(frame.isEndStream());
            MetaData.Request request = (MetaData.Request)frame.getMetaData();
            Assert.assertSame(metaData.getScheme(), request.getScheme());
            Assert.assertEquals(metaData.getMethod(), request.getMethod());
            Assert.assertEquals(metaData.getHost(), request.getHost());
            Assert.assertEquals(metaData.getPort(), request.getPort());
            Assert.assertEquals(metaData.getURI(), request.getURI());
            for (int j = 0; j < fields.size(); ++j)
            {
                HttpField field = fields.getField(j);
                Assert.assertTrue(request.getFields().contains(field));
            }
        }
    }

    @Test
    public void testGenerateParseOneByteAtATime() throws Exception
    {
        HeadersGenerator generator = new HeadersGenerator(new HeaderGenerator(), new HpackEncoder());

        final List<HeadersFrame> frames = new ArrayList<>();
        Parser parser = new Parser(byteBufferPool, new Parser.Listener.Adapter()
        {
            @Override
            public boolean onHeaders(HeadersFrame frame)
            {
                frames.add(frame);
                return false;
            }
        }, 4096, 8192);

        int streamId = 13;
        HttpFields fields = new HttpFields();
        fields.put("Accept", "text/html");
        fields.put("User-Agent", "Jetty");
        MetaData.Request metaData = new FinalMetaData.Request(HttpVersion.HTTP_2, HttpScheme.HTTP, "GET", new HostPortHttpField("localhost:8080"), "/path", fields);

        ByteBufferPool.Lease lease = new ByteBufferPool.Lease(byteBufferPool);
        generator.generateHeaders(lease, streamId, metaData, false);

        for (ByteBuffer buffer : lease.getByteBuffers())
        {
            while (buffer.hasRemaining())
            {
                parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
            }
        }

        Assert.assertEquals(1, frames.size());
        HeadersFrame frame = frames.get(0);
        Assert.assertEquals(streamId, frame.getStreamId());
        Assert.assertTrue(frame.isEndStream());
        MetaData.Request request = (MetaData.Request)frame.getMetaData();
        Assert.assertSame(metaData.getScheme(), request.getScheme());
        Assert.assertEquals(metaData.getMethod(), request.getMethod());
        Assert.assertEquals(metaData.getHost(), request.getHost());
        Assert.assertEquals(metaData.getPort(), request.getPort());
        Assert.assertEquals(metaData.getURI(), request.getURI());
        for (int j = 0; j < fields.size(); ++j)
        {
            HttpField field = fields.getField(j);
            Assert.assertTrue(request.getFields().contains(field));
        }
    }
}
