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

package org.elasticsearch.common.cli;

import java.io.IOException;

import org.elasticsearch.cli.MockTerminal;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.StreamsUtils;
import org.junit.After;
import org.junit.Before;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

public abstract class CliToolTestCase extends ESTestCase {

    @Before
    @SuppressForbidden(reason = "sets es.default.path.home during tests")
    public void setPathHome() {
        System.setProperty("es.default.path.home", createTempDir().toString());
    }

    @After
    @SuppressForbidden(reason = "clears es.default.path.home during tests")
    public void clearPathHome() {
        System.clearProperty("es.default.path.home");
    }

    public static String[] args(String command) {
        if (!Strings.hasLength(command)) {
            return Strings.EMPTY_ARRAY;
        }
        return command.split("\\s+");
    }

    public static void assertTerminalOutputContainsHelpFile(MockTerminal terminal, String classPath) throws IOException {
        String output = terminal.getOutput();
        assertThat(output, not(isEmptyString()));
        String expectedDocs = StreamsUtils.copyToStringFromClasspath(classPath);
        // convert to *nix newlines as MockTerminal used for tests also uses *nix newlines
        expectedDocs = expectedDocs.replace("\r\n", "\n");
        assertThat(output, containsString(expectedDocs));
    }
}
