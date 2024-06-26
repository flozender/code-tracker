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

package org.apache.hadoop.yarn.server.timelineservice.storage;

import java.io.IOException;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.yarn.api.records.timelineservice.TimelineEntities;
import org.apache.hadoop.yarn.api.records.timelineservice.TimelineEntity;
import org.apache.hadoop.yarn.api.records.timelineservice.TimelineWriteResponse;
import org.apache.hadoop.service.Service;

/**
 * This interface is for storing application timeline information.
 */
@InterfaceAudience.Private
@InterfaceStability.Unstable
public interface TimelineWriter extends Service {

  /**
   * Stores the entire information in {@link TimelineEntities} to the
   * timeline store. Any errors occurring for individual write request objects
   * will be reported in the response.
   *
   * @param clusterId context cluster ID
   * @param userId context user ID
   * @param flowId context flow ID
   * @param flowRunId context flow run ID
   * @param appId context app ID
   * @param data
   *          a {@link TimelineEntities} object.
   * @return a {@link TimelineWriteResponse} object.
   * @throws IOException
   */
  TimelineWriteResponse write(String clusterId, String userId,
      String flowId, String flowRunId, String appId,
      TimelineEntities data) throws IOException;

  /**
   * Aggregates the entity information to the timeline store based on which
   * track this entity is to be rolled up to The tracks along which aggregations
   * are to be done are given by {@link TimelineAggregationTrack}
   *
   * Any errors occurring for individual write request objects will be reported
   * in the response.
   *
   * @param data
   *          a {@link TimelineEntity} object
   *          a {@link TimelineAggregationTrack} enum
   *          value.
   * @return a {@link TimelineWriteResponse} object.
   * @throws IOException
   */
  TimelineWriteResponse aggregate(TimelineEntity data,
      TimelineAggregationTrack track) throws IOException;
}