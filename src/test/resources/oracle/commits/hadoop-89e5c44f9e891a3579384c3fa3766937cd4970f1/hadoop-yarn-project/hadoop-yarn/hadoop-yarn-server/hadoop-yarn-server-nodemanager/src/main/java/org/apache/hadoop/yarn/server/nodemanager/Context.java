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

package org.apache.hadoop.yarn.server.nodemanager;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.security.ContainerTokenIdentifier;
import org.apache.hadoop.yarn.server.api.protocolrecords.LogAggregationReport;
import org.apache.hadoop.yarn.server.api.records.NodeHealthStatus;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.ContainerManager;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.application.Application;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.Container;
import org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreService;
import org.apache.hadoop.yarn.server.nodemanager.scheduler.OpportunisticContainerAllocator;
import org.apache.hadoop.yarn.server.nodemanager.security.NMContainerTokenSecretManager;
import org.apache.hadoop.yarn.server.nodemanager.security.NMTokenSecretManagerInNM;
import org.apache.hadoop.yarn.server.nodemanager.timelineservice.NMTimelinePublisher;
import org.apache.hadoop.yarn.server.security.ApplicationACLsManager;

/**
 * Context interface for sharing information across components in the
 * NodeManager.
 */
public interface Context {

  /**
   * Interface exposing methods related to the queuing of containers in the NM.
   */
  interface QueuingContext {
    ConcurrentMap<ContainerId, ContainerTokenIdentifier> getQueuedContainers();

    ConcurrentMap<ContainerTokenIdentifier, String> getKilledQueuedContainers();
  }

  /**
   * Return the nodeId. Usable only when the ContainerManager is started.
   * 
   * @return the NodeId
   */
  NodeId getNodeId();

  /**
   * Return the node http-address. Usable only after the Webserver is started.
   * 
   * @return the http-port
   */
  int getHttpPort();

  ConcurrentMap<ApplicationId, Application> getApplications();

  Map<ApplicationId, Credentials> getSystemCredentialsForApps();

  /**
   * Get the registered collectors that located on this NM.
   * @return registered collectors, or null if the timeline service v.2 is not
   * enabled
   */
  Map<ApplicationId, String> getRegisteredCollectors();

  ConcurrentMap<ContainerId, Container> getContainers();

  ConcurrentMap<ContainerId, org.apache.hadoop.yarn.api.records.Container>
      getIncreasedContainers();

  NMContainerTokenSecretManager getContainerTokenSecretManager();
  
  NMTokenSecretManagerInNM getNMTokenSecretManager();

  NodeHealthStatus getNodeHealthStatus();

  ContainerManager getContainerManager();

  NodeResourceMonitor getNodeResourceMonitor();

  LocalDirsHandlerService getLocalDirsHandler();

  ApplicationACLsManager getApplicationACLsManager();

  NMStateStoreService getNMStateStore();

  boolean getDecommissioned();
  
  Configuration getConf();

  void setDecommissioned(boolean isDecommissioned);

  ConcurrentLinkedQueue<LogAggregationReport>
      getLogAggregationStatusForApps();

  NodeStatusUpdater getNodeStatusUpdater();

  /**
   * Returns a <code>QueuingContext</code> that provides information about the
   * number of Containers Queued as well as the number of Containers that were
   * queued and killed.
   */
  QueuingContext getQueuingContext();

  boolean isDistributedSchedulingEnabled();

  OpportunisticContainerAllocator getContainerAllocator();

  void setNMTimelinePublisher(NMTimelinePublisher nmMetricsPublisher);

  NMTimelinePublisher getNMTimelinePublisher();
}
