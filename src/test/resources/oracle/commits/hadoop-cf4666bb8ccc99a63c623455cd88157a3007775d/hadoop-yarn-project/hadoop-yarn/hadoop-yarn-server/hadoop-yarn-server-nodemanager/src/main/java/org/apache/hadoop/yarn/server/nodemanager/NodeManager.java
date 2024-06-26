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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.service.CompositeService;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.JvmPauseMonitor;
import org.apache.hadoop.util.NodeHealthScriptRunner;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.yarn.YarnUncaughtExceptionHandler;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.event.AsyncDispatcher;
import org.apache.hadoop.yarn.event.Dispatcher;
import org.apache.hadoop.yarn.event.EventHandler;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.security.ContainerTokenIdentifier;
import org.apache.hadoop.yarn.server.api.protocolrecords.LogAggregationReport;
import org.apache.hadoop.yarn.server.api.records.NodeHealthStatus;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.ContainerManager;
import org.apache.hadoop.yarn.server.nodemanager.collectormanager.NMCollectorService;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.ContainerManagerImpl;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.application.Application;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.Container;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.queuing.QueuingContainerManagerImpl;
import org.apache.hadoop.yarn.server.nodemanager.metrics.NodeManagerMetrics;
import org.apache.hadoop.yarn.server.nodemanager.nodelabels.ConfigurationNodeLabelsProvider;
import org.apache.hadoop.yarn.server.nodemanager.nodelabels.NodeLabelsProvider;
import org.apache.hadoop.yarn.server.nodemanager.nodelabels.ScriptBasedNodeLabelsProvider;
import org.apache.hadoop.yarn.server.nodemanager.recovery.NMLeveldbStateStoreService;
import org.apache.hadoop.yarn.server.nodemanager.recovery.NMNullStateStoreService;
import org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreService;
import org.apache.hadoop.yarn.server.nodemanager.scheduler.OpportunisticContainerAllocator;
import org.apache.hadoop.yarn.server.nodemanager.security.NMContainerTokenSecretManager;
import org.apache.hadoop.yarn.server.nodemanager.security.NMTokenSecretManagerInNM;
import org.apache.hadoop.yarn.server.nodemanager.timelineservice.NMTimelinePublisher;
import org.apache.hadoop.yarn.server.nodemanager.webapp.WebServer;
import org.apache.hadoop.yarn.server.security.ApplicationACLsManager;

import com.google.common.annotations.VisibleForTesting;

