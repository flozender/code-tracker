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

package org.apache.hadoop.yarn.server.timelineservice.reader;

import static org.junit.Assert.assertEquals;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.service.Service.STATE;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.timelineservice.reader.TimelineReaderServer;
import org.junit.Test;

public class TestTimelineReaderServer {

  @Test(timeout = 60000)
  public void testStartStopServer() throws Exception {
    @SuppressWarnings("resource")
    TimelineReaderServer server = new TimelineReaderServer();
    Configuration config = new YarnConfiguration();
    config.setBoolean(YarnConfiguration.TIMELINE_SERVICE_ENABLED, true);
    config.setFloat(YarnConfiguration.TIMELINE_SERVICE_VERSION, 2.0f);
    config.set(YarnConfiguration.TIMELINE_SERVICE_WEBAPP_ADDRESS,
        "localhost:0");
    try {
      server.init(config);
      assertEquals(STATE.INITED, server.getServiceState());
      assertEquals(2, server.getServices().size());

      server.start();
      assertEquals(STATE.STARTED, server.getServiceState());

      server.stop();
      assertEquals(STATE.STOPPED, server.getServiceState());
    } finally {
      server.stop();
    }
  }
}
