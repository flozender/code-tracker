/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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


package org.elasticsearch.common.recycler;

/**
 */
public final class RecyclerUtils {

    private RecyclerUtils() {
    }

    // Borrowed from IOUtils#close
    public static void release(Recycler.V... recyclers) {
        Throwable th = null;

        for (Recycler.V recycler : recyclers) {
            try {
                if (recycler != null) {
                    recycler.release();
                }
            } catch (Throwable t) {
                if (th == null) {
                    th = t;
                }
            }
        }

        if (th != null) {
            throw new RuntimeException(th);
        }
    }

}
