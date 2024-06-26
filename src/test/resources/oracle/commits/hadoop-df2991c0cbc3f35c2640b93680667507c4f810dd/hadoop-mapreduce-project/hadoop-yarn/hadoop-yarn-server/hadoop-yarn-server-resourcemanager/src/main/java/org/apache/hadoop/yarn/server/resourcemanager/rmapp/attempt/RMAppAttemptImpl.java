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

package org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReport;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.ResourceRequest;
import org.apache.hadoop.yarn.event.EventHandler;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.server.resourcemanager.ApplicationMasterService;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.amlauncher.AMLauncherEvent;
import org.apache.hadoop.yarn.server.resourcemanager.amlauncher.AMLauncherEventType;
import org.apache.hadoop.yarn.server.resourcemanager.resource.Resources;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppEventType;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppFailedAttemptEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppRejectedEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.event.RMAppAttemptContainerAcquiredEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.event.RMAppAttemptContainerFinishedEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.event.RMAppAttemptLaunchFailedEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.event.RMAppAttemptRegistrationEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.event.RMAppAttemptRejectedEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.event.RMAppAttemptStatusupdateEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.event.RMAppAttemptUnregistrationEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmcontainer.RMContainer;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.Allocation;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.SchedulerAppReport;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.YarnScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.event.AppAddedSchedulerEvent;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.event.AppRemovedSchedulerEvent;
import org.apache.hadoop.yarn.state.InvalidStateTransitonException;
import org.apache.hadoop.yarn.state.MultipleArcTransition;
import org.apache.hadoop.yarn.state.SingleArcTransition;
import org.apache.hadoop.yarn.state.StateMachine;
import org.apache.hadoop.yarn.state.StateMachineFactory;
import org.apache.hadoop.yarn.util.BuilderUtils;

public class RMAppAttemptImpl implements RMAppAttempt {

  private static final Log LOG = LogFactory.getLog(RMAppAttemptImpl.class);

  private static final RecordFactory recordFactory = RecordFactoryProvider
      .getRecordFactory(null);

  public final static Priority AM_CONTAINER_PRIORITY = recordFactory
      .newRecordInstance(Priority.class);
  static {
    AM_CONTAINER_PRIORITY.setPriority(0);
  }

  private final StateMachine<RMAppAttemptState,
                             RMAppAttemptEventType,
                             RMAppAttemptEvent> stateMachine;

  private final RMContext rmContext;
  @SuppressWarnings("rawtypes")
  private final EventHandler eventHandler;
  private final YarnScheduler scheduler;
  private final ApplicationMasterService masterService;

  private final ReadLock readLock;
  private final WriteLock writeLock;

  private final ApplicationAttemptId applicationAttemptId;
  private final String clientToken;
  private final ApplicationSubmissionContext submissionContext;

  //nodes on while this attempt's containers ran
  private final Set<NodeId> ranNodes =
    new HashSet<NodeId>();
  private final List<ContainerStatus> justFinishedContainers =
    new ArrayList<ContainerStatus>();
  private Container masterContainer;

  private float progress = 0;
  private String host = "N/A";
  private int rpcPort;
  private String trackingUrl = "N/A";
  // Set to null initially. Will eventually get set 
  // if an RMAppAttemptUnregistrationEvent occurs
  private FinalApplicationStatus finalStatus = null;
  private final StringBuilder diagnostics = new StringBuilder();

  private static final StateMachineFactory<RMAppAttemptImpl,
                                           RMAppAttemptState,
                                           RMAppAttemptEventType,
                                           RMAppAttemptEvent>
       stateMachineFactory  = new StateMachineFactory<RMAppAttemptImpl,
                                            RMAppAttemptState,
                                            RMAppAttemptEventType,
                                     RMAppAttemptEvent>(RMAppAttemptState.NEW)

       // Transitions from NEW State
      .addTransition(RMAppAttemptState.NEW, RMAppAttemptState.SUBMITTED,
          RMAppAttemptEventType.START, new AttemptStartedTransition())
      .addTransition(RMAppAttemptState.NEW, RMAppAttemptState.KILLED,
          RMAppAttemptEventType.KILL,
          new BaseFinalTransition(RMAppAttemptState.KILLED))

