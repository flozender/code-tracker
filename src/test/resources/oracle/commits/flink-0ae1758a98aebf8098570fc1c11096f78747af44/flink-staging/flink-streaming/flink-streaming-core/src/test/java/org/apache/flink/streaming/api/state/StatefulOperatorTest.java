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

package org.apache.flink.streaming.api.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.OperatorState;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.operators.testutils.MockEnvironment;
import org.apache.flink.runtime.operators.testutils.MockInputSplitProvider;
import org.apache.flink.runtime.state.LocalStateHandle.LocalStateHandleProvider;
import org.apache.flink.runtime.state.PartitionedStateHandle;
import org.apache.flink.runtime.state.StateHandle;
import org.apache.flink.streaming.api.checkpoint.Checkpointed;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.api.operators.StreamMap;
import org.apache.flink.streaming.runtime.tasks.StreamingRuntimeContext;
import org.apache.flink.util.InstantiationUtil;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test the functionality supported by stateful user functions for both
 * partitioned and non-partitioned user states. This test mimics the runtime
 * behavior of stateful stream operators.
 */
public class StatefulOperatorTest {

	@Test
	public void simpleStateTest() throws Exception {

		List<String> out = new ArrayList<String>();

		StreamMap<Integer, String> map = createOperatorWithContext(out, new ModKey(2), null);
		StreamingRuntimeContext context = map.getRuntimeContext();

		processInputs(map, Arrays.asList(1, 2, 3, 4, 5));

		assertEquals(Arrays.asList("1", "2", "3", "4", "5"), out);
		assertEquals((Integer) 5, context.getOperatorState("counter", 0, false).getState());
		assertEquals(ImmutableMap.of(0, 2, 1, 3), context.getOperatorStates().get("groupCounter").getPartitionedState());
		assertEquals("12345", context.getOperatorState("concat", "", false).getState());
		assertEquals((Integer) 5, ((StatefulMapper) map.getUserFunction()).checkpointedCounter);

		byte[] serializedState = InstantiationUtil.serializeObject(map.getStateSnapshotFromFunction(1, 1));

		StreamMap<Integer, String> restoredMap = createOperatorWithContext(out, new ModKey(2), serializedState);
		StreamingRuntimeContext restoredContext = restoredMap.getRuntimeContext();

		assertEquals((Integer) 5, restoredContext.getOperatorState("counter", 0, false).getState());
		assertEquals(ImmutableMap.of(0, 2, 1, 3), context.getOperatorStates().get("groupCounter").getPartitionedState());
		assertEquals("12345", restoredContext.getOperatorState("concat", "", false).getState());
		assertEquals((Integer) 5, ((StatefulMapper) restoredMap.getUserFunction()).checkpointedCounter);
		out.clear();

		processInputs(restoredMap, Arrays.asList(7, 8));

		assertEquals(Arrays.asList("7", "8"), out);
		assertEquals((Integer) 7, restoredContext.getOperatorState("counter", 0, false).getState());
		assertEquals(ImmutableMap.of(0, 3, 1, 4), restoredContext.getOperatorStates().get("groupCounter")
				.getPartitionedState());
		assertEquals("1234578", restoredContext.getOperatorState("concat", "", false).getState());
		assertEquals((Integer) 7, ((StatefulMapper) restoredMap.getUserFunction()).checkpointedCounter);

	}

	private void processInputs(StreamMap<Integer, ?> map, List<Integer> input) throws Exception {
		for (Integer i : input) {
			map.getRuntimeContext().setNextInput(i);
			map.processElement(i);
		}
	}

	@SuppressWarnings("unchecked")
	private StreamMap<Integer, String> createOperatorWithContext(List<String> output,
			KeySelector<Integer, Serializable> partitioner, byte[] serializedState) throws Exception {
		final List<String> outputList = output;

		StreamingRuntimeContext context = new StreamingRuntimeContext("MockTask", new MockEnvironment(3 * 1024 * 1024,
				new MockInputSplitProvider(), 1024), null, new ExecutionConfig(), partitioner,
				new LocalStateHandleProvider<Serializable>());

		StreamMap<Integer, String> op = new StreamMap<Integer, String>(new StatefulMapper());

		op.setup(new Output<String>() {

			@Override
			public void collect(String record) {
				outputList.add(record);
			}

			@Override
			public void close() {
			}
		}, context);

		if (serializedState != null) {
			op.restoreInitialState((Tuple2<StateHandle<Serializable>, Map<String, PartitionedStateHandle>>) InstantiationUtil
					.deserializeObject(serializedState, Thread.currentThread()
							.getContextClassLoader()));
		}

		op.open(null);

		return op;
	}

	public static class StatefulMapper extends RichMapFunction<Integer, String> implements
			Checkpointed<Integer> {
	private static final long serialVersionUID = -9007873655253339356L;
		OperatorState<Integer> counter;
		OperatorState<Integer> groupCounter;
		OperatorState<String> concat;
		
		Integer checkpointedCounter = 0;

		@Override
		public String map(Integer value) throws Exception {
			counter.updateState(counter.getState() + 1);
			groupCounter.updateState(groupCounter.getState() + 1);
			concat.updateState(concat.getState() + value.toString());
			checkpointedCounter++;
			try {
				counter.updateState(null);
				fail();
			} catch (RuntimeException e){
			}
			return value.toString();
		}

		@Override
		public void open(Configuration conf) throws IOException {
			counter = getRuntimeContext().getOperatorState("counter", 0, false);
			groupCounter = getRuntimeContext().getOperatorState("groupCounter", 0, true);
			concat = getRuntimeContext().getOperatorState("concat", "", false);
			try {
				getRuntimeContext().getOperatorState("test", null, true);
				fail();
			} catch (RuntimeException e){
			}
			try {
				getRuntimeContext().getOperatorState("test", null, true, null);
				fail();
			} catch (RuntimeException e){
			}
		}

		@Override
		public Integer snapshotState(long checkpointId, long checkpointTimestamp)
				throws Exception {
			return checkpointedCounter;
		}

		@Override
		public void restoreState(Integer state) {
			this.checkpointedCounter = (Integer) state;
		}
	}
	
	public static class ModKey implements KeySelector<Integer, Serializable> {

		private static final long serialVersionUID = 4193026742083046736L;

		int base;

		public ModKey(int base) {
			this.base = base;
		}

		@Override
		public Integer getKey(Integer value) throws Exception {
			return value % base;
		}

	}

}
