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

package org.elasticsearch.common.util.concurrent;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.ElasticsearchGenerationException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A list backed by an {@link AtomicReferenceArray} with potential null values, easily allowing
 * to get the concrete values as a list using {@link #asList()}.
 */
public class AtomicArray<E> {

    private static final AtomicArray EMPTY = new AtomicArray(0);

    @SuppressWarnings("unchecked")
    public static <E> E empty() {
        return (E) EMPTY;
    }

    private final AtomicReferenceArray<E> array;
    private volatile List<Entry<E>> nonNullList;

    public AtomicArray(int size) {
        array = new AtomicReferenceArray<E>(size);
    }

    /**
     * The size of the expected results, including potential null values.
     */
    public int length() {
        return array.length();
    }


    /**
     * Sets the element at position {@code i} to the given value.
     *
     * @param i     the index
     * @param value the new value
     */
    public void set(int i, E value) {
        array.set(i, value);
        if (nonNullList != null) { // read first, lighter, and most times it will be null...
            nonNullList = null;
        }
    }

    /**
     * Gets the current value at position {@code i}.
     *
     * @param i the index
     * @return the current value
     */
    public E get(int i) {
        return array.get(i);
    }

    /**
     * Returns the it as a non null list, with an Entry wrapping each value allowing to
     * retain its index.
     */
    public List<Entry<E>> asList() {
        if (nonNullList == null) {
            if (array == null || array.length() == 0) {
                nonNullList = ImmutableList.of();
            } else {
                List<Entry<E>> list = new ArrayList<Entry<E>>(array.length());
                for (int i = 0; i < array.length(); i++) {
                    E e = array.get(i);
                    if (e != null) {
                        list.add(new Entry<E>(i, e));
                    }
                }
                nonNullList = list;
            }
        }
        return nonNullList;
    }

    /**
     * Copies the content of the underlying atomic array to a normal one.
     */
    public E[] toArray(E[] a) {
        if (a.length != array.length()) {
            throw new ElasticsearchGenerationException("AtomicArrays can only be copied to arrays of the same size");
        }
        for (int i = 0; i < array.length(); i++) {
            a[i] = array.get(i);
        }
        return a;
    }

    /**
     * An entry within the array.
     */
    public static class Entry<E> {
        /**
         * The original index of the value within the array.
         */
        public final int index;
        /**
         * The value.
         */
        public final E value;

        public Entry(int index, E value) {
            this.index = index;
            this.value = value;
        }
    }
}
