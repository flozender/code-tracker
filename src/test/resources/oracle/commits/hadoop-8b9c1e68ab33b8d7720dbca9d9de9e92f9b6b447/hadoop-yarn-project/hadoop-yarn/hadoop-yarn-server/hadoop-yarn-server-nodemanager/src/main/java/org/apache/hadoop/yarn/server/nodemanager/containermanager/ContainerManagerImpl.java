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

package org.apache.hadoop.yarn.server.nodemanager.containermanager;

import static org.apache.hadoop.service.Service.STATE.STARTED;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.io.DataInputByteBuffer;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.PolicyProvider;
import org.apache.hadoop.security.token.SecretManager.InvalidToken;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.service.CompositeService;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.service.ServiceStateChangeListener;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.yarn.api.ContainerManagementProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.GetContainerStatusRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetContainerStatusResponse;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainerResponse;
import org.apache.hadoop.yarn.api.protocolrecords.StopContainerRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StopContainerResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.event.AsyncDispatcher;
import org.apache.hadoop.yarn.event.EventHandler;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.ipc.RPCUtil;
import org.apache.hadoop.yarn.ipc.YarnRPC;
import org.apache.hadoop.yarn.security.ContainerTokenIdentifier;
import org.apache.hadoop.yarn.security.NMTokenIdentifier;
import org.apache.hadoop.yarn.server.nodemanager.CMgrCompletedAppsEvent;
import org.apache.hadoop.yarn.server.nodemanager.CMgrCompletedContainersEvent;
import org.apache.hadoop.yarn.server.nodemanager.ContainerExecutor;
import org.apache.hadoop.yarn.server.nodemanager.ContainerManagerEvent;
import org.apache.hadoop.yarn.server.nodemanager.Context;
import org.apache.hadoop.yarn.server.nodemanager.DeletionService;
import org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerService;
import org.apache.hadoop.yarn.server.nodemanager.NMAuditLogger;
import org.apache.hadoop.yarn.server.nodemanager.NMAuditLogger.AuditConstants;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager;
import org.apache.hadoop.yarn.server.nodemanager.NodeStatusUpdater;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.application.Application;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.application.ApplicationContainerInitEvent;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.application.ApplicationEvent;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.application.ApplicationEventType;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.application.ApplicationFinishEvent;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.application.ApplicationImpl;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.application.ApplicationInitEvent;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.Container;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerEvent;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerEventType;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerImpl;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerKillEvent;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.launcher.ContainersLauncher;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.launcher.ContainersLauncherEventType;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.localizer.ResourceLocalizationService;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.localizer.event.LocalizationEventType;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.logaggregation.LogAggregationService;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.loghandler.LogHandler;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.loghandler.NonAggregatingLogHandler;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.loghandler.event.LogHandlerEventType;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.monitor.ContainersMonitor;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.monitor.ContainersMonitorEventType;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.monitor.ContainersMonitorImpl;
import org.apache.hadoop.yarn.server.nodemanager.metrics.NodeManagerMetrics;
import org.apache.hadoop.yarn.server.nodemanager.security.authorize.NMPolicyProvider;
import org.apache.hadoop.yarn.server.security.ApplicationACLsManager;
import org.apache.hadoop.yarn.server.utils.BuilderUtils;

import com.google.common.annotations.VisibleForTesting;

