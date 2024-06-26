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

package org.apache.hadoop.yarn.api.protocolrecords;

import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Stable;
import org.apache.hadoop.yarn.api.ContainerManagementProtocol;
import org.apache.hadoop.yarn.util.Records;

/**
 * <p>The response sent by the <code>NodeManager</code> to the 
 * <code>ApplicationMaster</code> when asked to <em>stop</em> an
 * allocated container.</p>
 * 
 * <p>Currently, this is empty.</p>
 * 
 * @see ContainerManagementProtocol#stopContainer(StopContainerRequest)
 */
@Public
@Stable
public abstract class StopContainerResponse {
  public static StopContainerResponse newInstance() {
    StopContainerResponse response =
        Records.newRecord(StopContainerResponse.class);
    return response;
  }
}
