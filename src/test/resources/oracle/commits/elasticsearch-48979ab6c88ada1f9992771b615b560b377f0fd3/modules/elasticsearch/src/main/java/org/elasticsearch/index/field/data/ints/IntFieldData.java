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

package org.elasticsearch.index.field.data.ints;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldCache;
import org.elasticsearch.index.field.data.FieldDataOptions;
import org.elasticsearch.index.field.data.NumericFieldData;
import org.elasticsearch.index.field.data.support.FieldDataLoader;
import org.elasticsearch.util.gnu.trove.TIntArrayList;

import java.io.IOException;

/**
 * @author kimchy (shay.banon)
 */
public abstract class IntFieldData extends NumericFieldData<IntDocFieldData> {

    static final int[] EMPTY_INT_ARRAY = new int[0];

    protected final int[] values;
    protected final int[] freqs;

    protected IntFieldData(String fieldName, FieldDataOptions options, int[] values, int[] freqs) {
        super(fieldName, options);
        this.values = values;
        this.freqs = freqs;
    }

    abstract public int value(int docId);

    abstract public int[] values(int docId);

    @Override public IntDocFieldData docFieldData(int docId) {
        return super.docFieldData(docId);
    }

    @Override protected IntDocFieldData createFieldData() {
        return new IntDocFieldData(this);
    }

    @Override public String stringValue(int docId) {
        return Integer.toString(value(docId));
    }

    @Override public void forEachValue(StringValueProc proc) {
        if (freqs == null) {
            for (int i = 1; i < values.length; i++) {
                proc.onValue(Integer.toString(values[i]), -1);
            }
        } else {
            for (int i = 1; i < values.length; i++) {
                proc.onValue(Integer.toString(values[i]), freqs[i]);
            }
        }
    }

    @Override public byte byteValue(int docId) {
        return (byte) value(docId);
    }

    @Override public short shortValue(int docId) {
        return (short) value(docId);
    }

    @Override public int intValue(int docId) {
        return value(docId);
    }

    @Override public long longValue(int docId) {
        return (long) value(docId);
    }

    @Override public float floatValue(int docId) {
        return (float) value(docId);
    }

    @Override public double doubleValue(int docId) {
        return (double) value(docId);
    }

    @Override public Type type() {
        return Type.INT;
    }

    public void forEachValue(ValueProc proc) {
        if (freqs == null) {
            for (int i = 1; i < values.length; i++) {
                proc.onValue(values[i], -1);
            }
        } else {
            for (int i = 1; i < values.length; i++) {
                proc.onValue(values[i], freqs[i]);
            }
        }
    }

    public static interface ValueProc {
        void onValue(int value, int freq);
    }


    public static IntFieldData load(IndexReader reader, String field, FieldDataOptions options) throws IOException {
        return FieldDataLoader.load(reader, field, options, new IntTypeLoader());
    }

    static class IntTypeLoader extends FieldDataLoader.FreqsTypeLoader<IntFieldData> {

        private final TIntArrayList terms = new TIntArrayList();

        IntTypeLoader() {
            super();
            // the first one indicates null value
            terms.add(0);
        }

        @Override public void collectTerm(String term) {
            terms.add(FieldCache.NUMERIC_UTILS_INT_PARSER.parseInt(term));
        }

        @Override public IntFieldData buildSingleValue(String field, int[] order) {
            return new SingleValueIntFieldData(field, options, order, terms.toNativeArray(), buildFreqs());
        }

        @Override public IntFieldData buildMultiValue(String field, int[][] order) {
            return new MultiValueIntFieldData(field, options, order, terms.toNativeArray(), buildFreqs());
        }
    }
}