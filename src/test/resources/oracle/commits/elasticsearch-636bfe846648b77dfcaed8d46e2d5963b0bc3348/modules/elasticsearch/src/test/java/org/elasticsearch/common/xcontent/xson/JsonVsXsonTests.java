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

package org.elasticsearch.common.xcontent.xson;

import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentGenerator;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.util.io.FastByteArrayOutputStream;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * @author kimchy (shay.banon)
 */
public class JsonVsXsonTests {

    @Test public void compareParsingTokens() throws IOException {
        FastByteArrayOutputStream xsonOs = new FastByteArrayOutputStream();
        XContentGenerator xsonGen = XContentFactory.xContent(XContentType.XSON).createGenerator(xsonOs);

        FastByteArrayOutputStream jsonOs = new FastByteArrayOutputStream();
        XContentGenerator jsonGen = XContentFactory.xContent(XContentType.JSON).createGenerator(jsonOs);

        xsonGen.writeStartObject();
        jsonGen.writeStartObject();

        xsonGen.writeStringField("test", "value");
        jsonGen.writeStringField("test", "value");

        xsonGen.writeArrayFieldStart("arr");
        jsonGen.writeArrayFieldStart("arr");
        xsonGen.writeNumber(1);
        jsonGen.writeNumber(1);
        xsonGen.writeNull();
        jsonGen.writeNull();
        xsonGen.writeEndArray();
        jsonGen.writeEndArray();

        xsonGen.writeEndObject();
        jsonGen.writeEndObject();

        xsonGen.close();
        jsonGen.close();

        verifySameTokens(XContentFactory.xContent(XContentType.JSON).createParser(jsonOs.copiedByteArray()), XContentFactory.xContent(XContentType.XSON).createParser(xsonOs.copiedByteArray()));
    }

    private void verifySameTokens(XContentParser parser1, XContentParser parser2) throws IOException {
        while (true) {
            XContentParser.Token token1 = parser1.nextToken();
            XContentParser.Token token2 = parser2.nextToken();
            if (token1 == null) {
                assertThat(token2, nullValue());
                return;
            }
            assertThat(token1, equalTo(token2));
            switch (token1) {
                case FIELD_NAME:
                    assertThat(parser1.currentName(), equalTo(parser2.currentName()));
                    break;
                case VALUE_STRING:
                    assertThat(parser1.text(), equalTo(parser2.text()));
                    break;
                case VALUE_NUMBER:
                    assertThat(parser1.numberType(), equalTo(parser2.numberType()));
                    assertThat(parser1.numberValue(), equalTo(parser2.numberValue()));
                    break;
            }
        }
    }
}
