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

package org.elasticsearch.action.bulk;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.action.Actions.*;

/**
 * A bulk request holds an ordered {@link IndexRequest}s and {@link DeleteRequest}s and allows to executes
 * it in a single batch.
 *
 * @author kimchy (shay.banon)
 * @see org.elasticsearch.client.Client#bulk(BulkRequest)
 */
public class BulkRequest implements ActionRequest {

    final List<ActionRequest> requests = Lists.newArrayList();

    private boolean listenerThreaded = false;

    /**
     * Adds an {@link IndexRequest} to the list of actions to execute. Follows the same behavior of {@link IndexRequest}
     * (for example, if no id is provided, one will be generated, or usage of the create flag).
     */
    public BulkRequest add(IndexRequest request) {
        // if the source is from a builder, we need to copy it over before adding the next one, which can come from a builder as well...
        if (request.sourceFromBuilder()) {
            request.beforeLocalFork();
        }
        requests.add(request);
        return this;
    }

    /**
     * Adds an {@link DeleteRequest} to the list of actions to execute.
     */
    public BulkRequest add(DeleteRequest request) {
        requests.add(request);
        return this;
    }

    /**
     * Adds a framed data in binary format
     */
    public BulkRequest add(byte[] data, int from, int length, boolean contentUnsafe) throws Exception {
        XContent xContent = XContentFactory.xContent(data, from, length);
        byte marker = xContent.streamSeparator();
        while (true) {
            int nextMarker = findNextMarker(marker, from, data, length);
            if (nextMarker == -1) {
                break;
            }
            // now parse the action
            XContentParser parser = xContent.createParser(data, from, nextMarker - from);

            // move pointers
            from = nextMarker + 1;

            // Move to START_OBJECT
            XContentParser.Token token = parser.nextToken();
            if (token == null) {
                continue;
            }
            assert token == XContentParser.Token.START_OBJECT;
            // Move to FIELD_NAME, that's the action
            token = parser.nextToken();
            assert token == XContentParser.Token.FIELD_NAME;
            String action = parser.currentName();
            // Move to START_OBJECT
            token = parser.nextToken();
            assert token == XContentParser.Token.START_OBJECT;

            String index = null;
            String type = null;
            String id = null;

            String currentFieldName = null;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (token.isValue()) {
                    if ("index".equals(currentFieldName)) {
                        index = parser.text();
                    } else if ("type".equals(currentFieldName)) {
                        type = parser.text();
                    } else if ("id".equals(currentFieldName)) {
                        id = parser.text();
                    }
                }
            }

            if ("delete".equals(action)) {
                add(new DeleteRequest(index, type, id));
            } else {
                nextMarker = findNextMarker(marker, from, data, length);
                if (nextMarker == -1) {
                    break;
                }
                add(new IndexRequest(index, type, id)
                        .create("create".equals(action))
                        .source(data, from, nextMarker - from, contentUnsafe));
                // move pointers
                from = nextMarker + 1;
            }
        }
        return this;
    }

    private int findNextMarker(byte marker, int from, byte[] data, int length) {
        for (int i = from; i < length; i++) {
            if (data[i] == marker) {
                return i;
            }
        }
        return -1;
    }

    public int numberOfActions() {
        return requests.size();
    }

    @Override public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (requests.isEmpty()) {
            validationException = addValidationError("no requests added", validationException);
        }
        for (int i = 0; i < requests.size(); i++) {
            ActionRequestValidationException ex = requests.get(i).validate();
            if (ex != null) {
                if (validationException == null) {
                    validationException = new ActionRequestValidationException();
                }
                validationException.addValidationErrors(ex.validationErrors());
            }
        }

        return validationException;
    }

    @Override public boolean listenerThreaded() {
        return listenerThreaded;
    }

    @Override public BulkRequest listenerThreaded(boolean listenerThreaded) {
        this.listenerThreaded = listenerThreaded;
        return this;
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        int size = in.readVInt();
        for (int i = 0; i < size; i++) {
            byte type = in.readByte();
            if (type == 0) {
                IndexRequest request = new IndexRequest();
                request.readFrom(in);
                requests.add(request);
            } else if (type == 1) {
                DeleteRequest request = new DeleteRequest();
                request.readFrom(in);
                requests.add(request);
            }
        }
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(requests.size());
        for (ActionRequest request : requests) {
            if (request instanceof IndexRequest) {
                out.writeByte((byte) 0);
            } else if (request instanceof DeleteRequest) {
                out.writeByte((byte) 1);
            }
            request.writeTo(out);
        }
    }
}
