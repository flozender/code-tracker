/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.aggregations.support.numeric;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 *
 */
public class ValueFormatterStreams {

    public static ValueFormatter read(StreamInput in) throws IOException {
        byte id = in.readByte();
        ValueFormatter formatter = null;
        switch (id) {
            case ValueFormatter.Raw.ID: return ValueFormatter.RAW;
            case ValueFormatter.IPv4Formatter.ID: return ValueFormatter.IPv4;
            case ValueFormatter.DateTime.ID: formatter = new ValueFormatter.DateTime(); break;
            case ValueFormatter.Number.Pattern.ID: formatter = new ValueFormatter.Number.Pattern(); break;
            default: throw new ElasticsearchIllegalArgumentException("Unknown value formatter with id [" + id + "]");
        }
        formatter.readFrom(in);
        return formatter;
    }

    public static ValueFormatter readOptional(StreamInput in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        return read(in);
    }

    public static void write(ValueFormatter formatter, StreamOutput out) throws IOException {
        out.writeByte(formatter.id());
        formatter.writeTo(out);
    }

    public static void writeOptional(ValueFormatter formatter, StreamOutput out) throws IOException {
        out.writeBoolean(formatter != null);
        if (formatter != null) {
            write(formatter, out);
        }
    }
}
