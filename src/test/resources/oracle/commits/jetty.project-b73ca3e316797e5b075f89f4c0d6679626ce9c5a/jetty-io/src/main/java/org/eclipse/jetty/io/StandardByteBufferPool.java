/*
 * Copyright (c) 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.jetty.io;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jetty.util.BufferUtil;

public class StandardByteBufferPool implements ByteBufferPool
{
    private final ConcurrentMap<Integer, Queue<ByteBuffer>> directBuffers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Queue<ByteBuffer>> heapBuffers = new ConcurrentHashMap<>();
    private final int factor;

    public StandardByteBufferPool()
    {
        this(1024);
    }

    public StandardByteBufferPool(int factor)
    {
        this.factor = factor;
    }

    public ByteBuffer acquire(int size, boolean direct)
    {
        int bucket = bucketFor(size);
        ConcurrentMap<Integer, Queue<ByteBuffer>> buffers = buffersFor(direct);

        ByteBuffer result = null;
        Queue<ByteBuffer> byteBuffers = buffers.get(bucket);
        if (byteBuffers != null)
            result = byteBuffers.poll();

        if (result == null)
        {
            int capacity = bucket * factor;
            result = direct ? BufferUtil.allocateDirect(capacity) : BufferUtil.allocate(capacity);
        }
        else
            BufferUtil.clear(result);

        return result;
    }

    public void release(ByteBuffer buffer)
    {
        int bucket = bucketFor(buffer.capacity());
        ConcurrentMap<Integer, Queue<ByteBuffer>> buffers = buffersFor(buffer.isDirect());

        // Avoid to create a new queue every time, just to be discarded immediately
        Queue<ByteBuffer> byteBuffers = buffers.get(bucket);
        if (byteBuffers == null)
        {
            byteBuffers = new ConcurrentLinkedQueue<>();
            Queue<ByteBuffer> existing = buffers.putIfAbsent(bucket, byteBuffers);
            if (existing != null)
                byteBuffers = existing;
        }

        buffer.clear();
        byteBuffers.offer(buffer);
    }

    public void clear()
    {
        directBuffers.clear();
        heapBuffers.clear();
    }

    private int bucketFor(int size)
    {
        int bucket = size / factor;
        if (size % factor > 0)
            ++bucket;
        return bucket;
    }

    private ConcurrentMap<Integer, Queue<ByteBuffer>> buffersFor(boolean direct)
    {
        return direct ? directBuffers : heapBuffers;
    }
}
