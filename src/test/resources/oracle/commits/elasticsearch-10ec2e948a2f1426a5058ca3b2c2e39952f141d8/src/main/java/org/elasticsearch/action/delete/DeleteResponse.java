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

package org.elasticsearch.action.delete;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * The response of the delete action.
 *
 * @see org.elasticsearch.action.delete.DeleteRequest
 * @see org.elasticsearch.client.Client#delete(DeleteRequest)
 */
public class DeleteResponse extends ActionResponse {

    private String index;
    private String id;
    private String type;
    private long version;
    private boolean notFound;

    public DeleteResponse() {

    }

    public DeleteResponse(String index, String type, String id, long version, boolean notFound) {
        this.index = index;
        this.id = id;
        this.type = type;
        this.version = version;
        this.notFound = notFound;
    }

    /**
     * The index the document was deleted from.
     */
    public String getIndex() {
        return this.index;
    }

    /**
     * The type of the document deleted.
     */
    public String getType() {
        return this.type;
    }

    /**
     * The id of the document deleted.
     */
    public String getId() {
        return this.id;
    }

    /**
     * The version of the delete operation.
     */
    public long getVersion() {
        return this.version;
    }

    /**
     * Returns <tt>true</tt> if there was no doc found to delete.
     */
    public boolean isNotFound() {
        return notFound;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        index = in.readSharedString();
        type = in.readSharedString();
        id = in.readString();
        version = in.readLong();
        notFound = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeSharedString(index);
        out.writeSharedString(type);
        out.writeString(id);
        out.writeLong(version);
        out.writeBoolean(notFound);
    }
}
