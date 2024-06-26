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

package org.elasticsearch.util.lucene.search;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.elasticsearch.util.collect.Lists;
import org.elasticsearch.util.lucene.docset.AndDocSet;
import org.elasticsearch.util.lucene.docset.DocSet;
import org.elasticsearch.util.lucene.docset.DocSets;

import java.io.IOException;
import java.util.List;

/**
 * @author kimchy (shay.banon)
 */
public class AndFilter extends Filter {

    private final List<? extends Filter> filters;

    public AndFilter(List<? extends Filter> filters) {
        this.filters = filters;
    }

    public List<? extends Filter> filters() {
        return filters;
    }

    @Override public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
        if (filters.size() == 1) {
            return DocSets.convert(reader, filters.get(0).getDocIdSet(reader));
        }
        List<DocSet> sets = Lists.newArrayListWithExpectedSize(filters.size());
        for (Filter filter : filters) {
            sets.add(DocSets.convert(reader, filter.getDocIdSet(reader)));
        }
        return new AndDocSet(sets);
    }

    @Override public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (null == filters ? 0 : filters.hashCode());
        return hash;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if ((obj == null) || (obj.getClass() != this.getClass()))
            return false;

        AndFilter other = (AndFilter) obj;
        return equalFilters(filters, other.filters);
    }

    private boolean equalFilters(List<? extends Filter> filters1, List<? extends Filter> filters2) {
        return (filters1 == filters2) || ((filters1 != null) && filters1.equals(filters2));
    }
}
