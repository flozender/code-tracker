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

package org.apache.hadoop.yarn.server.timelineservice.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.server.api.CollectorNodemanagerProtocol;
import org.apache.hadoop.yarn.server.api.ContainerInitializationContext;
import org.apache.hadoop.yarn.server.api.ContainerTerminationContext;
import org.apache.hadoop.yarn.server.api.protocolrecords.GetTimelineCollectorContextRequest;
import org.apache.hadoop.yarn.server.api.protocolrecords.GetTimelineCollectorContextResponse;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

public class TestPerNodeTimelineCollectorsAuxService {
  private ApplicationAttemptId appAttemptId;
  private PerNodeTimelineCollectorsAuxService auxService;

  public TestPerNodeTimelineCollectorsAuxService() {
    ApplicationId appId =
        ApplicationId.newInstance(System.currentTimeMillis(), 1);
    appAttemptId = ApplicationAttemptId.newInstance(appId, 1);
  }

  @After
  public void tearDown() throws Shell.ExitCodeException {
    if (auxService != null) {
      auxService.stop();
    }
  }

  @Test
  public void testAddApplication() throws Exception {
    auxService = createCollectorAndAddApplication();
    // auxService should have a single app
    assertTrue(auxService.hasApplication(
        appAttemptId.getApplicationId().toString()));
    auxService.close();
  }

  @Test
  public void testAddApplicationNonAMContainer() throws Exception {
    auxService = createCollector();

    ContainerId containerId = getContainerId(2L); // not an AM
    ContainerInitializationContext context =
        mock(ContainerInitializationContext.class);
    when(context.getContainerId()).thenReturn(containerId);
    auxService.initializeContainer(context);
    // auxService should not have that app
    assertFalse(auxService.hasApplication(
        appAttemptId.getApplicationId().toString()));
  }

  @Test
  public void testRemoveApplication() throws Exception {
    auxService = createCollectorAndAddApplication();
    // auxService should have a single app
    String appIdStr = appAttemptId.getApplicationId().toString();
    assertTrue(auxService.hasApplication(appIdStr));

    ContainerId containerId = getAMContainerId();
    ContainerTerminationContext context =
        mock(ContainerTerminationContext.class);
    when(context.getContainerId()).thenReturn(containerId);
    auxService.stopContainer(context);
    // auxService should not have that app
    assertFalse(auxService.hasApplication(appIdStr));
    auxService.close();
  }

  @Test
  public void testRemoveApplicationNonAMContainer() throws Exception {
    auxService = createCollectorAndAddApplication();
    // auxService should have a single app
    String appIdStr = appAttemptId.getApplicationId().toString();
    assertTrue(auxService.hasApplication(appIdStr));

    ContainerId containerId = getContainerId(2L); // not an AM
    ContainerTerminationContext context =
        mock(ContainerTerminationContext.class);
    when(context.getContainerId()).thenReturn(containerId);
    auxService.stopContainer(context);
    // auxService should still have that app
    assertTrue(auxService.hasApplication(appIdStr));
    auxService.close();
  }

  @Test(timeout = 60000)
  public void testLaunch() throws Exception {
    ExitUtil.disableSystemExit();
    try {
      auxService =
          PerNodeTimelineCollectorsAuxService.launchServer(new String[0],
              createCollectorManager());
    } catch (ExitUtil.ExitException e) {
      assertEquals(0, e.status);
      ExitUtil.resetFirstExitException();
      fail();
    }
  }

  private PerNodeTimelineCollectorsAuxService
      createCollectorAndAddApplication() {
    PerNodeTimelineCollectorsAuxService auxService = createCollector();
    // create an AM container
    ContainerId containerId = getAMContainerId();
    ContainerInitializationContext context =
        mock(ContainerInitializationContext.class);
    when(context.getContainerId()).thenReturn(containerId);
    auxService.initializeContainer(context);
    return auxService;
  }

  private PerNodeTimelineCollectorsAuxService createCollector() {
    TimelineCollectorManager collectorManager = createCollectorManager();
    PerNodeTimelineCollectorsAuxService auxService =
        spy(new PerNodeTimelineCollectorsAuxService(collectorManager));
    auxService.init(new YarnConfiguration());
    auxService.start();
    return auxService;
  }

  private TimelineCollectorManager createCollectorManager() {
    TimelineCollectorManager collectorManager =
        spy(new TimelineCollectorManager());
    doReturn(new Configuration()).when(collectorManager).getConfig();
    CollectorNodemanagerProtocol nmCollectorService =
        mock(CollectorNodemanagerProtocol.class);
    GetTimelineCollectorContextResponse response =
        GetTimelineCollectorContextResponse.newInstance(null, null, null, 0L);
    try {
      when(nmCollectorService.getTimelineCollectorContext(any(
          GetTimelineCollectorContextRequest.class))).thenReturn(response);
    } catch (YarnException | IOException e) {
      fail();
    }
    doReturn(nmCollectorService).when(collectorManager).getNMCollectorService();
    return collectorManager;
  }

  private ContainerId getAMContainerId() {
    return getContainerId(1L);
  }

  private ContainerId getContainerId(long id) {
    return ContainerId.newContainerId(appAttemptId, id);
  }
}