      // Transitions from SUBMITTED state
      .addTransition(RMAppAttemptState.SUBMITTED, RMAppAttemptState.FAILED,
          RMAppAttemptEventType.APP_REJECTED, new AppRejectedTransition())
      .addTransition(RMAppAttemptState.SUBMITTED, RMAppAttemptState.SCHEDULED,
          RMAppAttemptEventType.APP_ACCEPTED, new ScheduleTransition())
      .addTransition(RMAppAttemptState.SUBMITTED, RMAppAttemptState.KILLED,
          RMAppAttemptEventType.KILL,
          new BaseFinalTransition(RMAppAttemptState.KILLED))

       // Transitions from SCHEDULED State
      .addTransition(RMAppAttemptState.SCHEDULED,
          RMAppAttemptState.ALLOCATED,
          RMAppAttemptEventType.CONTAINER_ALLOCATED,
          new AMContainerAllocatedTransition())
      .addTransition(RMAppAttemptState.SCHEDULED, RMAppAttemptState.KILLED,
          RMAppAttemptEventType.KILL,
          new BaseFinalTransition(RMAppAttemptState.KILLED))

       // Transitions from ALLOCATED State
      .addTransition(RMAppAttemptState.ALLOCATED,
          RMAppAttemptState.ALLOCATED,
          RMAppAttemptEventType.CONTAINER_ACQUIRED,
          new ContainerAcquiredTransition())
      .addTransition(RMAppAttemptState.ALLOCATED, RMAppAttemptState.LAUNCHED,
          RMAppAttemptEventType.LAUNCHED, new AMLaunchedTransition())
      .addTransition(RMAppAttemptState.ALLOCATED, RMAppAttemptState.FAILED,
          RMAppAttemptEventType.LAUNCH_FAILED, new LaunchFailedTransition())
      .addTransition(RMAppAttemptState.ALLOCATED, RMAppAttemptState.KILLED,
          RMAppAttemptEventType.KILL, new KillAllocatedAMTransition())

       // Transitions from LAUNCHED State
      .addTransition(RMAppAttemptState.LAUNCHED, RMAppAttemptState.RUNNING,
          RMAppAttemptEventType.REGISTERED, new AMRegisteredTransition())
      .addTransition(RMAppAttemptState.LAUNCHED, RMAppAttemptState.FAILED,
          RMAppAttemptEventType.CONTAINER_FINISHED,
          new AMContainerCrashedTransition())
      .addTransition(
          RMAppAttemptState.LAUNCHED, RMAppAttemptState.FAILED,
          RMAppAttemptEventType.EXPIRE,
          new FinalTransition(RMAppAttemptState.FAILED))
      .addTransition(RMAppAttemptState.LAUNCHED, RMAppAttemptState.KILLED,
          RMAppAttemptEventType.KILL,
          new FinalTransition(RMAppAttemptState.KILLED))

       // Transitions from RUNNING State
      .addTransition(RMAppAttemptState.RUNNING, RMAppAttemptState.FINISHED,
          RMAppAttemptEventType.UNREGISTERED, new AMUnregisteredTransition())
      .addTransition(RMAppAttemptState.RUNNING, RMAppAttemptState.RUNNING,
          RMAppAttemptEventType.STATUS_UPDATE, new StatusUpdateTransition())
      .addTransition(RMAppAttemptState.RUNNING, RMAppAttemptState.RUNNING,
          RMAppAttemptEventType.CONTAINER_ALLOCATED)
      .addTransition(
                RMAppAttemptState.RUNNING, RMAppAttemptState.RUNNING,
                RMAppAttemptEventType.CONTAINER_ACQUIRED,
                new ContainerAcquiredTransition())
      .addTransition(
          RMAppAttemptState.RUNNING,
          EnumSet.of(RMAppAttemptState.RUNNING, RMAppAttemptState.FAILED),
          RMAppAttemptEventType.CONTAINER_FINISHED,
          new ContainerFinishedTransition())
      .addTransition(
          RMAppAttemptState.RUNNING, RMAppAttemptState.FAILED,
          RMAppAttemptEventType.EXPIRE,
          new FinalTransition(RMAppAttemptState.FAILED))
      .addTransition(
          RMAppAttemptState.RUNNING, RMAppAttemptState.KILLED,
          RMAppAttemptEventType.KILL,
          new FinalTransition(RMAppAttemptState.KILLED))

