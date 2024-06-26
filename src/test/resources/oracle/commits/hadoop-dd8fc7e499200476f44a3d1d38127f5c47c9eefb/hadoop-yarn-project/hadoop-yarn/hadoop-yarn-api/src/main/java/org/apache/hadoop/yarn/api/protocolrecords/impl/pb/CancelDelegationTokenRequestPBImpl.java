/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.hadoop.yarn.api.protocolrecords.impl.pb;

import org.apache.hadoop.yarn.api.protocolrecords.CancelDelegationTokenRequest;
import org.apache.hadoop.yarn.api.records.DelegationToken;
import org.apache.hadoop.yarn.api.records.ProtoBase;
import org.apache.hadoop.yarn.api.records.impl.pb.DelegationTokenPBImpl;
import org.apache.hadoop.yarn.proto.YarnProtos.DelegationTokenProto;
import org.apache.hadoop.yarn.proto.YarnServiceProtos.CancelDelegationTokenRequestProto;
import org.apache.hadoop.yarn.proto.YarnServiceProtos.CancelDelegationTokenRequestProtoOrBuilder;

public class CancelDelegationTokenRequestPBImpl extends
    ProtoBase<CancelDelegationTokenRequestProto> implements
    CancelDelegationTokenRequest {

  CancelDelegationTokenRequestProto proto = CancelDelegationTokenRequestProto
      .getDefaultInstance();
  CancelDelegationTokenRequestProto.Builder builder = null;
  boolean viaProto = false;

  public CancelDelegationTokenRequestPBImpl() {
    builder = CancelDelegationTokenRequestProto.newBuilder();
  }

  public CancelDelegationTokenRequestPBImpl(
      CancelDelegationTokenRequestProto proto) {
    this.proto = proto;
    viaProto = true;
  }

  DelegationToken token;

  @Override
  public DelegationToken getDelegationToken() {
    CancelDelegationTokenRequestProtoOrBuilder p = viaProto ? proto : builder;
    if (this.token != null) {
      return this.token;
    }
    if (!p.hasDelegationToken()) {
      return null;
    }
    this.token = convertFromProtoFormat(p.getDelegationToken());
    return this.token;
  }

  @Override
  public void setDelegationToken(DelegationToken token) {
    maybeInitBuilder();
    if (token == null)
      builder.clearDelegationToken();
    this.token = token;
  }

  @Override
  public CancelDelegationTokenRequestProto getProto() {
    mergeLocalToProto();
    proto = viaProto ? proto : builder.build();
    viaProto = true;
    return proto;
  }

  private void mergeLocalToBuilder() {
    if (token != null) {
      builder.setDelegationToken(convertToProtoFormat(this.token));
    }
  }

  private void mergeLocalToProto() {
    if (viaProto)
      maybeInitBuilder();
    mergeLocalToBuilder();
    proto = builder.build();
    viaProto = true;
  }

  private void maybeInitBuilder() {
    if (viaProto || builder == null) {
      builder = CancelDelegationTokenRequestProto.newBuilder(proto);
    }
    viaProto = false;
  }

  private DelegationTokenPBImpl convertFromProtoFormat(DelegationTokenProto p) {
    return new DelegationTokenPBImpl(p);
  }

  private DelegationTokenProto convertToProtoFormat(DelegationToken t) {
    return ((DelegationTokenPBImpl) t).getProto();
  }
}
