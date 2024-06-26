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
package org.apache.flink.streaming.runtime.operators;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.core.memory.DataInputViewStreamWrapper;
import org.apache.flink.core.memory.DataOutputViewStreamWrapper;
import org.apache.flink.runtime.io.disk.InputViewIterator;
import org.apache.flink.runtime.state.AbstractStateBackend;
import org.apache.flink.runtime.state.StreamStateHandle;
import org.apache.flink.runtime.util.ReusingMutableToRegularIteratorWrapper;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.util.InstantiationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Generic Sink that emits its input elements into an arbitrary backend. This sink is integrated with the checkpointing
 * mechanism and can provide exactly-once guarantees; depending on the storage backend and sink/committer implementation.
 * <p/>
 * Incoming records are stored within a {@link org.apache.flink.runtime.state.AbstractStateBackend}, and only committed if a
 * checkpoint is completed.
 *
 * @param <IN> Type of the elements emitted by this sink
 */
public abstract class GenericWriteAheadSink<IN> extends AbstractStreamOperator<IN> implements OneInputStreamOperator<IN, IN> {
	private static final long serialVersionUID = 1L;

	protected static final Logger LOG = LoggerFactory.getLogger(GenericWriteAheadSink.class);
	private final CheckpointCommitter committer;
	private transient AbstractStateBackend.CheckpointStateOutputStream out;
	protected final TypeSerializer<IN> serializer;
	private final String id;

	private ExactlyOnceState state = new ExactlyOnceState();

	public GenericWriteAheadSink(CheckpointCommitter committer, TypeSerializer<IN> serializer, String jobID) throws Exception {
		this.committer = committer;
		this.serializer = serializer;
		this.id = UUID.randomUUID().toString();
		this.committer.setJobId(jobID);
		this.committer.createResource();
	}

	@Override
	public void open() throws Exception {
		committer.setOperatorId(id);
		committer.setOperatorSubtaskId(getRuntimeContext().getIndexOfThisSubtask());
		committer.open();
		cleanState();
	}

	public void close() throws Exception {
		committer.close();
	}

	/**
	 * Saves a handle in the state.
	 *
	 * @param checkpointId
	 * @throws IOException
	 */
	private void saveHandleInState(final long checkpointId, final long timestamp) throws Exception {
		//only add handle if a new OperatorState was created since the last snapshot
		if (out != null) {
			StreamStateHandle handle = out.closeAndGetHandle();
			if (state.pendingHandles.containsKey(checkpointId)) {
				//we already have a checkpoint stored for that ID that may have been partially written,
				//so we discard this "alternate version" and use the stored checkpoint
				handle.discardState();
			} else {
				state.pendingHandles.put(checkpointId, new Tuple2<>(timestamp, handle));
			}
			out = null;
		}
	}

	@Override
	public void snapshotState(FSDataOutputStream out,
			long checkpointId,
			long timestamp) throws Exception {
		super.snapshotState(out, checkpointId, timestamp);

		saveHandleInState(checkpointId, timestamp);

		InstantiationUtil.serializeObject(out, state);
	}

	@Override
	public void restoreState(FSDataInputStream in) throws Exception {
		super.restoreState(in);

		this.state = InstantiationUtil.deserializeObject(in, getUserCodeClassloader());
	}

	private void cleanState() throws Exception {
		synchronized (this.state.pendingHandles) { //remove all handles that were already committed
			Set<Long> pastCheckpointIds = this.state.pendingHandles.keySet();
			Set<Long> checkpointsToRemove = new HashSet<>();
			for (Long pastCheckpointId : pastCheckpointIds) {
				if (committer.isCheckpointCommitted(pastCheckpointId)) {
					checkpointsToRemove.add(pastCheckpointId);
				}
			}
			for (Long toRemove : checkpointsToRemove) {
				this.state.pendingHandles.remove(toRemove);
			}
		}
	}

	@Override
	public void notifyOfCompletedCheckpoint(long checkpointId) throws Exception {
		super.notifyOfCompletedCheckpoint(checkpointId);

		synchronized (state.pendingHandles) {
			Set<Long> pastCheckpointIds = state.pendingHandles.keySet();
			Set<Long> checkpointsToRemove = new HashSet<>();
			for (Long pastCheckpointId : pastCheckpointIds) {
				if (pastCheckpointId <= checkpointId) {
					try {
						if (!committer.isCheckpointCommitted(pastCheckpointId)) {
							Tuple2<Long, StreamStateHandle> handle = state.pendingHandles.get(pastCheckpointId);
							FSDataInputStream in = handle.f1.openInputStream();
							boolean success = sendValues(new ReusingMutableToRegularIteratorWrapper<>(new InputViewIterator<>(new DataInputViewStreamWrapper(in), serializer), serializer), handle.f0);
							if (success) { //if the sending has failed we will retry on the next notify
								committer.commitCheckpoint(pastCheckpointId);
								checkpointsToRemove.add(pastCheckpointId);
							}
						} else {
							checkpointsToRemove.add(pastCheckpointId);
						}
					} catch (Exception e) {
						LOG.error("Could not commit checkpoint.", e);
						break; // we have to break here to prevent a new checkpoint from being committed before this one
					}
				}
			}
			for (Long toRemove : checkpointsToRemove) {
				Tuple2<Long, StreamStateHandle> handle = state.pendingHandles.get(toRemove);
				state.pendingHandles.remove(toRemove);
				handle.f1.discardState();
			}
		}
	}


	/**
	 * Write the given element into the backend.
	 *
	 * @param value value to be written
	 * @return true, if the sending was successful, false otherwise
	 * @throws Exception
	 */
	protected abstract boolean sendValues(Iterable<IN> value, long timestamp) throws Exception;

	@Override
	public void processElement(StreamRecord<IN> element) throws Exception {
		IN value = element.getValue();
		//generate initial operator state
		if (out == null) {
			out = getStateBackend().createCheckpointStateOutputStream(0, 0);
		}
		serializer.serialize(value, new DataOutputViewStreamWrapper(out));
	}

	@Override
	public void processWatermark(Watermark mark) throws Exception {
		//don't do anything, since we are a sink
	}

	/**
	 * This state is used to keep a list of all StateHandles (essentially references to past OperatorStates) that were
	 * used since the last completed checkpoint.
	 **/
	public static class ExactlyOnceState implements Serializable {

		private static final long serialVersionUID = -3571063495273460743L;

		protected TreeMap<Long, Tuple2<Long, StreamStateHandle>> pendingHandles;

		public ExactlyOnceState() {
			pendingHandles = new TreeMap<>();
		}

		public TreeMap<Long, Tuple2<Long, StreamStateHandle>> getState(ClassLoader userCodeClassLoader) throws Exception {
			return pendingHandles;
		}

		@Override
		public String toString() {
			return this.pendingHandles.toString();
		}
	}
}