      // Transitions from FAILED State
      .addTransition(
          RMAppAttemptState.FAILED,
          RMAppAttemptState.FAILED,
          EnumSet.of(
              RMAppAttemptEventType.EXPIRE,
              RMAppAttemptEventType.KILL,
              RMAppAttemptEventType.UNREGISTERED,
              RMAppAttemptEventType.STATUS_UPDATE,
              RMAppAttemptEventType.CONTAINER_ALLOCATED,
              RMAppAttemptEventType.CONTAINER_FINISHED))

      // Transitions from FINISHED State
      .addTransition(
          RMAppAttemptState.FINISHED,
          RMAppAttemptState.FINISHED,
          EnumSet.of(
              RMAppAttemptEventType.EXPIRE,
              RMAppAttemptEventType.UNREGISTERED,
              RMAppAttemptEventType.CONTAINER_ALLOCATED,
              RMAppAttemptEventType.CONTAINER_FINISHED,
              RMAppAttemptEventType.KILL))

      // Transitions from KILLED State
      .addTransition(
          RMAppAttemptState.KILLED,
          RMAppAttemptState.KILLED,
          EnumSet.of(RMAppAttemptEventType.APP_ACCEPTED,
              RMAppAttemptEventType.APP_REJECTED,
              RMAppAttemptEventType.EXPIRE,
              RMAppAttemptEventType.LAUNCHED,
              RMAppAttemptEventType.LAUNCH_FAILED,
              RMAppAttemptEventType.EXPIRE,
              RMAppAttemptEventType.REGISTERED,
              RMAppAttemptEventType.CONTAINER_ALLOCATED,
              RMAppAttemptEventType.CONTAINER_FINISHED,
              RMAppAttemptEventType.UNREGISTERED,
              RMAppAttemptEventType.KILL,
              RMAppAttemptEventType.STATUS_UPDATE))

    .installTopology();

  public RMAppAttemptImpl(ApplicationAttemptId appAttemptId,
      String clientToken, RMContext rmContext, YarnScheduler scheduler,
      ApplicationMasterService masterService,
      ApplicationSubmissionContext submissionContext) {

    this.applicationAttemptId = appAttemptId;
    this.rmContext = rmContext;
    this.eventHandler = rmContext.getDispatcher().getEventHandler();
    this.submissionContext = submissionContext;
    this.scheduler = scheduler;
    this.masterService = masterService;
    this.clientToken = clientToken;

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    this.readLock = lock.readLock();
    this.writeLock = lock.writeLock();

    this.stateMachine = stateMachineFactory.make(this);
  }

  @Override
  public ApplicationAttemptId getAppAttemptId() {
    return this.applicationAttemptId;
  }

  @Override
  public ApplicationSubmissionContext getSubmissionContext() {
    return this.submissionContext;
  }

