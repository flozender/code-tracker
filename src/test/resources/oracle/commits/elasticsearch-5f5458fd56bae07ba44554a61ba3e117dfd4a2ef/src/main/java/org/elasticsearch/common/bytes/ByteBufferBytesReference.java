/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.bytes;

import org.elasticsearch.common.Bytes;
import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 */
public class ByteBufferBytesReference implements BytesReference {

    private final ByteBuffer buffer;

    public ByteBufferBytesReference(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public byte get(int index) {
        return buffer.get(buffer.position() + index);
    }

    @Override
    public int length() {
        return buffer.remaining();
    }

    @Override
    public BytesReference slice(int from, int length) {
        ByteBuffer dup = buffer.duplicate();
        dup.position(buffer.position() + from);
        dup.limit(buffer.position() + from + length);
        return new ByteBufferBytesReference(dup);
    }

    @Override
    public StreamInput streamInput() {
        return new ByteBufferStreamInput(buffer);
    }

    @Override
    public void writeTo(StreamOutput out, boolean withLength) throws IOException {
        if (withLength) {
            out.writeVInt(length());
        }
        writeTo(out);
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        if (buffer.hasArray()) {
            os.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        } else {
            byte[] tmp = new byte[8192];
            ByteBuffer buf = buffer.duplicate();
            while (buf.hasRemaining()) {
                buf.get(tmp, 0, Math.min(tmp.length, buf.remaining()));
                os.write(tmp);
            }
        }
    }

    @Override
    public byte[] toBytes() {
        if (!buffer.hasRemaining()) {
            return Bytes.EMPTY_ARRAY;
        }
        byte[] tmp = new byte[buffer.remaining()];
        buffer.duplicate().get(tmp);
        return tmp;
    }

    @Override
    public BytesArray toBytesArray() {
        if (buffer.hasArray()) {
            return new BytesArray(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        }
        return new BytesArray(toBytes());
    }

    @Override
    public BytesArray copyBytesArray() {
        return new BytesArray(toBytes());
    }

    @Override
    public boolean hasArray() {
        return buffer.hasArray();
    }

    @Override
    public byte[] array() {
        return buffer.array();
    }

    @Override
    public int arrayOffset() {
        return buffer.arrayOffset() + buffer.position();
    }
}
