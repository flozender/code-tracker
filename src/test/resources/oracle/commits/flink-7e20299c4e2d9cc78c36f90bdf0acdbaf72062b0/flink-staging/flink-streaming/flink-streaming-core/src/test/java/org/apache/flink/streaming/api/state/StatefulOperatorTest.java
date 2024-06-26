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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.accumulators.Accumulator;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.OperatorState;
import org.apache.flink.api.common.state.StateCheckpointer;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.operators.testutils.MockEnvironment;
import org.apache.flink.runtime.operators.testutils.MockInputSplitProvider;
import org.apache.flink.runtime.state.LocalStateHandle.LocalStateHandleProvider;
import org.apache.flink.runtime.state.StateHandle;
import org.apache.flink.streaming.api.checkpoint.Checkpointed;
import org.apache.flink.streaming.api.datastream.KeyedDataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.api.operators.StreamMap;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.StreamingRuntimeContext;
import org.apache.flink.streaming.util.StreamingMultipleProgramsTestBase;
import org.apache.flink.util.InstantiationUtil;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test the functionality supported by stateful user functions for both
 * partitioned and non-partitioned user states. This test mimics the runtime
 * behavior of stateful stream operators.
 */
public class StatefulOperatorTest extends StreamingMultipleProgramsTestBase {

	@Test
	public void simpleStateTest() throws Exception {

		List<String> out = new ArrayList<String>();

		StreamMap<Integer, String> map = createOperatorWithContext(out, new ModKey(2), null);
		StreamingRuntimeContext context = map.getRuntimeContext();

		processInputs(map, Arrays.asList(1, 2, 3, 4, 5));

		assertEquals(Arrays.asList("1", "2", "3", "4", "5"), out);
		assertEquals((Integer) 5, context.getOperatorState("counter", 0, false).value());
		assertEquals(ImmutableMap.of(0, new MutableInt(2), 1, new MutableInt(3)), context.getOperatorStates().get("groupCounter").getPartitionedState());
		assertEquals("12345", context.getOperatorState("concat", "", false).value());
		assertEquals((Integer) 5, ((StatefulMapper) map.getUserFunction()).checkpointedCounter);

		byte[] serializedState0 = InstantiationUtil.serializeObject(map.getStateSnapshotFromFunction(1, 1));
		// Restore state but snapshot again before calling the value
		byte[] serializedState = InstantiationUtil.serializeObject(createOperatorWithContext(out,
				new ModKey(2), serializedState0).getStateSnapshotFromFunction(1, 1));

		StreamMap<Integer, String> restoredMap = createOperatorWithContext(out, new ModKey(2), serializedState);
		StreamingRuntimeContext restoredContext = restoredMap.getRuntimeContext();
		
		assertEquals((Integer) 5, restoredContext.getOperatorState("counter", 0, false).value());
		assertEquals(ImmutableMap.of(0, new MutableInt(2), 1, new MutableInt(3)), context.getOperatorStates().get("groupCounter").getPartitionedState());
		assertEquals("12345", restoredContext.getOperatorState("concat", "", false).value());
		assertEquals((Integer) 5, ((StatefulMapper) restoredMap.getUserFunction()).checkpointedCounter);
		out.clear();

		processInputs(restoredMap, Arrays.asList(7, 8));

		assertEquals(Arrays.asList("7", "8"), out);
		assertEquals((Integer) 7, restoredContext.getOperatorState("counter", 0, false).value());
		assertEquals(ImmutableMap.of(0, new MutableInt(3), 1, new MutableInt(4)), restoredContext.getOperatorStates().get("groupCounter")
				.getPartitionedState());
		assertEquals("1234578", restoredContext.getOperatorState("concat", "", false).value());
		assertEquals((Integer) 7, ((StatefulMapper) restoredMap.getUserFunction()).checkpointedCounter);

	}

	@Test
	public void apiTest() throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setParallelism(3);

