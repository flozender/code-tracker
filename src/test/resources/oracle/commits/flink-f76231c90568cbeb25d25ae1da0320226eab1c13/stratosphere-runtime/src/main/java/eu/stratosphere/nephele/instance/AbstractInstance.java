/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.nephele.instance;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import eu.stratosphere.nephele.deployment.TaskDeploymentDescriptor;
import eu.stratosphere.nephele.execution.librarycache.LibraryCacheManager;
import eu.stratosphere.nephele.execution.librarycache.LibraryCacheProfileRequest;
import eu.stratosphere.nephele.execution.librarycache.LibraryCacheProfileResponse;
import eu.stratosphere.nephele.execution.librarycache.LibraryCacheUpdate;
import eu.stratosphere.nephele.executiongraph.ExecutionVertexID;
import eu.stratosphere.nephele.io.channels.ChannelID;
import eu.stratosphere.nephele.ipc.RPC;
import eu.stratosphere.nephele.jobgraph.JobID;
import eu.stratosphere.nephele.net.NetUtils;
import eu.stratosphere.nephele.protocols.TaskOperationProtocol;
import eu.stratosphere.nephele.taskmanager.TaskCancelResult;
import eu.stratosphere.nephele.taskmanager.TaskKillResult;
import eu.stratosphere.nephele.taskmanager.TaskSubmissionResult;
import eu.stratosphere.nephele.topology.NetworkNode;
import eu.stratosphere.nephele.topology.NetworkTopology;

/**
 * An abstract instance represents a resource a {@link eu.stratosphere.nephele.taskmanager.TaskManager} runs on.
 * 
 * @author warneke
 */
public abstract class AbstractInstance extends NetworkNode {

	/**
	 * The type of the instance.
	 */
	private final InstanceType instanceType;

	/**
	 * The connection info identifying the instance.
	 */
	private final InstanceConnectionInfo instanceConnectionInfo;

	/**
	 * The hardware description as reported by the instance itself.
	 */
	private final HardwareDescription hardwareDescription;

	/**
	 * Stores the RPC stub object for the instance's task manager.
	 */
	private TaskOperationProtocol taskManager = null;

	/**
	 * Constructs an abstract instance object.
	 * 
	 * @param instanceType
	 *        the type of the instance
	 * @param instanceConnectionInfo
	 *        the connection info identifying the instance
	 * @param parentNode
	 *        the parent node in the network topology
	 * @param networkTopology
	 *        the network topology this node is a part of
	 * @param hardwareDescription
	 *        the hardware description provided by the instance itself
	 */
	public AbstractInstance(final InstanceType instanceType, final InstanceConnectionInfo instanceConnectionInfo,
			final NetworkNode parentNode, final NetworkTopology networkTopology,
			final HardwareDescription hardwareDescription) {
		super((instanceConnectionInfo == null) ? null : instanceConnectionInfo.toString(), parentNode, networkTopology);
		this.instanceType = instanceType;
		this.instanceConnectionInfo = instanceConnectionInfo;
		this.hardwareDescription = hardwareDescription;
	}

	/**
	 * Creates or returns the RPC stub object for the instance's task manager.
	 * 
	 * @return the RPC stub object for the instance's task manager
	 * @throws IOException
	 *         thrown if the RPC stub object for the task manager cannot be created
	 */
	private TaskOperationProtocol getTaskManagerProxy() throws IOException {

		if (this.taskManager == null) {

			this.taskManager = RPC.getProxy(TaskOperationProtocol.class,
				new InetSocketAddress(getInstanceConnectionInfo().getAddress(),
					getInstanceConnectionInfo().getIPCPort()), NetUtils.getSocketFactory());
		}

		return this.taskManager;
	}

	/**
	 * Destroys and removes the RPC stub object for this instance's task manager.
	 */
	private void destroyTaskManagerProxy() {

		if (this.taskManager != null) {
			RPC.stopProxy(this.taskManager);
			this.taskManager = null;
		}
	}

	/**
	 * Returns the type of the instance.
	 * 
	 * @return the type of the instance
	 */
	public final InstanceType getType() {
		return this.instanceType;
	}

	/**
	 * Returns the instance's connection information object.
	 * 
	 * @return the instance's connection information object
	 */
	public final InstanceConnectionInfo getInstanceConnectionInfo() {
		return this.instanceConnectionInfo;
	}

	/**
	 * Returns the instance's hardware description as reported by the instance itself.
	 * 
	 * @return the instance's hardware description
	 */
	public HardwareDescription getHardwareDescription() {
		return this.hardwareDescription;
	}

