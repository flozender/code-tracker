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

import java.util.List;

import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Stable;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.AMRMProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateRequest;

/**
 * <p>The response sent by the <code>ResourceManager</code> the  
 * <code>ApplicationMaster</code> during resource negotiation via
 * {@link AMRMProtocol#allocate(AllocateRequest)}.</p>
 *
 * <p>The response includes:
 *   <ul>
 *     <li>Response ID to track duplicate responses.</li>
 *     <li>
 *       A reboot flag to let the <code>ApplicationMaster</code> that its 
 *       horribly out of sync and needs to reboot.</li>
 *     <li>A list of newly allocated {@link Container}.</li>
 *     <li>A list of completed {@link Container}.</li>
 *     <li>
 *       The available headroom for resources in the cluster for the
 *       application. 
 *     </li>
 *   </ul>
 * </p>
 */
@Public
@Unstable
public interface AMResponse {
  /**
   * Should the <code>ApplicationMaster</code> reboot for being horribly 
   * out-of-sync with the <code>ResourceManager</code> as deigned by 
   * {@link #getResponseId()}?
   * 
   * @return <code>true</code> if the <code>ApplicationMaster</code> should
   *         reboot, <code>false</code> otherwise
   */
  @Public
  @Stable
  public boolean getReboot();
  
  @Private
  @Unstable
  public void setReboot(boolean reboot);

  /**
   * Get the last response id.
   * @return the last response id
   */
  @Public
  @Stable
  public int getResponseId();
  
  @Private
  @Unstable
  public void setResponseId(int responseId);

  /**
   * Get the list of newly allocated {@link Container} by the 
   * <code>ResourceManager</code>.
   * @return list of newly allocated <code>Container</code> 
   */
  @Public
  @Stable
  public List<Container> getNewContainerList();

  @Private
  @Unstable
  public Container getNewContainer(int index);

  @Private
  @Unstable
  public int getNewContainerCount();

  @Private
  @Unstable
  public void addAllNewContainers(List<Container> containers);

  @Private
  @Unstable
  public void addNewContainer(Container container);

  @Private
  @Unstable
  public void removeNewContainer(int index);

  @Private
  @Unstable
  public void clearNewContainers();
  
  /**
   * Get available headroom for resources in the cluster for the application.
   */
  @Public
  @Stable
  public Resource getAvailableResources();

  @Private
  @Unstable
  public void setAvailableResources(Resource limit);
  
  /**
   * Get the list of completed containers.
   * @return the list of completed containers
   */
  @Public
  @Stable
  public List<Container> getFinishedContainerList();

  @Private
  @Unstable
  public Container getFinishedContainer(int index);

  @Private
  @Unstable
  public int getFinishedContainerCount();
  

  @Private
  @Unstable
  public void addAllFinishedContainers(List<Container> containers);

  @Private
  @Unstable
  public void addFinishedContainer(Container container);

  @Private
  @Unstable
  public void removeFinishedContainer(int index);

  @Private
  @Unstable
  public void clearFinishedContainers();
}