public class NodeManager extends CompositeService 
    implements EventHandler<NodeManagerEvent> {

  /**
   * Priority of the NodeManager shutdown hook.
   */
  public static final int SHUTDOWN_HOOK_PRIORITY = 30;

  private static final Log LOG = LogFactory.getLog(NodeManager.class);
  private static long nmStartupTime = System.currentTimeMillis();
  protected final NodeManagerMetrics metrics = NodeManagerMetrics.create();
  private JvmPauseMonitor pauseMonitor;
  private ApplicationACLsManager aclsManager;
  private NodeHealthCheckerService nodeHealthChecker;
  private NodeLabelsProvider nodeLabelsProvider;
  private LocalDirsHandlerService dirsHandler;
  private Context context;
  private AsyncDispatcher dispatcher;
  private ContainerManagerImpl containerManager;
  private NMCollectorService nmCollectorService;
  private NodeStatusUpdater nodeStatusUpdater;
  private NodeResourceMonitor nodeResourceMonitor;
  private static CompositeServiceShutdownHook nodeManagerShutdownHook;
  private NMStateStoreService nmStore = null;
  
  private AtomicBoolean isStopping = new AtomicBoolean(false);
  private boolean rmWorkPreservingRestartEnabled;
  private boolean shouldExitOnShutdownEvent = false;

  public NodeManager() {
    super(NodeManager.class.getName());
  }

  public static long getNMStartupTime() {
    return nmStartupTime;
  }

  protected NodeStatusUpdater createNodeStatusUpdater(Context context,
      Dispatcher dispatcher, NodeHealthCheckerService healthChecker) {
    return new NodeStatusUpdaterImpl(context, dispatcher, healthChecker,
        metrics, nodeLabelsProvider);
  }

  protected NodeStatusUpdater createNodeStatusUpdater(Context context,
      Dispatcher dispatcher, NodeHealthCheckerService healthChecker,
      NodeLabelsProvider nodeLabelsProvider) {
    return new NodeStatusUpdaterImpl(context, dispatcher, healthChecker,
        metrics, nodeLabelsProvider);
  }

  protected NodeLabelsProvider createNodeLabelsProvider(Configuration conf)
      throws IOException {
    NodeLabelsProvider provider = null;
    String providerString =
        conf.get(YarnConfiguration.NM_NODE_LABELS_PROVIDER_CONFIG, null);
    if (providerString == null || providerString.trim().length() == 0) {
      // Seems like Distributed Node Labels configuration is not enabled
      return provider;
    }
    switch (providerString.trim().toLowerCase()) {
    case YarnConfiguration.CONFIG_NODE_LABELS_PROVIDER:
      provider = new ConfigurationNodeLabelsProvider();
      break;
    case YarnConfiguration.SCRIPT_NODE_LABELS_PROVIDER:
      provider = new ScriptBasedNodeLabelsProvider();
      break;
    default:
      try {
        Class<? extends NodeLabelsProvider> labelsProviderClass =
            conf.getClass(YarnConfiguration.NM_NODE_LABELS_PROVIDER_CONFIG,
                null, NodeLabelsProvider.class);
        provider = labelsProviderClass.newInstance();
      } catch (InstantiationException | IllegalAccessException
          | RuntimeException e) {
        LOG.error("Failed to create NodeLabelsProvider based on Configuration",
            e);
        throw new IOException(
            "Failed to create NodeLabelsProvider : " + e.getMessage(), e);
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Distributed Node Labels is enabled"
          + " with provider class as : " + provider.getClass().toString());
    }
    return provider;
  }

  protected NodeResourceMonitor createNodeResourceMonitor() {
    return new NodeResourceMonitorImpl();
  }

  protected ContainerManagerImpl createContainerManager(Context context,
      ContainerExecutor exec, DeletionService del,
      NodeStatusUpdater nodeStatusUpdater, ApplicationACLsManager aclsManager,
      LocalDirsHandlerService dirsHandler) {
    if (getConfig().getBoolean(YarnConfiguration.NM_CONTAINER_QUEUING_ENABLED,
        YarnConfiguration.NM_CONTAINER_QUEUING_ENABLED_DEFAULT)) {
      return new QueuingContainerManagerImpl(context, exec, del,
          nodeStatusUpdater, metrics, dirsHandler);
    } else {
      return new ContainerManagerImpl(context, exec, del, nodeStatusUpdater,
          metrics, dirsHandler);
    }
  }

  protected NMCollectorService createNMCollectorService(Context context) {
    return new NMCollectorService(context);
  }

  protected WebServer createWebServer(Context nmContext,
      ResourceView resourceView, ApplicationACLsManager aclsManager,
      LocalDirsHandlerService dirsHandler) {
    return new WebServer(nmContext, resourceView, aclsManager, dirsHandler);
  }

  protected DeletionService createDeletionService(ContainerExecutor exec) {
    return new DeletionService(exec, nmStore);
  }

  protected NMContext createNMContext(
      NMContainerTokenSecretManager containerTokenSecretManager,
      NMTokenSecretManagerInNM nmTokenSecretManager,
      NMStateStoreService stateStore, boolean isDistSchedulerEnabled,
      Configuration conf) {
    return new NMContext(containerTokenSecretManager, nmTokenSecretManager,
        dirsHandler, aclsManager, stateStore, isDistSchedulerEnabled, conf);
  }

  protected void doSecureLogin() throws IOException {
    SecurityUtil.login(getConfig(), YarnConfiguration.NM_KEYTAB,
        YarnConfiguration.NM_PRINCIPAL);
  }

  private void initAndStartRecoveryStore(Configuration conf)
      throws IOException {
    boolean recoveryEnabled = conf.getBoolean(
        YarnConfiguration.NM_RECOVERY_ENABLED,
        YarnConfiguration.DEFAULT_NM_RECOVERY_ENABLED);
    if (recoveryEnabled) {
      FileSystem recoveryFs = FileSystem.getLocal(conf);
      String recoveryDirName = conf.get(YarnConfiguration.NM_RECOVERY_DIR);
      if (recoveryDirName == null) {
        throw new IllegalArgumentException("Recovery is enabled but " +
            YarnConfiguration.NM_RECOVERY_DIR + " is not set.");
      }
      Path recoveryRoot = new Path(recoveryDirName);
      recoveryFs.mkdirs(recoveryRoot, new FsPermission((short)0700));
      nmStore = new NMLeveldbStateStoreService();
    } else {
      nmStore = new NMNullStateStoreService();
    }
    nmStore.init(conf);
    nmStore.start();
  }

  private void stopRecoveryStore() throws IOException {
    if (null != nmStore) {
      nmStore.stop();
      if (null != context) {
        if (context.getDecommissioned() && nmStore.canRecover()) {
          LOG.info("Removing state store due to decommission");
          Configuration conf = getConfig();
          Path recoveryRoot =
              new Path(conf.get(YarnConfiguration.NM_RECOVERY_DIR));
          LOG.info("Removing state store at " + recoveryRoot
              + " due to decommission");
          FileSystem recoveryFs = FileSystem.getLocal(conf);
          if (!recoveryFs.delete(recoveryRoot, true)) {
            LOG.warn("Unable to delete " + recoveryRoot);
          }
        }
      }
    }
  }

  private void recoverTokens(NMTokenSecretManagerInNM nmTokenSecretManager,
      NMContainerTokenSecretManager containerTokenSecretManager)
          throws IOException {
    if (nmStore.canRecover()) {
      nmTokenSecretManager.recover();
      containerTokenSecretManager.recover();
    }
  }

  public static NodeHealthScriptRunner getNodeHealthScriptRunner(Configuration conf) {
    String nodeHealthScript = 
        conf.get(YarnConfiguration.NM_HEALTH_CHECK_SCRIPT_PATH);
    if(!NodeHealthScriptRunner.shouldRun(nodeHealthScript)) {
      LOG.info("Node Manager health check script is not available "
          + "or doesn't have execute permission, so not "
          + "starting the node health script runner.");
      return null;
    }
    long nmCheckintervalTime = conf.getLong(
        YarnConfiguration.NM_HEALTH_CHECK_INTERVAL_MS,
        YarnConfiguration.DEFAULT_NM_HEALTH_CHECK_INTERVAL_MS);
    long scriptTimeout = conf.getLong(
        YarnConfiguration.NM_HEALTH_CHECK_SCRIPT_TIMEOUT_MS,
        YarnConfiguration.DEFAULT_NM_HEALTH_CHECK_SCRIPT_TIMEOUT_MS);
    String[] scriptArgs = conf.getStrings(
        YarnConfiguration.NM_HEALTH_CHECK_SCRIPT_OPTS, new String[] {});
    return new NodeHealthScriptRunner(nodeHealthScript,
        nmCheckintervalTime, scriptTimeout, scriptArgs);
  }

  @Override
  protected void serviceInit(Configuration conf) throws Exception {

    conf.setBoolean(Dispatcher.DISPATCHER_EXIT_ON_ERROR_KEY, true);

    rmWorkPreservingRestartEnabled = conf.getBoolean(YarnConfiguration
            .RM_WORK_PRESERVING_RECOVERY_ENABLED,
        YarnConfiguration.DEFAULT_RM_WORK_PRESERVING_RECOVERY_ENABLED);

    try {
      initAndStartRecoveryStore(conf);
    } catch (IOException e) {
      String recoveryDirName = conf.get(YarnConfiguration.NM_RECOVERY_DIR);
      throw new
          YarnRuntimeException("Unable to initialize recovery directory at "
              + recoveryDirName, e);
    }

    NMContainerTokenSecretManager containerTokenSecretManager =
        new NMContainerTokenSecretManager(conf, nmStore);

    NMTokenSecretManagerInNM nmTokenSecretManager =
        new NMTokenSecretManagerInNM(nmStore);

    recoverTokens(nmTokenSecretManager, containerTokenSecretManager);
    
    this.aclsManager = new ApplicationACLsManager(conf);

    ContainerExecutor exec = ReflectionUtils.newInstance(
        conf.getClass(YarnConfiguration.NM_CONTAINER_EXECUTOR,
          DefaultContainerExecutor.class, ContainerExecutor.class), conf);
    try {
      exec.init();
    } catch (IOException e) {
      throw new YarnRuntimeException("Failed to initialize container executor", e);
    }    
    DeletionService del = createDeletionService(exec);
    addService(del);

    // NodeManager level dispatcher
    this.dispatcher = new AsyncDispatcher();

    dirsHandler = new LocalDirsHandlerService(metrics);
    nodeHealthChecker =
        new NodeHealthCheckerService(
            getNodeHealthScriptRunner(conf), dirsHandler);
    addService(nodeHealthChecker);

    boolean isDistSchedulingEnabled =
        conf.getBoolean(YarnConfiguration.DIST_SCHEDULING_ENABLED,
            YarnConfiguration.DIST_SCHEDULING_ENABLED_DEFAULT);

    this.context = createNMContext(containerTokenSecretManager,
        nmTokenSecretManager, nmStore, isDistSchedulingEnabled, conf);

    nodeLabelsProvider = createNodeLabelsProvider(conf);

    if (null == nodeLabelsProvider) {
      nodeStatusUpdater =
          createNodeStatusUpdater(context, dispatcher, nodeHealthChecker);
    } else {
      addIfService(nodeLabelsProvider);
      nodeStatusUpdater =
          createNodeStatusUpdater(context, dispatcher, nodeHealthChecker,
              nodeLabelsProvider);
    }

    nodeResourceMonitor = createNodeResourceMonitor();
    addService(nodeResourceMonitor);
    ((NMContext) context).setNodeResourceMonitor(nodeResourceMonitor);

    containerManager =
        createContainerManager(context, exec, del, nodeStatusUpdater,
        this.aclsManager, dirsHandler);
    addService(containerManager);
    ((NMContext) context).setContainerManager(containerManager);

    WebServer webServer = createWebServer(context, containerManager
        .getContainersMonitor(), this.aclsManager, dirsHandler);
    addService(webServer);
    ((NMContext) context).setWebServer(webServer);

    ((NMContext) context).setQueueableContainerAllocator(
        new OpportunisticContainerAllocator(nodeStatusUpdater, context,
            webServer.getPort()));

    dispatcher.register(ContainerManagerEventType.class, containerManager);
    dispatcher.register(NodeManagerEventType.class, this);
    addService(dispatcher);

    pauseMonitor = new JvmPauseMonitor();
    addService(pauseMonitor);
    metrics.getJvmMetrics().setPauseMonitor(pauseMonitor);

    DefaultMetricsSystem.initialize("NodeManager");

    this.nmCollectorService = createNMCollectorService(context);
    addService(nmCollectorService);

    // StatusUpdater should be added last so that it get started last 
    // so that we make sure everything is up before registering with RM. 
    addService(nodeStatusUpdater);
    ((NMContext) context).setNodeStatusUpdater(nodeStatusUpdater);

    super.serviceInit(conf);
    // TODO add local dirs to del
  }

  @Override
  protected void serviceStart() throws Exception {
    try {
      doSecureLogin();
    } catch (IOException e) {
      throw new YarnRuntimeException("Failed NodeManager login", e);
    }
    super.serviceStart();
  }

  @Override
  protected void serviceStop() throws Exception {
    if (isStopping.getAndSet(true)) {
      return;
    }
    try {
      super.serviceStop();
      DefaultMetricsSystem.shutdown();
    } finally {
      // YARN-3641: NM's services stop get failed shouldn't block the
      // release of NMLevelDBStore.
      stopRecoveryStore();
    }
  }

  public String getName() {
    return "NodeManager";
  }

  protected void shutDown() {
    new Thread() {
      @Override
      public void run() {
        try {
          NodeManager.this.stop();
        } catch (Throwable t) {
          LOG.error("Error while shutting down NodeManager", t);
        } finally {
          if (shouldExitOnShutdownEvent
              && !ShutdownHookManager.get().isShutdownInProgress()) {
            ExitUtil.terminate(-1);
          }
        }
      }
    }.start();
  }

  protected void resyncWithRM() {
    //we do not want to block dispatcher thread here
    new Thread() {
      @Override
      public void run() {
        try {
          LOG.info("Notifying ContainerManager to block new container-requests");
          containerManager.setBlockNewContainerRequests(true);
          if (!rmWorkPreservingRestartEnabled) {
            LOG.info("Cleaning up running containers on resync");
            containerManager.cleanupContainersOnNMResync();
          } else {
            LOG.info("Preserving containers on resync");
          }
          ((NodeStatusUpdaterImpl) nodeStatusUpdater)
            .rebootNodeStatusUpdaterAndRegisterWithRM();
        } catch (YarnRuntimeException e) {
          LOG.fatal("Error while rebooting NodeStatusUpdater.", e);
          shutDown();
        }
      }
    }.start();
  }

  public static class NMContext implements Context {

    private NodeId nodeId = null;
    
    private Configuration conf = null;
    
    protected final ConcurrentMap<ApplicationId, Application> applications =
        new ConcurrentHashMap<ApplicationId, Application>();

    private volatile Map<ApplicationId, Credentials> systemCredentials =
        new HashMap<ApplicationId, Credentials>();

    protected final ConcurrentMap<ContainerId, Container> containers =
        new ConcurrentSkipListMap<ContainerId, Container>();

    protected Map<ApplicationId, String> registeredCollectors =
        new ConcurrentHashMap<ApplicationId, String>();

    protected final ConcurrentMap<ContainerId,
        org.apache.hadoop.yarn.api.records.Container> increasedContainers =
            new ConcurrentHashMap<>();

    private final NMContainerTokenSecretManager containerTokenSecretManager;
    private final NMTokenSecretManagerInNM nmTokenSecretManager;
    private ContainerManager containerManager;
    private NodeResourceMonitor nodeResourceMonitor;
    private final LocalDirsHandlerService dirsHandler;
    private final ApplicationACLsManager aclsManager;
    private WebServer webServer;
    private final NodeHealthStatus nodeHealthStatus = RecordFactoryProvider
        .getRecordFactory(null).newRecordInstance(NodeHealthStatus.class);
    private final NMStateStoreService stateStore;
    private boolean isDecommissioned = false;
    private final ConcurrentLinkedQueue<LogAggregationReport>
        logAggregationReportForApps;
    private NodeStatusUpdater nodeStatusUpdater;
    private final boolean isDistSchedulingEnabled;

    private OpportunisticContainerAllocator containerAllocator;

    private final QueuingContext queuingContext;

    private NMTimelinePublisher nmTimelinePublisher;

    public NMContext(NMContainerTokenSecretManager containerTokenSecretManager,
        NMTokenSecretManagerInNM nmTokenSecretManager,
        LocalDirsHandlerService dirsHandler, ApplicationACLsManager aclsManager,
        NMStateStoreService stateStore, boolean isDistSchedulingEnabled,
        Configuration conf) {
      this.containerTokenSecretManager = containerTokenSecretManager;
      this.nmTokenSecretManager = nmTokenSecretManager;
      this.dirsHandler = dirsHandler;
      this.aclsManager = aclsManager;
      this.nodeHealthStatus.setIsNodeHealthy(true);
      this.nodeHealthStatus.setHealthReport("Healthy");
      this.nodeHealthStatus.setLastHealthReportTime(System.currentTimeMillis());
      this.stateStore = stateStore;
      this.logAggregationReportForApps = new ConcurrentLinkedQueue<
          LogAggregationReport>();
      this.queuingContext = new QueuingNMContext();
      this.isDistSchedulingEnabled = isDistSchedulingEnabled;
      this.conf = conf;
    }

    /**
     * Usable only after ContainerManager is started.
     */
    @Override
    public NodeId getNodeId() {
      return this.nodeId;
    }

    @Override
    public int getHttpPort() {
      return this.webServer.getPort();
    }

    @Override
    public ConcurrentMap<ApplicationId, Application> getApplications() {
      return this.applications;
    }
    
    @Override
    public Configuration getConf() {
      return this.conf;
    }

    @Override
    public ConcurrentMap<ContainerId, Container> getContainers() {
      return this.containers;
    }

    @Override
    public ConcurrentMap<ContainerId, org.apache.hadoop.yarn.api.records.Container>
        getIncreasedContainers() {
      return this.increasedContainers;
    }

    @Override
    public NMContainerTokenSecretManager getContainerTokenSecretManager() {
      return this.containerTokenSecretManager;
    }
    
    @Override
    public NMTokenSecretManagerInNM getNMTokenSecretManager() {
      return this.nmTokenSecretManager;
    }
    
    @Override
    public NodeHealthStatus getNodeHealthStatus() {
      return this.nodeHealthStatus;
    }

    @Override
    public NodeResourceMonitor getNodeResourceMonitor() {
      return this.nodeResourceMonitor;
    }

    public void setNodeResourceMonitor(NodeResourceMonitor nodeResourceMonitor) {
      this.nodeResourceMonitor = nodeResourceMonitor;
    }

    @Override
    public ContainerManager getContainerManager() {
      return this.containerManager;
    }

    public void setContainerManager(ContainerManager containerManager) {
      this.containerManager = containerManager;
    }

    public void setWebServer(WebServer webServer) {
      this.webServer = webServer;
    }

    public void setNodeId(NodeId nodeId) {
      this.nodeId = nodeId;
    }

    @Override
    public LocalDirsHandlerService getLocalDirsHandler() {
      return dirsHandler;
    }
    
    @Override
    public ApplicationACLsManager getApplicationACLsManager() {
      return aclsManager;
    }

    @Override
    public NMStateStoreService getNMStateStore() {
      return stateStore;
    }

    @Override
    public boolean getDecommissioned() {
      return isDecommissioned;
    }

    @Override
    public void setDecommissioned(boolean isDecommissioned) {
      this.isDecommissioned = isDecommissioned;
    }

    @Override
    public Map<ApplicationId, Credentials> getSystemCredentialsForApps() {
      return systemCredentials;
    }

    public void setSystemCrendentialsForApps(
        Map<ApplicationId, Credentials> systemCredentials) {
      this.systemCredentials = systemCredentials;
    }

    @Override
    public ConcurrentLinkedQueue<LogAggregationReport>
        getLogAggregationStatusForApps() {
      return this.logAggregationReportForApps;
    }

    public NodeStatusUpdater getNodeStatusUpdater() {
      return this.nodeStatusUpdater;
    }

    public void setNodeStatusUpdater(NodeStatusUpdater nodeStatusUpdater) {
      this.nodeStatusUpdater = nodeStatusUpdater;
    }

    @Override
    public QueuingContext getQueuingContext() {
      return this.queuingContext;
    }

    public boolean isDistributedSchedulingEnabled() {
      return isDistSchedulingEnabled;
    }

    public void setQueueableContainerAllocator(
        OpportunisticContainerAllocator containerAllocator) {
      this.containerAllocator = containerAllocator;
    }

    @Override
    public OpportunisticContainerAllocator getContainerAllocator() {
      return containerAllocator;
    }

    @Override
    public Map<ApplicationId, String> getRegisteredCollectors() {
      return this.registeredCollectors;
    }

    public void addRegisteredCollectors(
        Map<ApplicationId, String> newRegisteredCollectors) {
      this.registeredCollectors.putAll(newRegisteredCollectors);
    }

    @Override
    public void setNMTimelinePublisher(NMTimelinePublisher nmMetricsPublisher) {
      this.nmTimelinePublisher = nmMetricsPublisher;
    }

    @Override
    public NMTimelinePublisher getNMTimelinePublisher() {
      return nmTimelinePublisher;
    }
  }

  /**
   * Class that keeps the context for containers queued at the NM.
   */
  public static class QueuingNMContext implements Context.QueuingContext {
    protected final ConcurrentMap<ContainerId, ContainerTokenIdentifier>
        queuedContainers = new ConcurrentSkipListMap<>();

    protected final ConcurrentMap<ContainerTokenIdentifier, String>
        killedQueuedContainers = new ConcurrentHashMap<>();

    @Override
    public ConcurrentMap<ContainerId, ContainerTokenIdentifier>
        getQueuedContainers() {
      return this.queuedContainers;
    }

    @Override
    public ConcurrentMap<ContainerTokenIdentifier, String>
        getKilledQueuedContainers() {
      return this.killedQueuedContainers;
    }
  }

  /**
   * @return the node health checker
   */
  public NodeHealthCheckerService getNodeHealthChecker() {
    return nodeHealthChecker;
  }

  private void initAndStartNodeManager(Configuration conf, boolean hasToReboot) {
    try {
      // Failed to start if we're a Unix based system but we don't have bash.
      // Bash is necessary to launch containers under Unix-based systems.
      if (!Shell.WINDOWS) {
        if (!Shell.isBashSupported) {
          String message =
              "Failing NodeManager start since we're on a "
                  + "Unix-based system but bash doesn't seem to be available.";
          LOG.fatal(message);
          throw new YarnRuntimeException(message);
        }
      }

      // Remove the old hook if we are rebooting.
      if (hasToReboot && null != nodeManagerShutdownHook) {
        ShutdownHookManager.get().removeShutdownHook(nodeManagerShutdownHook);
      }

      nodeManagerShutdownHook = new CompositeServiceShutdownHook(this);
      ShutdownHookManager.get().addShutdownHook(nodeManagerShutdownHook,
                                                SHUTDOWN_HOOK_PRIORITY);
      // System exit should be called only when NodeManager is instantiated from
      // main() funtion
      this.shouldExitOnShutdownEvent = true;
      this.init(conf);
      this.start();
    } catch (Throwable t) {
      LOG.fatal("Error starting NodeManager", t);
      System.exit(-1);
    }
  }

  @Override
  public void handle(NodeManagerEvent event) {
    switch (event.getType()) {
    case SHUTDOWN:
      shutDown();
      break;
    case RESYNC:
      resyncWithRM();
      break;
    default:
      LOG.warn("Invalid shutdown event " + event.getType() + ". Ignoring.");
    }
  }
  
  // For testing
  NodeManager createNewNodeManager() {
    return new NodeManager();
  }
  
  // For testing
  ContainerManagerImpl getContainerManager() {
    return containerManager;
  }
  
  //For testing
  Dispatcher getNMDispatcher(){
    return dispatcher;
  }

  @VisibleForTesting
  public Context getNMContext() {
    return this.context;
  }

  // For testing
  NMCollectorService getNMCollectorService() {
    return this.nmCollectorService;
  }

  public static void main(String[] args) throws IOException {
    Thread.setDefaultUncaughtExceptionHandler(new YarnUncaughtExceptionHandler());
    StringUtils.startupShutdownMessage(NodeManager.class, args, LOG);
    NodeManager nodeManager = new NodeManager();
    Configuration conf = new YarnConfiguration();
    new GenericOptionsParser(conf, args);
    nodeManager.initAndStartNodeManager(conf, false);
  }

  @VisibleForTesting
  @Private
  public NodeStatusUpdater getNodeStatusUpdater() {
    return nodeStatusUpdater;
  }
}
