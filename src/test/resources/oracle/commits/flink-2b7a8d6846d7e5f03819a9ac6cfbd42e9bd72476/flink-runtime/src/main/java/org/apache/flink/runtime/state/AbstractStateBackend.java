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

package org.apache.flink.runtime.state;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.state.KeyGroupAssigner;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.runtime.execution.Environment;
import org.apache.flink.runtime.query.TaskKvStateRegistry;

import java.io.IOException;
import java.util.List;

/**
 * A state backend defines how state is stored and snapshotted during checkpoints.
 */
public abstract class AbstractStateBackend implements java.io.Serializable {
	private static final long serialVersionUID = 4620415814639230247L;

	/**
	 * Creates a {@link CheckpointStreamFactory} that can be used to create streams
	 * that should end up in a checkpoint.
	 *
	 * @param jobId The {@link JobID} of the job for which we are creating checkpoint streams.
	 * @param operatorIdentifier An identifier of the operator for which we create streams.
	 */
	public abstract CheckpointStreamFactory createStreamFactory(
			JobID jobId,
			String operatorIdentifier) throws IOException;

	/**
	 * Creates a new {@link KeyedStateBackend} that is responsible for keeping keyed state
	 * and can be checkpointed to checkpoint streams.
	 */
	public abstract <K> KeyedStateBackend<K> createKeyedStateBackend(
			Environment env,
			JobID jobID,
			String operatorIdentifier,
			TypeSerializer<K> keySerializer,
			KeyGroupAssigner<K> keyGroupAssigner,
			KeyGroupRange keyGroupRange,
			TaskKvStateRegistry kvStateRegistry) throws Exception;

	/**
	 * Creates a new {@link KeyedStateBackend} that restores its state from the given list
	 * {@link KeyGroupsStateHandle KeyGroupStateHandles}.
	 */
	public abstract <K> KeyedStateBackend<K> restoreKeyedStateBackend(
			Environment env,
			JobID jobID,
			String operatorIdentifier,
			TypeSerializer<K> keySerializer,
			KeyGroupAssigner<K> keyGroupAssigner,
			KeyGroupRange keyGroupRange,
			List<KeyGroupsStateHandle> restoredState,
			TaskKvStateRegistry kvStateRegistry) throws Exception;

}
