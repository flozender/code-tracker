package org.elasticsearch.action.termvector;
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


import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.elasticsearch.action.termvector.MultiTermVectorsItemResponse;
import org.elasticsearch.action.termvector.MultiTermVectorsRequestBuilder;
import org.elasticsearch.action.termvector.MultiTermVectorsResponse;
import org.elasticsearch.action.termvector.TermVectorRequestBuilder;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
public class MultiTermVectorsTests extends AbstractTermVectorTests {

    @Test
    public void testDuelESLucene() throws Exception {
        AbstractTermVectorTests.TestFieldSetting[] testFieldSettings = getFieldSettings();
        createIndexBasedOnFieldSettings(testFieldSettings, -1);
        AbstractTermVectorTests.TestDoc[] testDocs = generateTestDocs(5, testFieldSettings);

        DirectoryReader directoryReader = indexDocsWithLucene(testDocs);
        AbstractTermVectorTests.TestConfig[] testConfigs = generateTestConfigs(20, testDocs, testFieldSettings);

        MultiTermVectorsRequestBuilder requestBuilder = client().prepareMultiTermVectors();
        for (AbstractTermVectorTests.TestConfig test : testConfigs) {
            requestBuilder.add(getRequestForConfig(test).request());
        }

        MultiTermVectorsItemResponse[] responseItems = requestBuilder.get().getResponses();

        for (int i = 0; i < testConfigs.length; i++) {
            TestConfig test = testConfigs[i];
            try {
                MultiTermVectorsItemResponse item = responseItems[i];
                if (test.expectedException != null) {
                    assertTrue(item.isFailed());
                    continue;
                } else if (item.isFailed()) {
                    fail(item.getFailure().getMessage());
                }
                Fields luceneTermVectors = getTermVectorsFromLucene(directoryReader, test.doc);
                validateResponse(item.getResponse(), luceneTermVectors, test);
            } catch (Throwable t) {
                throw new Exception("Test exception while running " + test.toString(), t);
            }
        }

    }
    public void testMissingIndexThrowsMissingIndex() throws Exception {
        TermVectorRequestBuilder requestBuilder = client().prepareTermVector("testX", "typeX", Integer.toString(1));
        MultiTermVectorsRequestBuilder mtvBuilder = new MultiTermVectorsRequestBuilder(client());
        mtvBuilder.add(requestBuilder.request());
        MultiTermVectorsResponse response = mtvBuilder.execute().actionGet();
        assertThat(response.getResponses().length, equalTo(1));
        assertThat(response.getResponses()[0].getFailure().getMessage(), equalTo("[" + response.getResponses()[0].getIndex() + "] missing"));
    }
}
