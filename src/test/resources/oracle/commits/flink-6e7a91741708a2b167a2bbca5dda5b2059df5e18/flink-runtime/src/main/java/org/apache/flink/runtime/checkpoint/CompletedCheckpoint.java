/*
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

package org.apache.flink.runtime.checkpoint;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.checkpoint.savepoint.SavepointStore;
import org.apache.flink.runtime.jobgraph.JobStatus;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.state.StateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * A successful checkpoint describes a checkpoint after all required tasks acknowledged it (with their state)
 * and that is considered completed.
 */
public class CompletedCheckpoint implements Serializable {

	private static final Logger LOG = LoggerFactory.getLogger(CompletedCheckpoint.class);

	private static final long serialVersionUID = -8360248179615702014L;

	private final JobID job;

	private final long checkpointID;

	/** The timestamp when the checkpoint was triggered. */
	private final long timestamp;

	/** The duration of the checkpoint (completion timestamp - trigger timestamp). */
	private final long duration;

	/** States of the different task groups belonging to this checkpoint */
	private final Map<JobVertexID, TaskState> taskStates;

	/** Properties for this checkpoint. */
	private final CheckpointProperties props;

	/** External path if persisted checkpoint; <code>null</code> otherwise. */
	private final String externalPath;

	/** Optional stats tracker callback for discard. */
	@Nullable
	private transient CompletedCheckpointStats.DiscardCallback discardCallback;

	// ------------------------------------------------------------------------

	public CompletedCheckpoint(
			JobID job,
			long checkpointID,
			long timestamp,
			long completionTimestamp,
			Map<JobVertexID, TaskState> taskStates) {

		this(job, checkpointID, timestamp, completionTimestamp, taskStates, CheckpointProperties.forStandardCheckpoint(), null);
	}

	public CompletedCheckpoint(
			JobID job,
			long checkpointID,
			long timestamp,
			long completionTimestamp,
			Map<JobVertexID, TaskState> taskStates,
			CheckpointProperties props,
			String externalPath) {

		checkArgument(checkpointID >= 0);
		checkArgument(timestamp >= 0);
		checkArgument(completionTimestamp >= 0);

		this.job = checkNotNull(job);
		this.checkpointID = checkpointID;
		this.timestamp = timestamp;
		this.duration = completionTimestamp - timestamp;
		this.taskStates = checkNotNull(taskStates);
		this.props = checkNotNull(props);
		this.externalPath = externalPath;

		if (props.externalizeCheckpoint() && externalPath == null) {
			throw new NullPointerException("Checkpoint properties say that the checkpoint " +
					"should have been persisted, but missing external path.");
		}
	}

	// ------------------------------------------------------------------------

	public JobID getJobId() {
		return job;
	}

	public long getCheckpointID() {
		return checkpointID;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getDuration() {
		return duration;
	}

	public CheckpointProperties getProperties() {
		return props;
	}

	public boolean subsume() throws Exception {
		if (props.discardOnSubsumed()) {
			discard();
			return true;
		}

		return false;
	}

	public boolean discard(JobStatus jobStatus) throws Exception {
		if (jobStatus == JobStatus.FINISHED && props.discardOnJobFinished() ||
				jobStatus == JobStatus.CANCELED && props.discardOnJobCancelled() ||
				jobStatus == JobStatus.FAILED && props.discardOnJobFailed() ||
				jobStatus == JobStatus.SUSPENDED && props.discardOnJobSuspended()) {

			discard();
			return true;
		} else {
			if (externalPath != null) {
				LOG.info("Persistent checkpoint with ID {} at '{}' not discarded.",
						checkpointID,
						externalPath);
			}

			return false;
		}
	}

	void discard() throws Exception {
		try {
			if (externalPath != null) {
				SavepointStore.removeSavepointFile(externalPath);
			}

			StateUtil.bestEffortDiscardAllStateObjects(taskStates.values());
		} finally {
			taskStates.clear();

			if (discardCallback != null) {
				discardCallback.notifyDiscardedCheckpoint();
			}
		}
	}

	public long getStateSize() {
		long result = 0L;

		for (TaskState taskState : taskStates.values()) {
			result += taskState.getStateSize();
		}

		return result;
	}

	public Map<JobVertexID, TaskState> getTaskStates() {
		return taskStates;
	}

	public TaskState getTaskState(JobVertexID jobVertexID) {
		return taskStates.get(jobVertexID);
	}

	public String getExternalPath() {
		return externalPath;
	}

	/**
	 * Sets the callback for tracking when this checkpoint is discarded.
	 *
	 * @param discardCallback Callback to call when the checkpoint is discarded.
	 */
	void setDiscardCallback(@Nullable CompletedCheckpointStats.DiscardCallback discardCallback) {
		this.discardCallback = discardCallback;
	}

	// --------------------------------------------------------------------------------------------

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CompletedCheckpoint) {
			CompletedCheckpoint other = (CompletedCheckpoint) obj;

			return job.equals(other.job) && checkpointID == other.checkpointID &&
				timestamp == other.timestamp && duration == other.duration &&
				taskStates.equals(other.taskStates);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (int) (this.checkpointID ^ this.checkpointID >>> 32) +
			31 * ((int) (this.timestamp ^ this.timestamp >>> 32) +
				31 * ((int) (this.duration ^ this.duration >>> 32) +
					31 * Objects.hash(job, taskStates)));
	}

	@Override
	public String toString() {
		return String.format("Checkpoint %d @ %d for %s", checkpointID, timestamp, job);
	}

}
