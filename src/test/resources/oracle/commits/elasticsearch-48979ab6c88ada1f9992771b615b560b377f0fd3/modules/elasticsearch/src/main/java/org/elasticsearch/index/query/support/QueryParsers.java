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

package org.elasticsearch.index.query.support;

import org.apache.lucene.search.*;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.query.xcontent.QueryParseContext;
import org.elasticsearch.util.lucene.search.TermFilter;

import javax.annotation.Nullable;

/**
 * @author kimchy (shay.banon)
 */
public final class QueryParsers {

    private QueryParsers() {

    }

    public static Query wrapSmartNameQuery(Query query, @Nullable MapperService.SmartNameFieldMappers smartFieldMappers,
                                           QueryParseContext parseContext) {
        if (smartFieldMappers == null) {
            return query;
        }
        if (!smartFieldMappers.hasDocMapper()) {
            return query;
        }
        DocumentMapper docMapper = smartFieldMappers.docMapper();

        Filter typeFilter = new TermFilter(docMapper.typeMapper().term(docMapper.type()));
        typeFilter = parseContext.cacheFilterIfPossible(typeFilter);

        return new FilteredQuery(query, typeFilter);
    }

    public static Filter wrapSmartNameFilter(Filter filter, @Nullable MapperService.SmartNameFieldMappers smartFieldMappers,
                                             QueryParseContext parseContext) {
        if (smartFieldMappers == null) {
            return filter;
        }
        if (!smartFieldMappers.hasDocMapper()) {
            return filter;
        }
        DocumentMapper docMapper = smartFieldMappers.docMapper();
        BooleanFilter booleanFilter = new BooleanFilter();

        Filter typeFilter = new TermFilter(docMapper.typeMapper().term(docMapper.type()));
        typeFilter = parseContext.cacheFilterIfPossible(typeFilter);

        booleanFilter.add(new FilterClause(typeFilter, BooleanClause.Occur.MUST));
        booleanFilter.add(new FilterClause(filter, BooleanClause.Occur.MUST));

        // don't cache the boolean filter...
        return booleanFilter;
    }
}
