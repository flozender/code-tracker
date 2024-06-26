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

package org.apache.hadoop.yarn.service;

import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FilterService implements Service {

  private final Service service;
  private final long startTime = System.currentTimeMillis();

  public FilterService(Service service) {
    this.service = service;
  }

  @Override
  public void init(Configuration config) {
    service.init(config);
  }

  @Override
  public void start() {
    service.start();
  }

  @Override
  public void stop() {
    service.stop();
  }

  @Override
  public void close() throws IOException {
    service.close();
  }

  @Override
  public void register(ServiceStateChangeListener listener) {
    service.register(listener);
  }

  @Override
  public void unregister(ServiceStateChangeListener listener) {
    service.unregister(listener);
  }

  @Override
  public String getName() {
    return service.getName();
  }

  @Override
  public Configuration getConfig() {
    return service.getConfig();
  }

  @Override
  public STATE getServiceState() {
    return service.getServiceState();
  }

  @Override
  public long getStartTime() {
    return startTime;
  }

  @Override
  public boolean isInState(STATE state) {
    return service.isInState(state);
  }

  @Override
  public Throwable getFailureCause() {
    return service.getFailureCause();
  }

  @Override
  public STATE getFailureState() {
    return service.getFailureState();
  }

  @Override
  public boolean waitForServiceToStop(long timeout) {
    return service.waitForServiceToStop(timeout);
  }

  @Override
  public List<LifecycleEvent> getLifecycleHistory() {
    return service.getLifecycleHistory();
  }

  @Override
  public Map<String, String> getBlockers() {
    return service.getBlockers();
  }
}
