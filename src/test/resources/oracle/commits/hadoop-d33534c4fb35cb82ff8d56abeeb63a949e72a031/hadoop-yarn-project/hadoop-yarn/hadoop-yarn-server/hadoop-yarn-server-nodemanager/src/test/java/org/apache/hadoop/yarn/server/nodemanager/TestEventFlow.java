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

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StopContainerRequest;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerState;
import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.event.AsyncDispatcher;
import org.apache.hadoop.yarn.event.Dispatcher;
import org.apache.hadoop.yarn.exceptions.YarnRemoteException;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.server.api.ResourceTracker;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager.NMContext;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.BaseContainerManagerTest;
import org.apache.hadoop.yarn.server.nodemanager.metrics.NodeManagerMetrics;
import org.apache.hadoop.yarn.server.nodemanager.security.NMContainerTokenSecretManager;
import org.apache.hadoop.yarn.server.security.ApplicationACLsManager;
import org.apache.hadoop.yarn.server.utils.BuilderUtils;
import org.junit.Test;


public class TestEventFlow {

  private static final RecordFactory recordFactory =
      RecordFactoryProvider.getRecordFactory(null);

  private static File localDir = new File("target",
      TestEventFlow.class.getName() + "-localDir").getAbsoluteFile();
  private static File localLogDir = new File("target",
      TestEventFlow.class.getName() + "-localLogDir").getAbsoluteFile();
  private static File remoteLogDir = new File("target",
      TestEventFlow.class.getName() + "-remoteLogDir").getAbsoluteFile();
  private static final long SIMULATED_RM_IDENTIFIER = 1234;

  @Test
  public void testSuccessfulContainerLaunch() throws InterruptedException,
      IOException, YarnRemoteException {

    FileContext localFS = FileContext.getLocalFSFileContext();

    localFS.delete(new Path(localDir.getAbsolutePath()), true);
    localFS.delete(new Path(localLogDir.getAbsolutePath()), true);
    localFS.delete(new Path(remoteLogDir.getAbsolutePath()), true);
    localDir.mkdir();
    localLogDir.mkdir();
    remoteLogDir.mkdir();

    YarnConfiguration conf = new YarnConfiguration();
    
    Context context = new NMContext(new NMContainerTokenSecretManager(conf)) {
      @Override
      public int getHttpPort() {
        return 1234;
      }
    };

    conf.set(YarnConfiguration.NM_LOCAL_DIRS, localDir.getAbsolutePath());
    conf.set(YarnConfiguration.NM_LOG_DIRS, localLogDir.getAbsolutePath());
    conf.set(YarnConfiguration.NM_REMOTE_APP_LOG_DIR, 
        remoteLogDir.getAbsolutePath());

    ContainerExecutor exec = new DefaultContainerExecutor();
    exec.setConf(conf);

    DeletionService del = new DeletionService(exec);
    Dispatcher dispatcher = new AsyncDispatcher();
    NodeHealthCheckerService healthChecker = new NodeHealthCheckerService();
    healthChecker.init(conf);
    LocalDirsHandlerService dirsHandler = healthChecker.getDiskHandler();
    NodeManagerMetrics metrics = NodeManagerMetrics.create();
    NodeStatusUpdater nodeStatusUpdater =
        new NodeStatusUpdaterImpl(context, dispatcher, healthChecker, metrics) {
      @Override
      protected ResourceTracker getRMClient() {
        return new LocalRMInterface();
      };

      @Override
      protected void startStatusUpdater() {
        return; // Don't start any updating thread.
      }

      @Override
      public long getRMIdentifier() {
        return SIMULATED_RM_IDENTIFIER;
      }
    };

    DummyContainerManager containerManager =
        new DummyContainerManager(context, exec, del, nodeStatusUpdater,
          metrics, new ApplicationACLsManager(conf), dirsHandler);
    nodeStatusUpdater.init(conf);
    ((NMContext)context).setContainerManager(containerManager);
    nodeStatusUpdater.start();
    containerManager.init(conf);
    containerManager.start();

    ContainerLaunchContext launchContext = 
        recordFactory.newRecordInstance(ContainerLaunchContext.class);
    ApplicationId applicationId = ApplicationId.newInstance(0, 0);
    ApplicationAttemptId applicationAttemptId =
        ApplicationAttemptId.newInstance(applicationId, 0);
    ContainerId cID = ContainerId.newInstance(applicationAttemptId, 0);

    Resource r = BuilderUtils.newResource(1024, 1);
    String user = "testing";
    String host = "127.0.0.1";
    int port = 1234;
    Token containerToken =
        BuilderUtils.newContainerToken(cID, host, port, user, r,
          System.currentTimeMillis() + 10000L, 123, "password".getBytes(),
          SIMULATED_RM_IDENTIFIER);
    StartContainerRequest request = 
        recordFactory.newRecordInstance(StartContainerRequest.class);
    request.setContainerLaunchContext(launchContext);
    request.setContainerToken(containerToken);
    containerManager.startContainer(request);

    BaseContainerManagerTest.waitForContainerState(containerManager, cID,
        ContainerState.RUNNING);

    StopContainerRequest stopRequest = 
        recordFactory.newRecordInstance(StopContainerRequest.class);
    stopRequest.setContainerId(cID);
    containerManager.stopContainer(stopRequest);
    BaseContainerManagerTest.waitForContainerState(containerManager, cID,
        ContainerState.COMPLETE);

    containerManager.stop();
  }
}