  @Override
  public FinalApplicationStatus getFinalApplicationStatus() {
    this.readLock.lock();
    try {
      return this.finalStatus;
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  public RMAppAttemptState getAppAttemptState() {
    this.readLock.lock();
    try {
      return this.stateMachine.getCurrentState();
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  public String getHost() {
    this.readLock.lock();

    try {
      return this.host;
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  public int getRpcPort() {
    this.readLock.lock();

    try {
      return this.rpcPort;
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  public String getTrackingUrl() {
    this.readLock.lock();

    try {
      return this.trackingUrl;
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  public String getClientToken() {
    return this.clientToken;
  }

  @Override
  public String getDiagnostics() {
    this.readLock.lock();

    try {
      return this.diagnostics.toString();
    } finally {
      this.readLock.unlock();
    }
  }

  public void setDiagnostics(String message) {
    this.writeLock.lock();

    try {
      this.diagnostics.append(message);
    } finally {
      this.writeLock.unlock();
    }
  }

  @Override
  public float getProgress() {
    this.readLock.lock();

    try {
      return this.progress;
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  public List<ContainerStatus> getJustFinishedContainers() {
    this.readLock.lock();
    try {
      return this.justFinishedContainers;
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  public List<ContainerStatus> pullJustFinishedContainers() {
    this.writeLock.lock();

    try {
      List<ContainerStatus> returnList = new ArrayList<ContainerStatus>(
          this.justFinishedContainers.size());
      returnList.addAll(this.justFinishedContainers);
      this.justFinishedContainers.clear();
      return returnList;
    } finally {
      this.writeLock.unlock();
    }
  }

  @Override
  public Set<NodeId> getRanNodes() {
    return ranNodes;
  }

  @Override
  public Container getMasterContainer() {
    this.readLock.lock();

    try {
      return this.masterContainer;
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  public void handle(RMAppAttemptEvent event) {

    this.writeLock.lock();

    try {
      ApplicationAttemptId appAttemptID = event.getApplicationAttemptId();
      LOG.info("Processing event for " + appAttemptID + " of type "
          + event.getType());
      final RMAppAttemptState oldState = getAppAttemptState();
      try {
        /* keep the master in sync with the state machine */
        this.stateMachine.doTransition(event.getType(), event);
      } catch (InvalidStateTransitonException e) {
        LOG.error("Can't handle this event at current state", e);
        /* TODO fail the application on the failed transition */
      }

      if (oldState != getAppAttemptState()) {
        LOG.info(appAttemptID + " State change from " + oldState + " to "
            + getAppAttemptState());
      }
    } finally {
      this.writeLock.unlock();
    }
  }

  @Override
  public ApplicationResourceUsageReport getApplicationResourceUsageReport() {
    this.readLock.lock();
    
    try {
      int numUsedContainers = 0;
      int numReservedContainers = 0;
      int reservedResources = 0;
      int currentConsumption = 0;
      SchedulerAppReport schedApp = 
          scheduler.getSchedulerAppInfo(this.getAppAttemptId());
      Collection<RMContainer> liveContainers;
      Collection<RMContainer> reservedContainers;
      if (schedApp != null) {
        liveContainers = schedApp.getLiveContainers();
        reservedContainers = schedApp.getReservedContainers();
        if (liveContainers != null) {
          numUsedContainers = liveContainers.size();
          for (RMContainer lc : liveContainers) {
            currentConsumption += lc.getContainer().getResource().getMemory();
          }
        }
        if (reservedContainers != null) {
          numReservedContainers = reservedContainers.size();
          for (RMContainer rc : reservedContainers) {
            reservedResources += rc.getContainer().getResource().getMemory();
          }
        }
      }
      
      ApplicationResourceUsageReport appResources = 
          recordFactory.newRecordInstance(ApplicationResourceUsageReport.class);
      appResources.setNumUsedContainers(numUsedContainers);
      appResources.setNumReservedContainers(numReservedContainers);
      appResources.setUsedResources(
          Resources.createResource(currentConsumption));
      appResources.setReservedResources(
          Resources.createResource(reservedResources));
      appResources.setNeededResources(
          Resources.createResource(currentConsumption + reservedResources));
      return appResources;
    } finally {
      this.readLock.unlock();
    }
  }

  private static class BaseTransition implements
      SingleArcTransition<RMAppAttemptImpl, RMAppAttemptEvent> {

    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {
    }

  }

  private static final class AttemptStartedTransition extends BaseTransition {
    @SuppressWarnings("unchecked")
	@Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      // Register with the ApplicationMasterService
      appAttempt.masterService
          .registerAppAttempt(appAttempt.applicationAttemptId);

      // Add the application to the scheduler
      appAttempt.eventHandler.handle(
          new AppAddedSchedulerEvent(appAttempt.applicationAttemptId,
              appAttempt.submissionContext.getQueue(),
              appAttempt.submissionContext.getUser()));
    }
  }

  private static final class AppRejectedTransition extends BaseTransition {
    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      RMAppAttemptRejectedEvent rejectedEvent = (RMAppAttemptRejectedEvent) event;

      // Save the diagnostic message
      String message = rejectedEvent.getMessage();
      appAttempt.setDiagnostics(message);

      // Send the rejection event to app
      appAttempt.eventHandler.handle(
          new RMAppRejectedEvent(
              rejectedEvent.getApplicationAttemptId().getApplicationId(),
              message)
          );
    }
  }

  private static final List<ContainerId> EMPTY_CONTAINER_RELEASE_LIST =
      new ArrayList<ContainerId>();
  private static final List<ResourceRequest> EMPTY_CONTAINER_REQUEST_LIST =
    new ArrayList<ResourceRequest>();

  private static final class ScheduleTransition extends BaseTransition {
    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      // Send the acceptance to the app
      appAttempt.eventHandler.handle(new RMAppEvent(event
          .getApplicationAttemptId().getApplicationId(),
          RMAppEventType.APP_ACCEPTED));

      // Request a container for the AM.
      ResourceRequest request = BuilderUtils.newResourceRequest(
          AM_CONTAINER_PRIORITY, "*", appAttempt.submissionContext
              .getAMContainerSpec().getResource(), 1);

      appAttempt.scheduler.allocate(appAttempt.applicationAttemptId,
          Collections.singletonList(request), EMPTY_CONTAINER_RELEASE_LIST);
    }
  }

  private static final class AMContainerAllocatedTransition extends BaseTransition {
    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      // Acquire the AM container from the scheduler.
      Allocation amContainerAllocation = appAttempt.scheduler.allocate(
          appAttempt.applicationAttemptId, EMPTY_CONTAINER_REQUEST_LIST,
          EMPTY_CONTAINER_RELEASE_LIST);

      // Set the masterContainer
      appAttempt.masterContainer = amContainerAllocation.getContainers().get(
          0);

      // Send event to launch the AM Container
      appAttempt.eventHandler.handle(new AMLauncherEvent(
          AMLauncherEventType.LAUNCH, appAttempt));
    }
  }

  private static class BaseFinalTransition extends BaseTransition {

    private final RMAppAttemptState finalAttemptState;

    public BaseFinalTransition(RMAppAttemptState finalAttemptState) {
      this.finalAttemptState = finalAttemptState;
    }

    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      // Tell the AMS. Unregister from the ApplicationMasterService
      appAttempt.masterService
          .unregisterAttempt(appAttempt.applicationAttemptId);

      // Tell the application and the scheduler
      ApplicationId applicationId = appAttempt.getAppAttemptId().getApplicationId();
      RMAppEvent appEvent = null;
      switch (finalAttemptState) {
        case FINISHED:
        {
          appEvent =
              new RMAppEvent(applicationId, RMAppEventType.ATTEMPT_FINISHED);
        }
        break;
        case KILLED:
        {
          appEvent =
              new RMAppFailedAttemptEvent(applicationId,
                  RMAppEventType.ATTEMPT_KILLED,
                  "Application killed by user.");
        }
        break;
        case FAILED:
        {
          appEvent =
              new RMAppFailedAttemptEvent(applicationId,
                  RMAppEventType.ATTEMPT_FAILED,
                  appAttempt.getDiagnostics());
        }
        break;
        default:
        {
          LOG.error("Cannot get this state!! Error!!");
        }
        break;
      }

      appAttempt.eventHandler.handle(appEvent);
      appAttempt.eventHandler.handle(new AppRemovedSchedulerEvent(appAttempt
          .getAppAttemptId(), finalAttemptState));
    }
  }

  private static final class AMLaunchedTransition extends BaseTransition {
    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      // Register with AMLivelinessMonitor
      appAttempt.rmContext.getAMLivelinessMonitor().register(
          appAttempt.applicationAttemptId);

    }
  }

  private static final class LaunchFailedTransition extends BaseFinalTransition {

    public LaunchFailedTransition() {
      super(RMAppAttemptState.FAILED);
    }

    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      // Use diagnostic from launcher
      RMAppAttemptLaunchFailedEvent launchFaileEvent
        = (RMAppAttemptLaunchFailedEvent) event;
      appAttempt.diagnostics.append(launchFaileEvent.getMessage());

      // Tell the app, scheduler
      super.transition(appAttempt, event);

    }
  }

  private static final class KillAllocatedAMTransition extends
      BaseFinalTransition {
    public KillAllocatedAMTransition() {
      super(RMAppAttemptState.KILLED);
    }

    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      // Tell the application and scheduler
      super.transition(appAttempt, event);

      // Tell the launcher to cleanup.
      appAttempt.eventHandler.handle(new AMLauncherEvent(
          AMLauncherEventType.CLEANUP, appAttempt));

    }
  }

  private static final class AMRegisteredTransition extends BaseTransition {
    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      RMAppAttemptRegistrationEvent registrationEvent
          = (RMAppAttemptRegistrationEvent) event;
      appAttempt.host = registrationEvent.getHost();
      appAttempt.rpcPort = registrationEvent.getRpcport();
      appAttempt.trackingUrl = registrationEvent.getTrackingurl();

      // Let the app know
      appAttempt.eventHandler.handle(new RMAppEvent(appAttempt
          .getAppAttemptId().getApplicationId(),
          RMAppEventType.ATTEMPT_REGISTERED));
    }
  }

  private static final class AMContainerCrashedTransition extends
      BaseFinalTransition {

    public AMContainerCrashedTransition() {
      super(RMAppAttemptState.FAILED);
    }

    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      RMAppAttemptContainerFinishedEvent finishEvent =
          ((RMAppAttemptContainerFinishedEvent)event);

      // UnRegister from AMLivelinessMonitor
      appAttempt.rmContext.getAMLivelinessMonitor().unregister(
          appAttempt.getAppAttemptId());

      // Setup diagnostic message
      ContainerStatus status = finishEvent.getContainerStatus();
      appAttempt.diagnostics.append("AM Container for " +
          appAttempt.getAppAttemptId() + " exited with " +
          " exitCode: " + status.getExitStatus() +
          " due to: " +  status.getDiagnostics() + "." +
          "Failing this attempt.");

      // Tell the app, scheduler
      super.transition(appAttempt, finishEvent);
    }
  }

  private static class FinalTransition extends BaseFinalTransition {

    public FinalTransition(RMAppAttemptState finalAttemptState) {
      super(finalAttemptState);
    }

    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      appAttempt.progress = 1.0f;

      // Tell the app and the scheduler
      super.transition(appAttempt, event);

      // UnRegister from AMLivelinessMonitor
      appAttempt.rmContext.getAMLivelinessMonitor().unregister(
          appAttempt.getAppAttemptId());

      // Tell the launcher to cleanup.
      appAttempt.eventHandler.handle(new AMLauncherEvent(
          AMLauncherEventType.CLEANUP, appAttempt));
    }
  }

  private static final class StatusUpdateTransition extends
      BaseTransition {
    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      RMAppAttemptStatusupdateEvent statusUpdateEvent
        = (RMAppAttemptStatusupdateEvent) event;

      // Update progress
      appAttempt.progress = statusUpdateEvent.getProgress();

      // Ping to AMLivelinessMonitor
      appAttempt.rmContext.getAMLivelinessMonitor().receivedPing(
          statusUpdateEvent.getApplicationAttemptId());
    }
  }

  private static final class AMUnregisteredTransition extends FinalTransition {

    public AMUnregisteredTransition() {
      super(RMAppAttemptState.FINISHED);
    }

    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      RMAppAttemptUnregistrationEvent unregisterEvent
        = (RMAppAttemptUnregistrationEvent) event;
      appAttempt.diagnostics.append(unregisterEvent.getDiagnostics());
      appAttempt.trackingUrl = unregisterEvent.getTrackingUrl();
      appAttempt.finalStatus = unregisterEvent.getFinalApplicationStatus();

      // Tell the app and the scheduler
      super.transition(appAttempt, event);
    }
  }

  private static final class ContainerAcquiredTransition extends
      BaseTransition {
    @Override
    public void transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {
      RMAppAttemptContainerAcquiredEvent acquiredEvent
        = (RMAppAttemptContainerAcquiredEvent) event;
      appAttempt.ranNodes.add(acquiredEvent.getContainer().getNodeId());
    }
  }

  private static final class ContainerFinishedTransition
      implements
      MultipleArcTransition<RMAppAttemptImpl, RMAppAttemptEvent, RMAppAttemptState> {

    @Override
    public RMAppAttemptState transition(RMAppAttemptImpl appAttempt,
        RMAppAttemptEvent event) {

      RMAppAttemptContainerFinishedEvent containerFinishedEvent
        = (RMAppAttemptContainerFinishedEvent) event;
      ContainerStatus containerStatus =
          containerFinishedEvent.getContainerStatus();

      // Is this container the AmContainer? If the finished container is same as
      // the AMContainer, AppAttempt fails
      if (appAttempt.masterContainer.getId().equals(
          containerStatus.getContainerId())) {
        // Setup diagnostic message
        appAttempt.diagnostics.append("AM Container for " +
            appAttempt.getAppAttemptId() + " exited with " +
            " exitCode: " + containerStatus.getExitStatus() +
            " due to: " +  containerStatus.getDiagnostics() + "." +
            "Failing this attempt.");

        /*
         * In the case when the AM dies, the trackingUrl is left pointing to the AM's
         * URL, which shows up in the scheduler UI as a broken link. Setting it here
         * to empty string will prevent any link from being displayed.
         * NOTE: don't set trackingUrl to 'null'. That will cause null-pointer exceptions
         * in the generated proto code.
         */
        appAttempt.trackingUrl = "";

        new FinalTransition(RMAppAttemptState.FAILED).transition(
            appAttempt, containerFinishedEvent);
        return RMAppAttemptState.FAILED;
      }

      // Normal container.

      // Put it in completedcontainers list
      appAttempt.justFinishedContainers.add(containerStatus);
      return RMAppAttemptState.RUNNING;
    }
  }
}
