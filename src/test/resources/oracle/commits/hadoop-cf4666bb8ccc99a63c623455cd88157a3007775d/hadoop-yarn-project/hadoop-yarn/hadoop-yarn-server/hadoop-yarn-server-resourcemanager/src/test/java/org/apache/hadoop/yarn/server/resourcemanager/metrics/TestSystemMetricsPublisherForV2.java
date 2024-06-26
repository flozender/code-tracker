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

package org.apache.hadoop.yarn.server.resourcemanager.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerState;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.timelineservice.TimelineEntityType;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.event.Dispatcher;
import org.apache.hadoop.yarn.event.DrainDispatcher;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMApp;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppImpl;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppMetrics;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppState;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.RMAppAttempt;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.RMAppAttemptState;
import org.apache.hadoop.yarn.server.resourcemanager.rmcontainer.RMContainer;
import org.apache.hadoop.yarn.server.resourcemanager.timelineservice.RMTimelineCollectorManager;
import org.apache.hadoop.yarn.server.timelineservice.collector.AppLevelTimelineCollector;
import org.apache.hadoop.yarn.server.timelineservice.storage.FileSystemTimelineWriterImpl;
import org.apache.hadoop.yarn.util.timeline.TimelineUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSystemMetricsPublisherForV2 {

  /**
   * is the folder where the FileSystemTimelineWriterImpl writes the entities
   */
  protected static File testRootDir = new File("target",
      TestSystemMetricsPublisherForV2.class.getName() + "-localDir")
      .getAbsoluteFile();

  private static TimelineServiceV2Publisher metricsPublisher;
  private static DrainDispatcher dispatcher = new DrainDispatcher();
  private static final String DEFAULT_FLOW_VERSION = "1";
  private static final long DEFAULT_FLOW_RUN = 1;

  private static ConcurrentMap<ApplicationId, RMApp> rmAppsMapInContext;

  private static RMTimelineCollectorManager rmTimelineCollectorManager;

  @BeforeClass
  public static void setup() throws Exception {
    if (testRootDir.exists()) {
      //cleanup before hand
      FileContext.getLocalFSFileContext().delete(
          new Path(testRootDir.getAbsolutePath()), true);
    }

    RMContext rmContext = mock(RMContext.class);
    rmAppsMapInContext = new ConcurrentHashMap<ApplicationId, RMApp>();
    when(rmContext.getRMApps()).thenReturn(rmAppsMapInContext);
    rmTimelineCollectorManager = new RMTimelineCollectorManager(rmContext);
    when(rmContext.getRMTimelineCollectorManager()).thenReturn(
        rmTimelineCollectorManager);

    Configuration conf = getTimelineV2Conf();
    rmTimelineCollectorManager.init(conf);
    rmTimelineCollectorManager.start();

    dispatcher.init(conf);
    dispatcher.start();
    metricsPublisher = new TimelineServiceV2Publisher(rmContext) {
      @Override
      protected Dispatcher getDispatcher() {
        return dispatcher;
      }
    };
    metricsPublisher.init(conf);
    metricsPublisher.start();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (testRootDir.exists()) {
      FileContext.getLocalFSFileContext().delete(
          new Path(testRootDir.getAbsolutePath()), true);
    }
    if (rmTimelineCollectorManager != null) {
      rmTimelineCollectorManager.stop();
    }
    if (metricsPublisher != null) {
      metricsPublisher.stop();
    }
  }

  private static Configuration getTimelineV2Conf() {
    Configuration conf = new Configuration();
    conf.setBoolean(YarnConfiguration.TIMELINE_SERVICE_ENABLED, true);
    conf.setBoolean(YarnConfiguration.SYSTEM_METRICS_PUBLISHER_ENABLED, true);
    conf.setInt(
        YarnConfiguration.RM_SYSTEM_METRICS_PUBLISHER_DISPATCHER_POOL_SIZE, 2);
    conf.setBoolean(YarnConfiguration.RM_PUBLISH_CONTAINER_METRICS_ENABLED,
        true);
    try {
      conf.set(FileSystemTimelineWriterImpl.TIMELINE_SERVICE_STORAGE_DIR_ROOT,
          testRootDir.getCanonicalPath());
    } catch (IOException e) {
      e.printStackTrace();
      Assert
          .fail("Exception while setting the TIMELINE_SERVICE_STORAGE_DIR_ROOT ");
    }
    return conf;
  }

  @Test
  public void testSystemMetricPublisherInitialization() {
    @SuppressWarnings("resource")
    TimelineServiceV2Publisher metricsPublisher =
        new TimelineServiceV2Publisher(mock(RMContext.class));
    try {
      Configuration conf = getTimelineV2Conf();
      conf.setBoolean(YarnConfiguration.RM_PUBLISH_CONTAINER_METRICS_ENABLED,
          false);
      metricsPublisher.init(conf);
      assertFalse(
          "Default configuration should not publish container Metrics from RM",
          metricsPublisher.isPublishContainerMetrics());

      metricsPublisher.stop();

      metricsPublisher = new TimelineServiceV2Publisher(mock(RMContext.class));
      conf = getTimelineV2Conf();
      metricsPublisher.init(conf);
      metricsPublisher.start();
      assertTrue("Expected to publish container Metrics from RM",
          metricsPublisher.isPublishContainerMetrics());
    } finally {
      metricsPublisher.stop();
    }
  }

  @Test(timeout = 10000)
  public void testPublishApplicationMetrics() throws Exception {
    ApplicationId appId = ApplicationId.newInstance(0, 1);
    RMApp app = createAppAndRegister(appId);

    metricsPublisher.appCreated(app, app.getStartTime());
    metricsPublisher.appACLsUpdated(app, "user1,user2", 4L);
    metricsPublisher.appFinished(app, RMAppState.FINISHED, app.getFinishTime());
    dispatcher.await();

    String outputDirApp =
        getTimelineEntityDir(app) + "/" + TimelineEntityType.YARN_APPLICATION
            + "/";

    File entityFolder = new File(outputDirApp);
    Assert.assertTrue(entityFolder.isDirectory());

    // file name is <entityId>.thist
    String timelineServiceFileName =
        appId.toString()
            + FileSystemTimelineWriterImpl.TIMELINE_SERVICE_STORAGE_EXTENSION;
    File appFile = new File(outputDirApp, timelineServiceFileName);
    Assert.assertTrue(appFile.exists());
    Assert.assertEquals("Expected 3 events to be published", 3,
        getNumOfNonEmptyLines(appFile));
  }

  @Test(timeout = 10000)
  public void testPublishAppAttemptMetrics() throws Exception {
    ApplicationId appId = ApplicationId.newInstance(0, 1);
    RMApp app = rmAppsMapInContext.get(appId);
    if (app == null) {
      app = createAppAndRegister(appId);
    }
    ApplicationAttemptId appAttemptId =
        ApplicationAttemptId.newInstance(appId, 1);
    RMAppAttempt appAttempt = createRMAppAttempt(appAttemptId);
    metricsPublisher.appAttemptRegistered(appAttempt, Integer.MAX_VALUE + 1L);
    when(app.getFinalApplicationStatus()).thenReturn(
        FinalApplicationStatus.UNDEFINED);
    metricsPublisher.appAttemptFinished(appAttempt, RMAppAttemptState.FINISHED,
        app, Integer.MAX_VALUE + 2L);

    dispatcher.await();

    String outputDirApp =
        getTimelineEntityDir(app) + "/"
            + TimelineEntityType.YARN_APPLICATION_ATTEMPT + "/";

    File entityFolder = new File(outputDirApp);
    Assert.assertTrue(entityFolder.isDirectory());

    // file name is <entityId>.thist
    String timelineServiceFileName =
        appAttemptId.toString()
            + FileSystemTimelineWriterImpl.TIMELINE_SERVICE_STORAGE_EXTENSION;
    File appFile = new File(outputDirApp, timelineServiceFileName);
    Assert.assertTrue(appFile.exists());
    Assert.assertEquals("Expected 2 events to be published", 2,
        getNumOfNonEmptyLines(appFile));
  }

  @Test(timeout = 10000)
  public void testPublishContainerMetrics() throws Exception {
    ApplicationId appId = ApplicationId.newInstance(0, 1);
    RMApp app = rmAppsMapInContext.get(appId);
    if (app == null) {
      app = createAppAndRegister(appId);
    }
    ContainerId containerId =
        ContainerId.newContainerId(ApplicationAttemptId.newInstance(
            appId, 1), 1);
    RMContainer container = createRMContainer(containerId);
    metricsPublisher.containerCreated(container, container.getCreationTime());
    metricsPublisher.containerFinished(container, container.getFinishTime());
    dispatcher.await();

    String outputDirApp =
        getTimelineEntityDir(app) + "/"
            + TimelineEntityType.YARN_CONTAINER + "/";

    File entityFolder = new File(outputDirApp);
    Assert.assertTrue(entityFolder.isDirectory());

    // file name is <entityId>.thist
    String timelineServiceFileName =
        containerId.toString()
            + FileSystemTimelineWriterImpl.TIMELINE_SERVICE_STORAGE_EXTENSION;
    File appFile = new File(outputDirApp, timelineServiceFileName);
    Assert.assertTrue(appFile.exists());
    Assert.assertEquals("Expected 2 events to be published", 2,
        getNumOfNonEmptyLines(appFile));
  }

  private RMApp createAppAndRegister(ApplicationId appId) {
    RMApp app = createRMApp(appId);

    // some stuff which are currently taken care in RMAppImpl
    rmAppsMapInContext.putIfAbsent(appId, app);
    AppLevelTimelineCollector collector = new AppLevelTimelineCollector(appId);
    rmTimelineCollectorManager.putIfAbsent(appId, collector);
    return app;
  }

  private long getNumOfNonEmptyLines(File entityFile) throws IOException {
    BufferedReader reader = null;
    String strLine;
    long count = 0;
    try {
      reader = new BufferedReader(new FileReader(entityFile));
      while ((strLine = reader.readLine()) != null) {
        if (strLine.trim().length() > 0)
          count++;
      }
    } finally {
      reader.close();
    }
    return count;
  }

  private String getTimelineEntityDir(RMApp app) {
    String outputDirApp =
        testRootDir.getAbsolutePath()+"/"
            + FileSystemTimelineWriterImpl.ENTITIES_DIR
            + "/"
            + YarnConfiguration.DEFAULT_RM_CLUSTER_ID
            + "/"
            + app.getUser()
            + "/"
            + TimelineUtils.generateDefaultFlowIdBasedOnAppId(app
                .getApplicationId()) + "/" + DEFAULT_FLOW_VERSION + "/"
            + DEFAULT_FLOW_RUN + "/" + app.getApplicationId();
    return outputDirApp;
  }

  private static RMApp createRMApp(ApplicationId appId) {
    RMApp app = mock(RMAppImpl.class);
    when(app.getApplicationId()).thenReturn(appId);
    when(app.getName()).thenReturn("test app");
    when(app.getApplicationType()).thenReturn("test app type");
    when(app.getUser()).thenReturn("testUser");
    when(app.getQueue()).thenReturn("test queue");
    when(app.getSubmitTime()).thenReturn(Integer.MAX_VALUE + 1L);
    when(app.getStartTime()).thenReturn(Integer.MAX_VALUE + 2L);
    when(app.getFinishTime()).thenReturn(Integer.MAX_VALUE + 3L);
    when(app.getDiagnostics()).thenReturn(
        new StringBuilder("test diagnostics info"));
    RMAppAttempt appAttempt = mock(RMAppAttempt.class);
    when(appAttempt.getAppAttemptId()).thenReturn(
        ApplicationAttemptId.newInstance(appId, 1));
    when(app.getCurrentAppAttempt()).thenReturn(appAttempt);
    when(app.getFinalApplicationStatus()).thenReturn(
        FinalApplicationStatus.UNDEFINED);
    when(app.getRMAppMetrics()).thenReturn(
        new RMAppMetrics(null, 0, 0, Integer.MAX_VALUE, Long.MAX_VALUE));
    when(app.getApplicationTags()).thenReturn(Collections.<String> emptySet());
    ApplicationSubmissionContext appSubmissionContext =
        mock(ApplicationSubmissionContext.class);
    when(appSubmissionContext.getPriority())
        .thenReturn(Priority.newInstance(0));
    when(app.getApplicationSubmissionContext())
        .thenReturn(appSubmissionContext);
    return app;
  }

  private static RMAppAttempt createRMAppAttempt(
      ApplicationAttemptId appAttemptId) {
    RMAppAttempt appAttempt = mock(RMAppAttempt.class);
    when(appAttempt.getAppAttemptId()).thenReturn(appAttemptId);
    when(appAttempt.getHost()).thenReturn("test host");
    when(appAttempt.getRpcPort()).thenReturn(-100);
    Container container = mock(Container.class);
    when(container.getId()).thenReturn(
        ContainerId.newContainerId(appAttemptId, 1));
    when(appAttempt.getMasterContainer()).thenReturn(container);
    when(appAttempt.getDiagnostics()).thenReturn("test diagnostics info");
    when(appAttempt.getTrackingUrl()).thenReturn("test tracking url");
    when(appAttempt.getOriginalTrackingUrl()).thenReturn(
        "test original tracking url");
    return appAttempt;
  }

  private static RMContainer createRMContainer(ContainerId containerId) {
    RMContainer container = mock(RMContainer.class);
    when(container.getContainerId()).thenReturn(containerId);
    when(container.getAllocatedNode()).thenReturn(
        NodeId.newInstance("test host", -100));
    when(container.getAllocatedResource()).thenReturn(
        Resource.newInstance(-1, -1));
    when(container.getAllocatedPriority()).thenReturn(Priority.UNDEFINED);
    when(container.getCreationTime()).thenReturn(Integer.MAX_VALUE + 1L);
    when(container.getFinishTime()).thenReturn(Integer.MAX_VALUE + 2L);
    when(container.getDiagnosticsInfo()).thenReturn("test diagnostics info");
    when(container.getContainerExitStatus()).thenReturn(-1);
    when(container.getContainerState()).thenReturn(ContainerState.COMPLETE);
    Container mockContainer = mock(Container.class);
    when(container.getContainer()).thenReturn(mockContainer);
    when(mockContainer.getNodeHttpAddress())
      .thenReturn("http://localhost:1234");
    return container;
  }
}
