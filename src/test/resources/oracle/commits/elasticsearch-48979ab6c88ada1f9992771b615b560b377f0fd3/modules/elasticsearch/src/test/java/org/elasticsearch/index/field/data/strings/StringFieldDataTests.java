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

package org.elasticsearch.index.field.data.strings;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.elasticsearch.index.field.data.FieldData;
import org.elasticsearch.util.Tuple;
import org.elasticsearch.util.lucene.Lucene;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.elasticsearch.index.field.data.FieldDataOptions.*;
import static org.elasticsearch.util.Tuple.*;
import static org.elasticsearch.util.lucene.DocumentBuilder.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * @author kimchy (Shay Banon)
 */
public class StringFieldDataTests {

    @Test public void stringFieldDataTests() throws Exception {
        Directory dir = new RAMDirectory();
        IndexWriter indexWriter = new IndexWriter(dir, Lucene.STANDARD_ANALYZER, true, IndexWriter.MaxFieldLength.UNLIMITED);

        indexWriter.addDocument(doc()
                .add(field("svalue", "zzz"))
                .add(field("mvalue", "111")).build());

        indexWriter.addDocument(doc()
                .add(field("svalue", "xxx"))
                .add(field("mvalue", "222 333")).build());

        indexWriter.addDocument(doc()
                .add(field("mvalue", "333 444")).build());

        indexWriter.addDocument(doc()
                .add(field("svalue", "aaa")).build());

        indexWriter.addDocument(doc()
                .add(field("svalue", "aaa")).build());

        IndexReader reader = indexWriter.getReader();

        StringFieldData.load(reader, "svalue", fieldDataOptions().withFreqs(false));
        StringFieldData.load(reader, "mvalue", fieldDataOptions().withFreqs(false));

        StringFieldData sFieldData = StringFieldData.load(reader, "svalue", fieldDataOptions().withFreqs(true));
        StringFieldData mFieldData = StringFieldData.load(reader, "mvalue", fieldDataOptions().withFreqs(true));

        assertThat(sFieldData.fieldName(), equalTo("svalue"));
        assertThat(sFieldData.type(), equalTo(FieldData.Type.STRING));
        assertThat(sFieldData.multiValued(), equalTo(false));

        assertThat(mFieldData.fieldName(), equalTo("mvalue"));
        assertThat(mFieldData.type(), equalTo(FieldData.Type.STRING));
        assertThat(mFieldData.multiValued(), equalTo(true));

        // svalue
        assertThat(sFieldData.hasValue(0), equalTo(true));
        assertThat(sFieldData.value(0), equalTo("zzz"));
        assertThat(sFieldData.docFieldData(0).getValue(), equalTo("zzz"));
        assertThat(sFieldData.values(0).length, equalTo(1));
        assertThat(sFieldData.docFieldData(0).getValues().length, equalTo(1));
        assertThat(sFieldData.values(0)[0], equalTo("zzz"));
        assertThat(sFieldData.docFieldData(0).getValues()[0], equalTo("zzz"));

        assertThat(sFieldData.hasValue(1), equalTo(true));
        assertThat(sFieldData.value(1), equalTo("xxx"));
        assertThat(sFieldData.values(1).length, equalTo(1));
        assertThat(sFieldData.values(1)[0], equalTo("xxx"));

        assertThat(sFieldData.hasValue(2), equalTo(false));

        assertThat(sFieldData.hasValue(3), equalTo(true));
        assertThat(sFieldData.value(3), equalTo("aaa"));
        assertThat(sFieldData.values(3).length, equalTo(1));
        assertThat(sFieldData.values(3)[0], equalTo("aaa"));

        assertThat(sFieldData.hasValue(4), equalTo(true));
        assertThat(sFieldData.value(4), equalTo("aaa"));
        assertThat(sFieldData.values(4).length, equalTo(1));
        assertThat(sFieldData.values(4)[0], equalTo("aaa"));

        // check order is correct
        final ArrayList<Tuple<String, Integer>> values = new ArrayList<Tuple<String, Integer>>();
        sFieldData.forEachValue(new FieldData.StringValueProc() {
            @Override public void onValue(String value, int freq) {
                values.add(tuple(value, freq));
            }
        });
        assertThat(values.size(), equalTo(3));

        assertThat(values.get(0).v1(), equalTo("aaa"));
        assertThat(values.get(0).v2(), equalTo(2));

        assertThat(values.get(1).v1(), equalTo("xxx"));
        assertThat(values.get(1).v2(), equalTo(1));

        assertThat(values.get(2).v1(), equalTo("zzz"));
        assertThat(values.get(2).v2(), equalTo(1));

        // mvalue
        assertThat(mFieldData.hasValue(0), equalTo(true));
        assertThat(mFieldData.value(0), equalTo("111"));
        assertThat(mFieldData.values(0).length, equalTo(1));
        assertThat(mFieldData.values(0)[0], equalTo("111"));

        assertThat(mFieldData.hasValue(1), equalTo(true));
        assertThat(mFieldData.value(1), equalTo("222"));
        assertThat(mFieldData.values(1).length, equalTo(2));
        assertThat(mFieldData.values(1)[0], equalTo("222"));
        assertThat(mFieldData.values(1)[1], equalTo("333"));

        assertThat(mFieldData.hasValue(2), equalTo(true));
        assertThat(mFieldData.value(2), equalTo("333"));
        assertThat(mFieldData.values(2).length, equalTo(2));
        assertThat(mFieldData.values(2)[0], equalTo("333"));
        assertThat(mFieldData.values(2)[1], equalTo("444"));

        assertThat(mFieldData.hasValue(3), equalTo(false));

        assertThat(mFieldData.hasValue(4), equalTo(false));

        values.clear();
        mFieldData.forEachValue(new FieldData.StringValueProc() {
            @Override public void onValue(String value, int freq) {
                values.add(tuple(value, freq));
            }
        });
        assertThat(values.size(), equalTo(4));

        assertThat(values.get(0).v1(), equalTo("111"));
        assertThat(values.get(0).v2(), equalTo(1));

        assertThat(values.get(1).v1(), equalTo("222"));
        assertThat(values.get(1).v2(), equalTo(1));

        assertThat(values.get(2).v1(), equalTo("333"));
        assertThat(values.get(2).v2(), equalTo(2));

        assertThat(values.get(3).v1(), equalTo("444"));
        assertThat(values.get(3).v2(), equalTo(1));

        indexWriter.close();
    }
}
