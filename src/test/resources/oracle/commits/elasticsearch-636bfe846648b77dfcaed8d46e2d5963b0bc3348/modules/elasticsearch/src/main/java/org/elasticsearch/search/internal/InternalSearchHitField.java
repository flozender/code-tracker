/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
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

package org.elasticsearch.search.internal;

import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.util.io.stream.StreamInput;
import org.elasticsearch.util.io.stream.StreamOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author kimchy (Shay Banon)
 */
public class InternalSearchHitField implements SearchHitField {

    private String name;

    private List<Object> values;

    private InternalSearchHitField() {

    }

    public InternalSearchHitField(String name, List<Object> values) {
        this.name = name;
        this.values = values;
    }

    public String name() {
        return name;
    }

    @Override public String getName() {
        return name();
    }

    public List<Object> values() {
        return values;
    }

    @Override public List<Object> getValues() {
        return values();
    }


    @Override public Iterator<Object> iterator() {
        return values.iterator();
    }

    public static InternalSearchHitField readSearchHitField(StreamInput in) throws IOException {
        InternalSearchHitField result = new InternalSearchHitField();
        result.readFrom(in);
        return result;
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        name = in.readUTF();
        int size = in.readVInt();
        values = new ArrayList<Object>(size);
        for (int i = 0; i < size; i++) {
            values.add(Lucene.readFieldValue(in));
        }
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        out.writeUTF(name);
        out.writeVInt(values.size());
        for (Object value : values) {
            Lucene.writeFieldValue(out, value);
        }
    }
}