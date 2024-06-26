/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.elasticsearch.common.lucene.index;

import com.google.common.collect.Lists;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import java.lang.IllegalArgumentException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.lucene.docset.DocIdSets;

import java.io.IOException;
import java.util.List;

/**
 * A frequency TermsEnum that returns frequencies derived from a collection of
 * cached leaf termEnums. It also allows to provide a filter to explicitly
 * compute frequencies only for docs that match the filter (heavier!).
 */
public class FilterableTermsEnum extends TermsEnum {

    static class Holder {
        final TermsEnum termsEnum;
        @Nullable
        PostingsEnum docsEnum;
        @Nullable
        final Bits bits;

        Holder(TermsEnum termsEnum, Bits bits) {
            this.termsEnum = termsEnum;
            this.bits = bits;
        }
    }

    static final String UNSUPPORTED_MESSAGE = "This TermsEnum only supports #seekExact(BytesRef) as well as #docFreq() and #totalTermFreq()";
    protected final static int NOT_FOUND = -1;
    private final Holder[] enums;
    protected int currentDocFreq = 0;
    protected long currentTotalTermFreq = 0;
    protected BytesRef current;
    protected final int docsEnumFlag;
    protected int numDocs;

    public FilterableTermsEnum(IndexReader reader, String field, int docsEnumFlag, @Nullable final Filter filter) throws IOException {
        if ((docsEnumFlag != PostingsEnum.FREQS) && (docsEnumFlag != PostingsEnum.NONE)) {
            throw new IllegalArgumentException("invalid docsEnumFlag of " + docsEnumFlag);
        }
        this.docsEnumFlag = docsEnumFlag;
        if (filter == null) {
            // Important - need to use the doc count that includes deleted docs
            // or we have this issue: https://github.com/elasticsearch/elasticsearch/issues/7951
            numDocs = reader.maxDoc();
        }
        List<LeafReaderContext> leaves = reader.leaves();
        List<Holder> enums = Lists.newArrayListWithExpectedSize(leaves.size());
        for (LeafReaderContext context : leaves) {
            Terms terms = context.reader().terms(field);
            if (terms == null) {
                continue;
            }
            TermsEnum termsEnum = terms.iterator();
            if (termsEnum == null) {
                continue;
            }
            Bits bits = null;
            if (filter != null) {
                // we want to force apply deleted docs
                DocIdSet docIdSet = filter.getDocIdSet(context, context.reader().getLiveDocs());
                if (DocIdSets.isEmpty(docIdSet)) {
                    // fully filtered, none matching, no need to iterate on this
                    continue;
                }
                bits = DocIdSets.toSafeBits(context.reader().maxDoc(), docIdSet);
                // Count how many docs are in our filtered set
                // TODO make this lazy-loaded only for those that need it?
                DocIdSetIterator iterator = docIdSet.iterator();
                if (iterator != null) {
                    while (iterator.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        numDocs++;
                    }
                }
            }
            enums.add(new Holder(termsEnum, bits));
        }
        this.enums = enums.toArray(new Holder[enums.size()]);
    }

    public int getNumDocs() {
        return numDocs;
    }

    @Override
    public BytesRef term() throws IOException {
        return current;
    }

    @Override
    public boolean seekExact(BytesRef text) throws IOException {
        int docFreq = 0;
        long totalTermFreq = 0;
        for (Holder anEnum : enums) {
            if (anEnum.termsEnum.seekExact(text)) {
                if (anEnum.bits == null) {
                    docFreq += anEnum.termsEnum.docFreq();
                    if (docsEnumFlag == PostingsEnum.FREQS) {
                        long leafTotalTermFreq = anEnum.termsEnum.totalTermFreq();
                        if (totalTermFreq == -1 || leafTotalTermFreq == -1) {
                            totalTermFreq = -1;
                            continue;
                        }
                        totalTermFreq += leafTotalTermFreq;
                    }
                } else {
                    final PostingsEnum docsEnum = anEnum.docsEnum = anEnum.termsEnum.postings(anEnum.bits, anEnum.docsEnum, docsEnumFlag);
                    // 2 choices for performing same heavy loop - one attempts to calculate totalTermFreq and other does not
                    if (docsEnumFlag == PostingsEnum.FREQS) {
                        for (int docId = docsEnum.nextDoc(); docId != DocIdSetIterator.NO_MORE_DOCS; docId = docsEnum.nextDoc()) {
                            docFreq++;
                            // docsEnum.freq() returns 1 if doc indexed with IndexOptions.DOCS_ONLY so no way of knowing if value
                            // is really 1 or unrecorded when filtering like this
                            totalTermFreq += docsEnum.freq();
                        }
                    } else {
                        for (int docId = docsEnum.nextDoc(); docId != DocIdSetIterator.NO_MORE_DOCS; docId = docsEnum.nextDoc()) {
                            // docsEnum.freq() behaviour is undefined if docsEnumFlag==PostingsEnum.FLAG_NONE so don't bother with call
                            docFreq++;
                        }
                    }
                }
            }
        }
        if (docFreq > 0) {
            currentDocFreq = docFreq;
            currentTotalTermFreq = totalTermFreq;
            current = text;
            return true;
        } else {
            currentDocFreq = NOT_FOUND;
            currentTotalTermFreq = NOT_FOUND;
            current = null;
            return false;
        }
    }

    @Override
    public int docFreq() throws IOException {
        return currentDocFreq;
    }

    @Override
    public long totalTermFreq() throws IOException {
        return currentTotalTermFreq;
    }

    @Override
    public void seekExact(long ord) throws IOException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public SeekStatus seekCeil(BytesRef text) throws IOException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public long ord() throws IOException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public PostingsEnum postings(Bits liveDocs, PostingsEnum reuse, int flags) throws IOException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public BytesRef next() throws IOException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }
}