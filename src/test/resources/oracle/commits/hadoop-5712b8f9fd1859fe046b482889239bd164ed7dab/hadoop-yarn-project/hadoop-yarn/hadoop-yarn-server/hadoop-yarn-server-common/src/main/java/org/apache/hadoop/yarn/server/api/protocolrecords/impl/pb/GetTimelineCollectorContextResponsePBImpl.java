/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.api.protocolrecords.impl.pb;


import com.google.protobuf.TextFormat;
import org.apache.hadoop.yarn.proto.YarnServerCommonServiceProtos.GetTimelineCollectorContextResponseProtoOrBuilder;
import org.apache.hadoop.yarn.proto.YarnServerCommonServiceProtos.GetTimelineCollectorContextResponseProto;
import org.apache.hadoop.yarn.server.api.protocolrecords.GetTimelineCollectorContextResponse;

public class GetTimelineCollectorContextResponsePBImpl extends
    GetTimelineCollectorContextResponse {

  GetTimelineCollectorContextResponseProto proto =
      GetTimelineCollectorContextResponseProto.getDefaultInstance();
  GetTimelineCollectorContextResponseProto.Builder builder = null;
  boolean viaProto = false;

  public GetTimelineCollectorContextResponsePBImpl() {
    builder = GetTimelineCollectorContextResponseProto.newBuilder();
  }

  public GetTimelineCollectorContextResponsePBImpl(
      GetTimelineCollectorContextResponseProto proto) {
    this.proto = proto;
    viaProto = true;
  }

  public GetTimelineCollectorContextResponseProto getProto() {
    mergeLocalToProto();
    proto = viaProto ? proto : builder.build();
    viaProto = true;
    return proto;
  }

  @Override
  public int hashCode() {
    return getProto().hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null)
      return false;
    if (other.getClass().isAssignableFrom(this.getClass())) {
      return this.getProto().equals(this.getClass().cast(other).getProto());
    }
    return false;
  }

  @Override
  public String toString() {
    return TextFormat.shortDebugString(getProto());
  }

  private void mergeLocalToProto() {
    if (viaProto)
      maybeInitBuilder();
    proto = builder.build();
    viaProto = true;
  }

  private void maybeInitBuilder() {
    if (viaProto || builder == null) {
      builder = GetTimelineCollectorContextResponseProto.newBuilder(proto);
    }
    viaProto = false;
  }

  @Override
  public String getUserId() {
    GetTimelineCollectorContextResponseProtoOrBuilder p = viaProto ? proto : builder;
    if (!p.hasUserId()) {
      return null;
    }
    return p.getUserId();
  }

  @Override
  public void setUserId(String userId) {
    maybeInitBuilder();
    if (userId == null) {
      builder.clearUserId();
      return;
    }
    builder.setUserId(userId);
  }

  @Override
  public String getFlowId() {
    GetTimelineCollectorContextResponseProtoOrBuilder p = viaProto ? proto : builder;
    if (!p.hasFlowId()) {
      return null;
    }
    return p.getFlowId();
  }

  @Override
  public void setFlowId(String flowId) {
    maybeInitBuilder();
    if (flowId == null) {
      builder.clearFlowId();
      return;
    }
    builder.setFlowId(flowId);
  }

  @Override
  public String getFlowRunId() {
    GetTimelineCollectorContextResponseProtoOrBuilder p = viaProto ? proto : builder;
    if (!p.hasFlowRunId()) {
      return null;
    }
    return p.getFlowRunId();
  }

  @Override
  public void setFlowRunId(String flowRunId) {
    maybeInitBuilder();
    if (flowRunId == null) {
      builder.clearFlowRunId();
      return;
    }
    builder.setFlowRunId(flowRunId);
  }
}