		KeyedDataStream<Integer, Integer> keyedStream = env
				.fromCollection(Arrays.asList(0, 1, 2, 3, 4, 5, 6))
				.keyBy(new ModKey(4));

		keyedStream.map(new StatefulMapper()).addSink(new SinkFunction<String>() {
			private static final long serialVersionUID = 1L;

			public void invoke(String value) throws Exception {
			}
		});

		keyedStream.map(new StatefulMapper2()).setParallelism(1).addSink(new SinkFunction<String>() {
			private static final long serialVersionUID = 1L;

			public void invoke(String value) throws Exception {
			}
		});

		try {
			keyedStream.shuffle();
			fail();
		} catch (UnsupportedOperationException e) {

		}
		
		env.fromElements(0, 1, 2, 2, 2, 3, 4, 3, 4).keyBy(new KeySelector<Integer, Integer>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Integer getKey(Integer value) throws Exception {
				return value;
			}

		}).map(new PStateKeyRemovalTestMapper()).setParallelism(1).addSink(new SinkFunction<String>() {
			private static final long serialVersionUID = 1L;

			public void invoke(String value) throws Exception {
			}
		});

		env.execute();
	}

	private void processInputs(StreamMap<Integer, ?> map, List<Integer> input) throws Exception {
		for (Integer i : input) {
			map.getRuntimeContext().setNextInput(new StreamRecord<Integer>(i, 0L));
			map.processElement(new StreamRecord<Integer>(i, 0L));
		}
	}

	@SuppressWarnings("unchecked")
	private StreamMap<Integer, String> createOperatorWithContext(List<String> output,
			KeySelector<Integer, ? extends Serializable> partitioner, byte[] serializedState) throws Exception {
		final List<String> outputList = output;

		StreamingRuntimeContext context = new StreamingRuntimeContext(
				new MockEnvironment("MockTask", 3 * 1024 * 1024, new MockInputSplitProvider(), 1024),
				new ExecutionConfig(),
				partitioner,
				new LocalStateHandleProvider<Serializable>(),
				new HashMap<String, Accumulator<?, ?>>(),
				null);

		StreamMap<Integer, String> op = new StreamMap<Integer, String>(new StatefulMapper());

		op.setup(new Output<StreamRecord<String>>() {

			@Override
			public void collect(StreamRecord<String> record) {
				outputList.add(record.getValue());
			}

			@Override
			public void emitWatermark(Watermark mark) {

			}

			@Override
			public void close() {
			}
		}, context);

		if (serializedState != null) {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			op.restoreInitialState((Tuple2<StateHandle<Serializable>, Map<String, OperatorStateHandle>>) InstantiationUtil
					.deserializeObject(serializedState, cl));
		}

		op.open(null);

		return op;
	}

	public static class StatefulMapper extends RichMapFunction<Integer, String> implements
			Checkpointed<Integer> {
		private static final long serialVersionUID = -9007873655253339356L;
		OperatorState<Integer> counter;
		OperatorState<MutableInt> groupCounter;
		OperatorState<String> concat;

		Integer checkpointedCounter = 0;

		@Override
		public String map(Integer value) throws Exception {
			counter.update(counter.value() + 1);
			MutableInt incremented = groupCounter.value();
			incremented.increment();
			groupCounter.update(incremented);
			concat.update(concat.value() + value.toString());
			checkpointedCounter++;
			try {
				counter.update(null);
				fail();
			} catch (RuntimeException e) {
			}
			return value.toString();
		}

		@Override
		public void open(Configuration conf) throws IOException {
			counter = getRuntimeContext().getOperatorState("counter", 0, false, intCheckpointer);
			groupCounter = getRuntimeContext().getOperatorState("groupCounter", new MutableInt(0), true);
			concat = getRuntimeContext().getOperatorState("concat", "", false);
			try {
				getRuntimeContext().getOperatorState("test", null, true);
				fail();
			} catch (RuntimeException e) {
			}
			try {
				getRuntimeContext().getOperatorState("test", null, true, null);
				fail();
			} catch (RuntimeException e) {
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void close() throws Exception {
			Map<String, StreamOperatorState<?, ?>> states = ((StreamingRuntimeContext) getRuntimeContext()).getOperatorStates();
			PartitionedStreamOperatorState<Integer, Integer, Integer> groupCounter = (PartitionedStreamOperatorState<Integer, Integer, Integer>) states.get("groupCounter");
			for (Entry<Serializable, Integer> count : groupCounter.getPartitionedState().entrySet()) {
				Integer key = (Integer) count.getKey();
				Integer expected = key < 3 ? 2 : 1;

				assertEquals(new MutableInt(expected), count.getValue());
			}
		}

		@Override
		public Integer snapshotState(long checkpointId, long checkpointTimestamp) throws Exception {
			return checkpointedCounter;
		}

		@Override
		public void restoreState(Integer state) {
			this.checkpointedCounter = (Integer) state;
		}
	}

	public static class StatefulMapper2 extends RichMapFunction<Integer, String> {
		private static final long serialVersionUID = 1L;
		OperatorState<Integer> groupCounter;

		@Override
		public String map(Integer value) throws Exception {
			groupCounter.update(groupCounter.value() + 1);

			return value.toString();
		}

		@Override
		public void open(Configuration conf) throws IOException {
			groupCounter = getRuntimeContext().getOperatorState("groupCounter", 0, true, intCheckpointer);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void close() throws Exception {
			Map<String, StreamOperatorState<?, ?>> states = ((StreamingRuntimeContext) getRuntimeContext()).getOperatorStates();
			PartitionedStreamOperatorState<Integer, Integer, Integer> groupCounter = 
					(PartitionedStreamOperatorState<Integer, Integer, Integer>) states.get("groupCounter");
			for (Entry<Serializable, Integer> count : groupCounter.getPartitionedState().entrySet()) {
				Integer key = (Integer) count.getKey();
				Integer expected = key < 3 ? 2 : 1;
				assertEquals(expected, count.getValue());
			}
		}

	}
	
	public static StateCheckpointer<Integer, String> intCheckpointer = new StateCheckpointer<Integer, String>() {

		private static final long serialVersionUID = 1L;

		@Override
		public String snapshotState(Integer state, long checkpointId, long checkpointTimestamp) {
			return state.toString();
		}

		@Override
		public Integer restoreState(String stateSnapshot) {
			return Integer.parseInt(stateSnapshot);
		}
	};

	public static class PStateKeyRemovalTestMapper extends RichMapFunction<Integer, String> {

		private static final long serialVersionUID = 1L;
		OperatorState<Boolean> seen;

		@Override
		public String map(Integer value) throws Exception {
			if (value == 0) {
				seen.update(null);
			}else{
				Boolean s = seen.value();
				if (s) {
					seen.update(null);
				} else {
					seen.update(true);
				}
			}

			return value.toString();
		}

		public void open(Configuration c) throws IOException {
			seen = getRuntimeContext().getOperatorState("seen", false, true);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void close() throws Exception {
			Map<String, StreamOperatorState<?, ?>> states = ((StreamingRuntimeContext) getRuntimeContext()).getOperatorStates();
			PartitionedStreamOperatorState<Integer, Boolean, Boolean> seen = (PartitionedStreamOperatorState<Integer, Boolean, Boolean>) states.get("seen");
			assertFalse(seen.getPartitionedState().containsKey(0));
			assertEquals(2,seen.getPartitionedState().size());
			for (Entry<Serializable, Boolean> s : seen.getPartitionedState().entrySet()) {
					assertTrue(s.getValue());
			}
		}

	}

	public static class ModKey implements KeySelector<Integer, Integer> {

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
