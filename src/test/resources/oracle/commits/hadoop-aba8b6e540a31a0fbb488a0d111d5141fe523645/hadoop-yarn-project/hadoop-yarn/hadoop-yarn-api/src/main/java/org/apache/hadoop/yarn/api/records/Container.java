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

package org.apache.hadoop.yarn.api.records;

import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Stable;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.AMRMProtocol;
import org.apache.hadoop.yarn.api.ContainerManager;
import org.apache.hadoop.yarn.util.Records;

/**
 * <p><code>Container</code> represents an allocated resource in the cluster.
 * </p>
 * 
 * <p>The <code>ResourceManager</code> is the sole authority to allocate any
 * <code>Container</code> to applications. The allocated <code>Container</code>
 * is always on a single node and has a unique {@link ContainerId}. It has
 * a specific amount of {@link Resource} allocated.</p>
 * 
 * <p>It includes details such as:
 *   <ul>
 *     <li>{@link ContainerId} for the container, which is globally unique.</li>
 *     <li>
 *       {@link NodeId} of the node on which it is allocated.
 *     </li>
 *     <li>HTTP uri of the node.</li>
 *     <li>{@link Resource} allocated to the container.</li>
 *     <li>{@link Priority} at which the container was allocated.</li>
 *     <li>{@link ContainerState} of the container.</li>
 *     <li>
 *       {@link ContainerToken} of the container, used to securely verify 
 *       authenticity of the allocation. 
 *     </li>
 *     <li>{@link ContainerStatus} of the container.</li>
 *   </ul>
 * </p>
 * 
 * <p>Typically, an <code>ApplicationMaster</code> receives the 
 * <code>Container</code> from the <code>ResourceManager</code> during
 * resource-negotiation and then talks to the <code>NodManager</code> to 
 * start/stop containers.</p>
 * 
 * @see AMRMProtocol#allocate(org.apache.hadoop.yarn.api.protocolrecords.AllocateRequest)
 * @see ContainerManager#startContainer(org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequest)
 * @see ContainerManager#stopContainer(org.apache.hadoop.yarn.api.protocolrecords.StopContainerRequest)
 */
@Public
@Stable
public abstract class Container implements Comparable<Container> {

  @Private
  public static Container newInstance(ContainerId containerId, NodeId nodeId,
      String nodeHttpAddress, Resource resource, Priority priority,
      ContainerToken containerToken) {
    Container container = Records.newRecord(Container.class);
    container.setId(containerId);
    container.setNodeId(nodeId);
    container.setNodeHttpAddress(nodeHttpAddress);
    container.setResource(resource);
    container.setPriority(priority);
    container.setContainerToken(containerToken);
    return container;
  }

  /**
   * Get the globally unique identifier for the container.
   * @return globally unique identifier for the container
   */
  @Public
  @Stable
  public abstract ContainerId getId();
  
  @Private
  @Unstable
  public abstract void setId(ContainerId id);

  /**
   * Get the identifier of the node on which the container is allocated.
   * @return identifier of the node on which the container is allocated
   */
  @Public
  @Stable
  public abstract NodeId getNodeId();
  
  @Private
  @Unstable
  public abstract void setNodeId(NodeId nodeId);
  
  /**
   * Get the http uri of the node on which the container is allocated.
   * @return http uri of the node on which the container is allocated
   */
  @Public
  @Stable
  public abstract String getNodeHttpAddress();
  
  @Private
  @Unstable
  public abstract void setNodeHttpAddress(String nodeHttpAddress);
  
  /**
   * Get the <code>Resource</code> allocated to the container.
   * @return <code>Resource</code> allocated to the container
   */
  @Public
  @Stable
  public abstract Resource getResource();
  
  @Private
  @Unstable
  public abstract void setResource(Resource resource);

  /**
   * Get the <code>Priority</code> at which the <code>Container</code> was
   * allocated.
   * @return <code>Priority</code> at which the <code>Container</code> was
   *         allocated
   */
  public abstract Priority getPriority();
  
  @Private
  @Unstable
  public abstract void setPriority(Priority priority);
  
  /**
   * Get the <code>ContainerToken</code> for the container.
   * @return <code>ContainerToken</code> for the container
   */
  @Public
  @Stable
  public abstract ContainerToken getContainerToken();
  
  @Private
  @Unstable
  public abstract void setContainerToken(ContainerToken containerToken);
}
