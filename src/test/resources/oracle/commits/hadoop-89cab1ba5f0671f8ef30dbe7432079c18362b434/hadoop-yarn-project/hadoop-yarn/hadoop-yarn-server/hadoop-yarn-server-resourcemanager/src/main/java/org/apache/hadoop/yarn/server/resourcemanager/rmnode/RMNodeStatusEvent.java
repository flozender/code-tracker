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

package org.apache.hadoop.yarn.server.resourcemanager.rmnode;

import java.util.Collections;
import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.server.api.protocolrecords.LogAggregationReport;
import org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatResponse;
import org.apache.hadoop.yarn.server.api.records.NodeHealthStatus;

public class RMNodeStatusEvent extends RMNodeEvent {

  private final NodeHealthStatus nodeHealthStatus;
  private final List<ContainerStatus> containersCollection;
  private final NodeHeartbeatResponse latestResponse;
  private final List<ApplicationId> keepAliveAppIds;
  private List<LogAggregationReport> logAggregationReportsForApps;
  private final List<Container> nmReportedIncreasedContainers;
  
  // Used by tests
  public RMNodeStatusEvent(NodeId nodeId, NodeHealthStatus nodeHealthStatus,
      List<ContainerStatus> collection, List<ApplicationId> keepAliveAppIds,
      NodeHeartbeatResponse latestResponse) {
    this(nodeId, nodeHealthStatus, collection, keepAliveAppIds,
        latestResponse, null);
  }

  public RMNodeStatusEvent(NodeId nodeId, NodeHealthStatus nodeHealthStatus,
      List<ContainerStatus> collection, List<ApplicationId> keepAliveAppIds,
      NodeHeartbeatResponse latestResponse,
      List<Container> nmReportedIncreasedContainers) {
    this(nodeId, nodeHealthStatus, collection, keepAliveAppIds, latestResponse,
        null, nmReportedIncreasedContainers);
  }

  public RMNodeStatusEvent(NodeId nodeId, NodeHealthStatus nodeHealthStatus,
      List<ContainerStatus> collection, List<ApplicationId> keepAliveAppIds,
      NodeHeartbeatResponse latestResponse,
      List<LogAggregationReport> logAggregationReportsForApps,
      List<Container> nmReportedIncreasedContainers) {
    super(nodeId, RMNodeEventType.STATUS_UPDATE);
    this.nodeHealthStatus = nodeHealthStatus;
    this.containersCollection = collection;
    this.keepAliveAppIds = keepAliveAppIds;
    this.latestResponse = latestResponse;
    this.logAggregationReportsForApps = logAggregationReportsForApps;
    this.nmReportedIncreasedContainers = nmReportedIncreasedContainers;
  }

  public NodeHealthStatus getNodeHealthStatus() {
    return this.nodeHealthStatus;
  }

  public List<ContainerStatus> getContainers() {
    return this.containersCollection;
  }

  public NodeHeartbeatResponse getLatestResponse() {
    return this.latestResponse;
  }
  
  public List<ApplicationId> getKeepAliveAppIds() {
    return this.keepAliveAppIds;
  }

  public List<LogAggregationReport> getLogAggregationReportsForApps() {
    return this.logAggregationReportsForApps;
  }

  public void setLogAggregationReportsForApps(
      List<LogAggregationReport> logAggregationReportsForApps) {
    this.logAggregationReportsForApps = logAggregationReportsForApps;
  }
  
  public List<Container> getNMReportedIncreasedContainers() {
    return nmReportedIncreasedContainers == null ? Collections.EMPTY_LIST
        : nmReportedIncreasedContainers;
  }
}