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

package org.elasticsearch.index.cache.filter;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;
import org.elasticsearch.index.IndexComponent;
import org.elasticsearch.util.component.CloseableComponent;

/**
 * @author kimchy (shay.banon)
 */
public interface FilterCache extends IndexComponent, CloseableComponent {

    String type();

    Filter cache(Filter filterToCache);

    boolean isCached(Filter filter);

    void clear(IndexReader reader);

    void clear();

    /**
     * Clears unreferenced filters.
     */
    void clearUnreferenced();
}