	/**
	 * Checks if all the libraries required to run the job with the given
	 * job ID are available on this instance. Any libary that is missing
	 * is transferred to the instance as a result of this call.
	 * 
	 * @param jobID
	 *        the ID of the job whose libraries are to be checked for
	 * @throws IOException
	 *         thrown if an error occurs while checking for the libraries
	 */
	public synchronized void checkLibraryAvailability(final JobID jobID) throws IOException {

		// Now distribute the required libraries for the job
		String[] requiredLibraries = LibraryCacheManager.getRequiredJarFiles(jobID);

		if (requiredLibraries == null)
			throw new IOException("No entry of required libraries for job " + jobID);

		LibraryCacheProfileRequest request = new LibraryCacheProfileRequest();
		request.setRequiredLibraries(requiredLibraries);

		// Send the request
		LibraryCacheProfileResponse response = null;
		response = getTaskManagerProxy().getLibraryCacheProfile(request);

		// Check response and transfer libraries if necessary
		for (int k = 0; k < requiredLibraries.length; k++) {
			if (!response.isCached(k)) {
				LibraryCacheUpdate update = new LibraryCacheUpdate(requiredLibraries[k]);
				getTaskManagerProxy().updateLibraryCache(update);
			}
		}
	}

	/**
	 * Submits a list of tasks to the instance's {@link eu.stratosphere.nephele.taskmanager.TaskManager}.
	 * 
	 * @param tasks
	 *        the list of tasks to be submitted
	 * @return the result of the submission attempt
	 * @throws IOException
	 *         thrown if an error occurs while transmitting the task
	 */
	public synchronized List<TaskSubmissionResult> submitTasks(final List<TaskDeploymentDescriptor> tasks)
			throws IOException {

		return getTaskManagerProxy().submitTasks(tasks);
	}

	/**
	 * Cancels the task identified by the given ID at the instance's
	 * {@link eu.stratosphere.nephele.taskmanager.TaskManager}.
	 * 
	 * @param id
	 *        the ID identifying the task to be canceled
	 * @throws IOException
	 *         thrown if an error occurs while transmitting the request or receiving the response
	 * @return the result of the cancel attempt
	 */
	public synchronized TaskCancelResult cancelTask(final ExecutionVertexID id) throws IOException {

		return getTaskManagerProxy().cancelTask(id);
	}

	/**
	 * Kills the task identified by the given ID at the instance's
	 * {@link eu.stratosphere.nephele.taskmanager.TaskManager}.
	 * 
	 * @param id
	 *        the ID identifying the task to be killed
	 * @throws IOException
	 *         thrown if an error occurs while transmitting the request or receiving the response
	 * @return the result of the kill attempt
	 */
	public synchronized TaskKillResult killTask(final ExecutionVertexID id) throws IOException {

		return getTaskManagerProxy().killTask(id);
	}


	@Override
	public boolean equals(final Object obj) {

		// Fall back since dummy instances do not have a instanceConnectionInfo
		if (this.instanceConnectionInfo == null) {
			return super.equals(obj);
		}

		if (!(obj instanceof AbstractInstance)) {
			return false;
		}

		final AbstractInstance abstractInstance = (AbstractInstance) obj;

		return this.instanceConnectionInfo.equals(abstractInstance.getInstanceConnectionInfo());
	}


	@Override
	public int hashCode() {

		// Fall back since dummy instances do not have a instanceConnectionInfo
		if (this.instanceConnectionInfo == null) {
			return super.hashCode();
		}

		return this.instanceConnectionInfo.hashCode();
	}

	/**
	 * Triggers the remote task manager to print out the current utilization of its read and write buffers to its logs.
	 * 
	 * @throws IOException
	 *         thrown if an error occurs while transmitting the request
	 */
	public synchronized void logBufferUtilization() throws IOException {

		getTaskManagerProxy().logBufferUtilization();
	}

	/**
	 * Kills the task manager running on this instance. This method is mainly intended to test and debug Nephele's fault
	 * tolerance mechanisms.
	 * 
	 * @throws IOException
	 *         thrown if an error occurs while transmitting the request
	 */
	public synchronized void killTaskManager() throws IOException {

		getTaskManagerProxy().killTaskManager();
	}

	/**
	 * Invalidates the entries identified by the given channel IDs from the remote task manager's receiver lookup cache.
	 * 
	 * @param channelIDs
	 *        the channel IDs identifying the cache entries to invalidate
	 * @throws IOException
	 *         thrown if an error occurs during this remote procedure call
	 */
	public synchronized void invalidateLookupCacheEntries(final Set<ChannelID> channelIDs) throws IOException {

		getTaskManagerProxy().invalidateLookupCacheEntries(channelIDs);
	}

	/**
	 * Destroys all RPC stub objects attached to this instance.
	 */
	public synchronized void destroyProxies() {

		destroyTaskManagerProxy();

	}
}
