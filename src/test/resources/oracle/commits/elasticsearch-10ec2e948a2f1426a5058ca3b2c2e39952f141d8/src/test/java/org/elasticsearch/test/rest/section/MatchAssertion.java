/*
 * Licensed to Elasticsearch under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elasticsearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.elasticsearch.test.rest.section;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Represents a match assert section:
 *
 *   - match:   { get.fields._routing: "5" }
 *
 */
public class MatchAssertion extends Assertion {

    private static final ESLogger logger = Loggers.getLogger(MatchAssertion.class);

    public MatchAssertion(String field, Object expectedValue) {
        super(field, expectedValue);
    }

    @Override
    protected void doAssert(Object actualValue, Object expectedValue) {
        assertThat(errorMessage(), actualValue, notNullValue());
        logger.trace("assert that [{}] matches [{}]", actualValue, expectedValue);
        if (!actualValue.getClass().equals(expectedValue.getClass())) {
            if (actualValue instanceof Number && expectedValue instanceof Number) {
                //Double 1.0 is equals to Integer 1
                assertThat(errorMessage(), ((Number) actualValue).doubleValue(), equalTo(((Number) expectedValue).doubleValue()));
            }
        } else {
            assertThat(errorMessage(), actualValue, equalTo(expectedValue));
        }
    }

    private String errorMessage() {
        return "field [" + getField() + "] doesn't match the expected value";
    }
}