public class ContainerManagerImpl extends CompositeService implements
    ServiceStateChangeListener, ContainerManagementProtocol,
    EventHandler<ContainerManagerEvent> {

  private static final Log LOG = LogFactory.getLog(ContainerManagerImpl.class);

  final Context context;
  private final ContainersMonitor containersMonitor;
  private Server server;
  private final ResourceLocalizationService rsrcLocalizationSrvc;
  private final ContainersLauncher containersLauncher;
  private final AuxServices auxiliaryServices;
  private final NodeManagerMetrics metrics;

  private final NodeStatusUpdater nodeStatusUpdater;

  private final RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);

  protected LocalDirsHandlerService dirsHandler;
  protected final AsyncDispatcher dispatcher;
  private final ApplicationACLsManager aclsManager;

  private final DeletionService deletionService;
  private AtomicBoolean blockNewContainerRequests = new AtomicBoolean(false);

  public ContainerManagerImpl(Context context, ContainerExecutor exec,
      DeletionService deletionContext, NodeStatusUpdater nodeStatusUpdater,
      NodeManagerMetrics metrics, ApplicationACLsManager aclsManager,
      LocalDirsHandlerService dirsHandler) {
    super(ContainerManagerImpl.class.getName());
    this.context = context;
    this.dirsHandler = dirsHandler;

    // ContainerManager level dispatcher.
    dispatcher = new AsyncDispatcher();
    this.deletionService = deletionContext;
    this.metrics = metrics;

    rsrcLocalizationSrvc =
        createResourceLocalizationService(exec, deletionContext);
    addService(rsrcLocalizationSrvc);

    containersLauncher = createContainersLauncher(context, exec);
    addService(containersLauncher);

    this.nodeStatusUpdater = nodeStatusUpdater;
    this.aclsManager = aclsManager;

    // Start configurable services
    auxiliaryServices = new AuxServices();
    auxiliaryServices.registerServiceListener(this);
    addService(auxiliaryServices);

    this.containersMonitor =
        new ContainersMonitorImpl(exec, dispatcher, this.context);
    addService(this.containersMonitor);

    dispatcher.register(ContainerEventType.class,
        new ContainerEventDispatcher());
    dispatcher.register(ApplicationEventType.class,
        new ApplicationEventDispatcher());
    dispatcher.register(LocalizationEventType.class, rsrcLocalizationSrvc);
    dispatcher.register(AuxServicesEventType.class, auxiliaryServices);
    dispatcher.register(ContainersMonitorEventType.class, containersMonitor);
    dispatcher.register(ContainersLauncherEventType.class, containersLauncher);
    
    addService(dispatcher);
  }

  @Override
  public void serviceInit(Configuration conf) throws Exception {
    LogHandler logHandler =
      createLogHandler(conf, this.context, this.deletionService);
    addIfService(logHandler);
    dispatcher.register(LogHandlerEventType.class, logHandler);
    
    super.serviceInit(conf);
  }

  private void addIfService(Object object) {
    if (object instanceof Service) {
      addService((Service) object);
    }
  }

  protected LogHandler createLogHandler(Configuration conf, Context context,
      DeletionService deletionService) {
    if (conf.getBoolean(YarnConfiguration.LOG_AGGREGATION_ENABLED,
        YarnConfiguration.DEFAULT_LOG_AGGREGATION_ENABLED)) {
      return new LogAggregationService(this.dispatcher, context,
          deletionService, dirsHandler);
    } else {
      return new NonAggregatingLogHandler(this.dispatcher, deletionService,
                                          dirsHandler);
    }
  }

  public ContainersMonitor getContainersMonitor() {
    return this.containersMonitor;
  }

  protected ResourceLocalizationService createResourceLocalizationService(
      ContainerExecutor exec, DeletionService deletionContext) {
    return new ResourceLocalizationService(this.dispatcher, exec,
        deletionContext, dirsHandler);
  }

  protected ContainersLauncher createContainersLauncher(Context context,
      ContainerExecutor exec) {
    return new ContainersLauncher(context, this.dispatcher, exec, dirsHandler);
  }

  @Override
  protected void serviceStart() throws Exception {

    // Enqueue user dirs in deletion context

    Configuration conf = getConfig();
    YarnRPC rpc = YarnRPC.create(conf);

    InetSocketAddress initialAddress = conf.getSocketAddr(
        YarnConfiguration.NM_ADDRESS,
        YarnConfiguration.DEFAULT_NM_ADDRESS,
        YarnConfiguration.DEFAULT_NM_PORT);

    server =
        rpc.getServer(ContainerManagementProtocol.class, this, initialAddress, conf,
            this.context.getNMTokenSecretManager(),
            conf.getInt(YarnConfiguration.NM_CONTAINER_MGR_THREAD_COUNT, 
                YarnConfiguration.DEFAULT_NM_CONTAINER_MGR_THREAD_COUNT));
    
    // Enable service authorization?
    if (conf.getBoolean(
        CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHORIZATION, 
        false)) {
      refreshServiceAcls(conf, new NMPolicyProvider());
    }

    LOG.info("Blocking new container-requests as container manager rpc" +
    		" server is still starting.");
    this.setBlockNewContainerRequests(true);
    server.start();
    InetSocketAddress connectAddress = NetUtils.getConnectAddress(server);
    NodeId nodeId = NodeId.newInstance(
        connectAddress.getAddress().getCanonicalHostName(),
        connectAddress.getPort());
    ((NodeManager.NMContext)context).setNodeId(nodeId);
    this.context.getNMTokenSecretManager().setNodeId(nodeId);
    this.context.getContainerTokenSecretManager().setNodeId(nodeId);
    LOG.info("ContainerManager started at " + connectAddress);
    super.serviceStart();
  }

  void refreshServiceAcls(Configuration configuration, 
      PolicyProvider policyProvider) {
    this.server.refreshServiceAcl(configuration, policyProvider);
  }

  @Override
  public void serviceStop() throws Exception {
    if (auxiliaryServices.getServiceState() == STARTED) {
      auxiliaryServices.unregisterServiceListener(this);
    }
    if (server != null) {
      server.stop();
    }
    super.serviceStop();
  }

  // Get the remoteUGI corresponding to the api call.
  protected UserGroupInformation getRemoteUgi()
      throws YarnException {
    UserGroupInformation remoteUgi;
    try {
      remoteUgi = UserGroupInformation.getCurrentUser();
    } catch (IOException e) {
      String msg = "Cannot obtain the user-name. Got exception: "
          + StringUtils.stringifyException(e);
      LOG.warn(msg);
      throw RPCUtil.getRemoteException(msg);
    }
    return remoteUgi;
  }

  // Obtain the needed ContainerTokenIdentifier from the remote-UGI. RPC layer
  // currently sets only the required id, but iterate through anyways just to
  // be sure.
  @Private
  @VisibleForTesting
  protected NMTokenIdentifier selectNMTokenIdentifier(
      UserGroupInformation remoteUgi) {
    Set<TokenIdentifier> tokenIdentifiers = remoteUgi.getTokenIdentifiers();
    NMTokenIdentifier resultId = null;
    for (TokenIdentifier id : tokenIdentifiers) {
      if (id instanceof NMTokenIdentifier) {
        resultId = (NMTokenIdentifier) id;
        break;
      }
    }
    return resultId;
  }

  /**
   * @param containerTokenIdentifier
   *          of the container to be started
   * @param ugi
   *          ugi corresponding to the remote end making the api-call
   * @throws YarnException
   */
  @Private
  @VisibleForTesting
  protected void authorizeStartRequest(NMTokenIdentifier nmTokenIdentifier,
      ContainerTokenIdentifier containerTokenIdentifier,
      UserGroupInformation ugi) throws YarnException {

    ContainerId containerId = containerTokenIdentifier.getContainerID();
    String containerIDStr = containerId.toString();
    boolean unauthorized = false;
    StringBuilder messageBuilder =
        new StringBuilder("Unauthorized request to start container. ");
    if (!nmTokenIdentifier.getApplicationAttemptId().equals(
        containerId.getApplicationAttemptId())) {
      unauthorized = true;
      messageBuilder.append("\nNMToken for application attempt : ")
        .append(nmTokenIdentifier.getApplicationAttemptId())
        .append(" was used for starting container with container token")
        .append(" issued for application attempt : ")
        .append(containerId.getApplicationAttemptId());
    } else if (!ugi.getUserName().equals(
        nmTokenIdentifier.getApplicationAttemptId().toString())) {
      unauthorized = true;
      messageBuilder.append("\nExpected applicationAttemptId: ")
        .append(ugi.getUserName()).append(" Found: ")
        .append(nmTokenIdentifier.getApplicationAttemptId().toString());
    } else if (!this.context.getContainerTokenSecretManager()
        .isValidStartContainerRequest(containerTokenIdentifier)) {
      // Is the container being relaunched? Or RPC layer let startCall with
      // tokens generated off old-secret through?
      unauthorized = true;
      messageBuilder.append("\n Attempt to relaunch the same ")
        .append("container with id ").append(containerIDStr).append(".");
    } else if (containerTokenIdentifier.getExpiryTimeStamp() < System
      .currentTimeMillis()) {
      // Ensure the token is not expired.
      unauthorized = true;
      messageBuilder.append("\nThis token is expired. current time is ")
        .append(System.currentTimeMillis()).append(" found ")
        .append(containerTokenIdentifier.getExpiryTimeStamp());
    }

    if (unauthorized) {
      String msg = messageBuilder.toString();
      LOG.error(msg);
      throw RPCUtil.getRemoteException(msg);
    }
  }

  /**
   * Start a container on this NodeManager.
   */
  @SuppressWarnings("unchecked")
  @Override
  public StartContainerResponse startContainer(StartContainerRequest request)
      throws YarnException, IOException {

    if (blockNewContainerRequests.get()) {
      throw new NMNotYetReadyException(
        "Rejecting new containers as NodeManager has not"
            + " yet connected with ResourceManager");
    }
    /*
     * 1) It should save the NMToken into NMTokenSecretManager. This is done
     * here instead of RPC layer because at the time of opening/authenticating
     * the connection it doesn't know what all RPC calls user will make on it.
     * Also new NMToken is issued only at startContainer (once it gets renewed).
     * 
     * 2) It should validate containerToken. Need to check below things. a) It
     * is signed by correct master key (part of retrieve password). b) It
     * belongs to correct Node Manager (part of retrieve password). c) It has
     * correct RMIdentifier. d) It is not expired.
     */
    // update NMToken

    UserGroupInformation remoteUgi = getRemoteUgi();
    NMTokenIdentifier nmTokenIdentifier = selectNMTokenIdentifier(remoteUgi);
    
    // Validate containerToken
    ContainerTokenIdentifier containerTokenIdentifier =
        verifyAndGetContainerTokenIdentifier(request.getContainerToken());

    authorizeStartRequest(nmTokenIdentifier, containerTokenIdentifier,
      remoteUgi);

    if (containerTokenIdentifier.getRMIdentifer() != nodeStatusUpdater
        .getRMIdentifier()) {
        // Is the container coming from unknown RM
        StringBuilder sb = new StringBuilder("\nContainer ");
        sb.append(containerTokenIdentifier.getContainerID().toString())
          .append(" rejected as it is allocated by a previous RM");
        throw new InvalidContainerException(sb.toString());
    }
    
    updateNMTokenIdentifier(nmTokenIdentifier);
    
    ContainerId containerId = containerTokenIdentifier.getContainerID();
    String containerIdStr = containerId.toString();
    String user = containerTokenIdentifier.getApplicationSubmitter();

    LOG.info("Start request for " + containerIdStr + " by user " + user);

    ContainerLaunchContext launchContext = request.getContainerLaunchContext();

    Credentials credentials = parseCredentials(launchContext);

    Container container =
        new ContainerImpl(getConfig(), this.dispatcher, launchContext,
          credentials, metrics, containerTokenIdentifier);
    ApplicationId applicationID =
        containerId.getApplicationAttemptId().getApplicationId();
    if (context.getContainers().putIfAbsent(containerId, container) != null) {
      NMAuditLogger.logFailure(user, AuditConstants.START_CONTAINER,
        "ContainerManagerImpl", "Container already running on this node!",
        applicationID, containerId);
      throw RPCUtil.getRemoteException("Container " + containerIdStr
          + " already is running on this node!!");
    }

    // Create the application
    Application application =
        new ApplicationImpl(dispatcher, this.aclsManager, user, applicationID,
          credentials, context);
    if (null == context.getApplications().putIfAbsent(applicationID,
      application)) {
      LOG.info("Creating a new application reference for app " + applicationID);

      dispatcher.getEventHandler().handle(
        new ApplicationInitEvent(applicationID, container.getLaunchContext()
          .getApplicationACLs()));
    }

    dispatcher.getEventHandler().handle(
      new ApplicationContainerInitEvent(container));

    this.context.getContainerTokenSecretManager().startContainerSuccessful(
      containerTokenIdentifier);
    NMAuditLogger.logSuccess(user, AuditConstants.START_CONTAINER,
      "ContainerManageImpl", applicationID, containerId);
    StartContainerResponse response =
        recordFactory.newRecordInstance(StartContainerResponse.class);
    response.setAllServicesMetaData(auxiliaryServices.getMetaData());
    // TODO launchedContainer misplaced -> doesn't necessarily mean a container
    // launch. A finished Application will not launch containers.
    metrics.launchedContainer();
    metrics.allocateContainer(containerTokenIdentifier.getResource());
    return response;
  }

  protected ContainerTokenIdentifier verifyAndGetContainerTokenIdentifier(
      org.apache.hadoop.yarn.api.records.Token token) throws YarnException,
      InvalidToken {
    ContainerTokenIdentifier containerTokenIdentifier = null;
    try {
      containerTokenIdentifier =
          BuilderUtils.newContainerTokenIdentifier(token);
    } catch (IOException e) {
      throw RPCUtil.getRemoteException(e);
    }
    byte[] password =
        context.getContainerTokenSecretManager().retrievePassword(
          containerTokenIdentifier);
    byte[] tokenPass = token.getPassword().array();
    if (password == null || tokenPass == null
        || !Arrays.equals(password, tokenPass)) {
      throw new InvalidToken(
        "Invalid container token used for starting container on : "
            + context.getNodeId().toString());
    }
    return containerTokenIdentifier;
  }

  @Private
  @VisibleForTesting
  protected void updateNMTokenIdentifier(NMTokenIdentifier nmTokenIdentifier)
      throws InvalidToken {
    context.getNMTokenSecretManager().appAttemptStartContainer(
      nmTokenIdentifier);
  }

  private Credentials parseCredentials(ContainerLaunchContext launchContext)
      throws YarnException {
    Credentials credentials = new Credentials();
    // //////////// Parse credentials
    ByteBuffer tokens = launchContext.getTokens();

    if (tokens != null) {
      DataInputByteBuffer buf = new DataInputByteBuffer();
      tokens.rewind();
      buf.reset(tokens);
      try {
        credentials.readTokenStorageStream(buf);
        if (LOG.isDebugEnabled()) {
          for (Token<? extends TokenIdentifier> tk : credentials.getAllTokens()) {
            LOG.debug(tk.getService() + " = " + tk.toString());
          }
        }
      } catch (IOException e) {
        throw RPCUtil.getRemoteException(e);
      }
    }
    // //////////// End of parsing credentials
    return credentials;
  }

  /**
   * Stop the container running on this NodeManager.
   */
  @Override
  @SuppressWarnings("unchecked")
  public StopContainerResponse stopContainer(StopContainerRequest request)
      throws YarnException, IOException {

    ContainerId containerID = request.getContainerId();
    String containerIDStr = containerID.toString();
    Container container = this.context.getContainers().get(containerID);
    LOG.info("Getting container-status for " + containerIDStr);
    authorizeGetAndStopContainerRequest(containerID, container, true);

    StopContainerResponse response =
        recordFactory.newRecordInstance(StopContainerResponse.class);

    dispatcher.getEventHandler().handle(
      new ContainerKillEvent(containerID,
        "Container killed by the ApplicationMaster."));

    NMAuditLogger.logSuccess(container.getUser(),
      AuditConstants.STOP_CONTAINER, "ContainerManageImpl", containerID
        .getApplicationAttemptId().getApplicationId(), containerID);

    // TODO: Move this code to appropriate place once kill_container is
    // implemented.
    nodeStatusUpdater.sendOutofBandHeartBeat();

    return response;
  }

  @Override
  public GetContainerStatusResponse getContainerStatus(
      GetContainerStatusRequest request) throws YarnException, IOException {

    ContainerId containerID = request.getContainerId();
    String containerIDStr = containerID.toString();
    Container container = this.context.getContainers().get(containerID);

    LOG.info("Getting container-status for " + containerIDStr);
    authorizeGetAndStopContainerRequest(containerID, container, false);

    ContainerStatus containerStatus = container.cloneAndGetContainerStatus();
    LOG.info("Returning " + containerStatus);
    GetContainerStatusResponse response =
        recordFactory.newRecordInstance(GetContainerStatusResponse.class);
    response.setStatus(containerStatus);
    return response;
  }

  @Private
  @VisibleForTesting
  protected void authorizeGetAndStopContainerRequest(ContainerId containerId,
      Container container, boolean stopRequest) throws YarnException {

    UserGroupInformation remoteUgi = getRemoteUgi();
    NMTokenIdentifier identifier = selectNMTokenIdentifier(remoteUgi);

    /*
     * For get/stop container status; we need to verify that 1) User (NMToken)
     * application attempt only has started container. 2) Requested containerId
     * belongs to the same application attempt (NMToken) which was used. (Note:-
     * This will prevent user in knowing another application's containers).
     */

    if ((!identifier.getApplicationAttemptId().equals(
      containerId.getApplicationAttemptId()))
        || (container != null && !identifier.getApplicationAttemptId().equals(
          container.getContainerId().getApplicationAttemptId()))) {
      if (stopRequest) {
        LOG.warn(identifier.getApplicationAttemptId()
            + " attempted to stop non-application container : "
            + container.getContainerId().toString());
        NMAuditLogger.logFailure("UnknownUser", AuditConstants.STOP_CONTAINER,
          "ContainerManagerImpl", "Trying to stop unknown container!",
          identifier.getApplicationAttemptId().getApplicationId(),
          container.getContainerId());
      } else {
        LOG.warn(identifier.getApplicationAttemptId()
            + " attempted to get get status for non-application container : "
            + container.getContainerId().toString());
      }
      throw RPCUtil.getRemoteException("Container " + containerId.toString()
          + " is not started by this application attempt.");
    }

    if (container == null) {
      throw RPCUtil.getRemoteException("Container " + containerId.toString()
          + " is not handled by this NodeManager");
    }
  }

  class ContainerEventDispatcher implements EventHandler<ContainerEvent> {
    @Override
    public void handle(ContainerEvent event) {
      Map<ContainerId,Container> containers =
        ContainerManagerImpl.this.context.getContainers();
      Container c = containers.get(event.getContainerID());
      if (c != null) {
        c.handle(event);
      } else {
        LOG.warn("Event " + event + " sent to absent container " +
            event.getContainerID());
      }
    }
  }

  class ApplicationEventDispatcher implements EventHandler<ApplicationEvent> {

    @Override
    public void handle(ApplicationEvent event) {
      Application app =
          ContainerManagerImpl.this.context.getApplications().get(
              event.getApplicationID());
      if (app != null) {
        app.handle(event);
      } else {
        LOG.warn("Event " + event + " sent to absent application "
            + event.getApplicationID());
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(ContainerManagerEvent event) {
    switch (event.getType()) {
    case FINISH_APPS:
      CMgrCompletedAppsEvent appsFinishedEvent =
          (CMgrCompletedAppsEvent) event;
      for (ApplicationId appID : appsFinishedEvent.getAppsToCleanup()) {
        this.dispatcher.getEventHandler().handle(
            new ApplicationFinishEvent(appID,
                "Application Killed by ResourceManager"));
      }
      break;
    case FINISH_CONTAINERS:
      CMgrCompletedContainersEvent containersFinishedEvent =
          (CMgrCompletedContainersEvent) event;
      for (ContainerId container : containersFinishedEvent
          .getContainersToCleanup()) {
        String diagnostic = "";
        if (containersFinishedEvent.getReason() == 
            CMgrCompletedContainersEvent.Reason.ON_SHUTDOWN) {
          diagnostic = "Container Killed on Shutdown";
        } else if (containersFinishedEvent.getReason() == 
            CMgrCompletedContainersEvent.Reason.BY_RESOURCEMANAGER) {
          diagnostic = "Container Killed by ResourceManager";
        }
        this.dispatcher.getEventHandler().handle(
            new ContainerKillEvent(container, diagnostic));
      }
      break;
    default:
      LOG.warn("Invalid event " + event.getType() + ". Ignoring.");
    }
  }

  public void setBlockNewContainerRequests(boolean blockNewContainerRequests) {
    this.blockNewContainerRequests.set(blockNewContainerRequests);
  }

  @Private
  @VisibleForTesting
  public boolean getBlockNewContainerRequestsStatus() {
    return this.blockNewContainerRequests.get();
  }
  
  @Override
  public void stateChanged(Service service) {
    // TODO Auto-generated method stub
  }
  
  public Context getContext() {
    return this.context;
  }

}
