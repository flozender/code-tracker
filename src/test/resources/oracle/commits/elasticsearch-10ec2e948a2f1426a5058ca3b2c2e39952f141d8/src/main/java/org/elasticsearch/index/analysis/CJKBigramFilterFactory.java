package org.elasticsearch.index.analysis;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.settings.IndexSettings;

/**
 * Factory that creates a {@link CJKBigramFilter} to form bigrams of CJK terms
 * that are generated from StandardTokenizer or ICUTokenizer.
 * <p>
 * CJK types are set by these tokenizers, but you can also use flags to
 * explicitly control which of the CJK scripts are turned into bigrams.
 * <p>
 * By default, when a CJK character has no adjacent characters to form a bigram,
 * it is output in unigram form. If you want to always output both unigrams and
 * bigrams, set the <code>outputUnigrams</code> flag. This can be used for a
 * combined unigram+bigram approach.
 * <p>
 * In all cases, all non-CJK input is passed thru unmodified.
 */
public final class CJKBigramFilterFactory extends AbstractTokenFilterFactory {

    private final int flags;
    private final boolean outputUnigrams;

    @Inject
    public CJKBigramFilterFactory(Index index, @IndexSettings Settings indexSettings, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettings, name, settings);
        outputUnigrams = settings.getAsBoolean("output_unigrams", false);
        final String[] asArray = settings.getAsArray("ignored_scripts");
        Set<String> scripts = new HashSet<String>(Arrays.asList("han", "hiragana", "katakana", "hangul"));
        if (asArray != null) {
            scripts.removeAll(Arrays.asList(asArray));
        }
        int flags = 0;
        for (String script : scripts) {
            if ("han".equals(script)) {
                flags |= CJKBigramFilter.HAN;
            } else if ("hiragana".equals(script)) {
                flags |= CJKBigramFilter.HIRAGANA;
            } else if("katakana".equals(script)) {
                flags |= CJKBigramFilter.KATAKANA;
            } else if("hangul".equals(script)) {
                flags |= CJKBigramFilter.HANGUL;
            } 
        }
        this.flags = flags;
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new CJKBigramFilter(tokenStream, flags, outputUnigrams);
    }

}
