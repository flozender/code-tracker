/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.streaming.api.operators.co;


import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.functions.co.CoProcessFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.OperatorStateHandles;
import org.apache.flink.streaming.util.KeyedTwoInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.TestHarnessUtil;
import org.apache.flink.streaming.util.TwoInputStreamOperatorTestHarness;
import org.apache.flink.util.Collector;
import org.apache.flink.util.TestLogger;
import org.junit.Test;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link CoProcessOperator}.
 */
public class CoProcessOperatorTest extends TestLogger {

	@Test
	public void testTimestampAndWatermarkQuerying() throws Exception {

		CoProcessOperator<String, Integer, String, String> operator =
				new CoProcessOperator<>(new WatermarkQueryingProcessFunction());

		TwoInputStreamOperatorTestHarness<Integer, String, String> testHarness =
				new KeyedTwoInputStreamOperatorTestHarness<>(
						operator,
						new IntToStringKeySelector<>(),
						new IdentityKeySelector<String>(),
						BasicTypeInfo.STRING_TYPE_INFO);

		testHarness.setup();
		testHarness.open();

		testHarness.processWatermark1(new Watermark(17));
		testHarness.processWatermark2(new Watermark(17));
		testHarness.processElement1(new StreamRecord<>(5, 12L));

		testHarness.processWatermark1(new Watermark(42));
		testHarness.processWatermark2(new Watermark(42));
		testHarness.processElement2(new StreamRecord<>("6", 13L));

		ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();

		expectedOutput.add(new Watermark(17L));
		expectedOutput.add(new StreamRecord<>("5WM:17 TS:12", 12L));
		expectedOutput.add(new Watermark(42L));
		expectedOutput.add(new StreamRecord<>("6WM:42 TS:13", 13L));

		TestHarnessUtil.assertOutputEquals("Output was not correct.", expectedOutput, testHarness.getOutput());

		testHarness.close();
	}

	@Test
	public void testTimestampAndProcessingTimeQuerying() throws Exception {

		CoProcessOperator<String, Integer, String, String> operator =
				new CoProcessOperator<>(new ProcessingTimeQueryingProcessFunction());

		TwoInputStreamOperatorTestHarness<Integer, String, String> testHarness =
				new KeyedTwoInputStreamOperatorTestHarness<>(
						operator,
						new IntToStringKeySelector<>(),
						new IdentityKeySelector<String>(),
						BasicTypeInfo.STRING_TYPE_INFO);

		testHarness.setup();
		testHarness.open();

		testHarness.setProcessingTime(17);
		testHarness.processElement1(new StreamRecord<>(5));

		testHarness.setProcessingTime(42);
		testHarness.processElement2(new StreamRecord<>("6"));

		ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();

		expectedOutput.add(new StreamRecord<>("5PT:17 TS:null"));
		expectedOutput.add(new StreamRecord<>("6PT:42 TS:null"));

		TestHarnessUtil.assertOutputEquals("Output was not correct.", expectedOutput, testHarness.getOutput());

		testHarness.close();
	}

	@Test
	public void testEventTimeTimers() throws Exception {

		CoProcessOperator<String, Integer, String, String> operator =
				new CoProcessOperator<>(new EventTimeTriggeringProcessFunction());

		TwoInputStreamOperatorTestHarness<Integer, String, String> testHarness =
				new KeyedTwoInputStreamOperatorTestHarness<>(
						operator,
						new IntToStringKeySelector<>(),
						new IdentityKeySelector<String>(),
						BasicTypeInfo.STRING_TYPE_INFO);

		testHarness.setup();
		testHarness.open();

		testHarness.processElement1(new StreamRecord<>(17, 42L));
		testHarness.processElement2(new StreamRecord<>("18", 42L));

		testHarness.processWatermark1(new Watermark(5));
		testHarness.processWatermark2(new Watermark(5));

		testHarness.processWatermark1(new Watermark(6));
		testHarness.processWatermark2(new Watermark(6));

		ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();

		expectedOutput.add(new StreamRecord<>("INPUT1:17", 42L));
		expectedOutput.add(new StreamRecord<>("INPUT2:18", 42L));
		expectedOutput.add(new StreamRecord<>("1777", 5L));
		expectedOutput.add(new Watermark(5L));
		expectedOutput.add(new StreamRecord<>("1777", 6L));
		expectedOutput.add(new Watermark(6L));

		TestHarnessUtil.assertOutputEquals("Output was not correct.", expectedOutput, testHarness.getOutput());

		testHarness.close();
	}

	@Test
	public void testProcessingTimeTimers() throws Exception {

		CoProcessOperator<String, Integer, String, String> operator =
				new CoProcessOperator<>(new ProcessingTimeTriggeringProcessFunction());

		TwoInputStreamOperatorTestHarness<Integer, String, String> testHarness =
				new KeyedTwoInputStreamOperatorTestHarness<>(
						operator,
						new IntToStringKeySelector<>(),
						new IdentityKeySelector<String>(),
						BasicTypeInfo.STRING_TYPE_INFO);

		testHarness.setup();
		testHarness.open();

		testHarness.processElement1(new StreamRecord<>(17));
		testHarness.processElement2(new StreamRecord<>("18"));

		testHarness.setProcessingTime(5);
		testHarness.setProcessingTime(6);

		ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();

		expectedOutput.add(new StreamRecord<>("INPUT1:17"));
		expectedOutput.add(new StreamRecord<>("INPUT2:18"));
		expectedOutput.add(new StreamRecord<>("1777", 5L));
		expectedOutput.add(new StreamRecord<>("1777", 6L));

		TestHarnessUtil.assertOutputEquals("Output was not correct.", expectedOutput, testHarness.getOutput());

		testHarness.close();
	}

	/**
	 * Verifies that we don't have leakage between different keys.
	 */
	@Test
	public void testEventTimeTimerWithState() throws Exception {

		CoProcessOperator<String, Integer, String, String> operator =
				new CoProcessOperator<>(new EventTimeTriggeringStatefulProcessFunction());

		TwoInputStreamOperatorTestHarness<Integer, String, String> testHarness =
				new KeyedTwoInputStreamOperatorTestHarness<>(
						operator,
						new IntToStringKeySelector<>(),
						new IdentityKeySelector<String>(),
						BasicTypeInfo.STRING_TYPE_INFO);

		testHarness.setup();
		testHarness.open();

		testHarness.processWatermark1(new Watermark(1));
		testHarness.processWatermark2(new Watermark(1));
		testHarness.processElement1(new StreamRecord<>(17, 0L)); // should set timer for 6

		testHarness.processWatermark1(new Watermark(2));
		testHarness.processWatermark2(new Watermark(2));
		testHarness.processElement2(new StreamRecord<>("42", 1L)); // should set timer for 7

		testHarness.processWatermark1(new Watermark(6));
		testHarness.processWatermark2(new Watermark(6));

		testHarness.processWatermark1(new Watermark(7));
		testHarness.processWatermark2(new Watermark(7));

		ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();

		expectedOutput.add(new Watermark(1L));
		expectedOutput.add(new StreamRecord<>("INPUT1:17", 0L));
		expectedOutput.add(new Watermark(2L));
		expectedOutput.add(new StreamRecord<>("INPUT2:42", 1L));
		expectedOutput.add(new StreamRecord<>("STATE:17", 6L));
		expectedOutput.add(new Watermark(6L));
		expectedOutput.add(new StreamRecord<>("STATE:42", 7L));
		expectedOutput.add(new Watermark(7L));

		TestHarnessUtil.assertOutputEquals("Output was not correct.", expectedOutput, testHarness.getOutput());

		testHarness.close();
	}

	/**
	 * Verifies that we don't have leakage between different keys.
	 */
	@Test
	public void testProcessingTimeTimerWithState() throws Exception {

		CoProcessOperator<String, Integer, String, String> operator =
				new CoProcessOperator<>(new ProcessingTimeTriggeringStatefulProcessFunction());

		TwoInputStreamOperatorTestHarness<Integer, String, String> testHarness =
				new KeyedTwoInputStreamOperatorTestHarness<>(
						operator,
						new IntToStringKeySelector<>(),
						new IdentityKeySelector<String>(),
						BasicTypeInfo.STRING_TYPE_INFO);

		testHarness.setup();
		testHarness.open();

		testHarness.setProcessingTime(1);
		testHarness.processElement1(new StreamRecord<>(17)); // should set timer for 6

		testHarness.setProcessingTime(2);
		testHarness.processElement2(new StreamRecord<>("42")); // should set timer for 7

		testHarness.setProcessingTime(6);
		testHarness.setProcessingTime(7);

		ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();

		expectedOutput.add(new StreamRecord<>("INPUT1:17"));
		expectedOutput.add(new StreamRecord<>("INPUT2:42"));
		expectedOutput.add(new StreamRecord<>("STATE:17", 6L));
		expectedOutput.add(new StreamRecord<>("STATE:42", 7L));

		TestHarnessUtil.assertOutputEquals("Output was not correct.", expectedOutput, testHarness.getOutput());

		testHarness.close();
	}

	@Test
	public void testSnapshotAndRestore() throws Exception {

		CoProcessOperator<String, Integer, String, String> operator =
				new CoProcessOperator<>(new BothTriggeringProcessFunction());

		TwoInputStreamOperatorTestHarness<Integer, String, String> testHarness =
				new KeyedTwoInputStreamOperatorTestHarness<>(
						operator,
						new IntToStringKeySelector<>(),
						new IdentityKeySelector<String>(),
						BasicTypeInfo.STRING_TYPE_INFO);

		testHarness.setup();
		testHarness.open();

		testHarness.processElement1(new StreamRecord<>(5, 12L));
		testHarness.processElement2(new StreamRecord<>("5", 12L));

		// snapshot and restore from scratch
		OperatorStateHandles snapshot = testHarness.snapshot(0, 0);

		testHarness.close();

		operator = new CoProcessOperator<>(new BothTriggeringProcessFunction());

		testHarness = new KeyedTwoInputStreamOperatorTestHarness<>(
				operator,
				new IntToStringKeySelector<>(),
				new IdentityKeySelector<String>(),
				BasicTypeInfo.STRING_TYPE_INFO);

		testHarness.setup();
		testHarness.initializeState(snapshot);
		testHarness.open();

		testHarness.setProcessingTime(5);
		testHarness.processWatermark1(new Watermark(6));
		testHarness.processWatermark2(new Watermark(6));

		ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();

		expectedOutput.add(new StreamRecord<>("PROC:1777", 5L));
		expectedOutput.add(new StreamRecord<>("EVENT:1777", 6L));
		expectedOutput.add(new Watermark(6));

		TestHarnessUtil.assertOutputEquals("Output was not correct.", expectedOutput, testHarness.getOutput());

		testHarness.close();
	}


	private static class IntToStringKeySelector<T> implements KeySelector<Integer, String> {
		private static final long serialVersionUID = 1L;

		@Override
		public String getKey(Integer value) throws Exception {
			return "" + value;
		}
	}

	private static class IdentityKeySelector<T> implements KeySelector<T, T> {
		private static final long serialVersionUID = 1L;

		@Override
		public T getKey(T value) throws Exception {
			return value;
		}
	}

	private static class WatermarkQueryingProcessFunction extends CoProcessFunction<Integer, String, String> {

		private static final long serialVersionUID = 1L;

		@Override
		public void processElement1(Integer value, Context ctx, Collector<String> out) throws Exception {
			out.collect(value + "WM:" + ctx.timerService().currentWatermark() + " TS:" + ctx.timestamp());
		}

		@Override
		public void processElement2(String value, Context ctx, Collector<String> out) throws Exception {
			out.collect(value + "WM:" + ctx.timerService().currentWatermark() + " TS:" + ctx.timestamp());
		}

		@Override
		public void onTimer(
				long timestamp,
				OnTimerContext ctx,
				Collector<String> out) throws Exception {
		}
	}

	private static class EventTimeTriggeringProcessFunction extends CoProcessFunction<Integer, String, String> {

		private static final long serialVersionUID = 1L;

		@Override
		public void processElement1(Integer value, Context ctx, Collector<String> out) throws Exception {
			out.collect("INPUT1:" + value);
			ctx.timerService().registerEventTimeTimer(5);
		}

		@Override
		public void processElement2(String value, Context ctx, Collector<String> out) throws Exception {
			out.collect("INPUT2:" + value);
			ctx.timerService().registerEventTimeTimer(6);
		}

		@Override
		public void onTimer(
				long timestamp,
				OnTimerContext ctx,
				Collector<String> out) throws Exception {

			assertEquals(TimeDomain.EVENT_TIME, ctx.timeDomain());
			out.collect("" + 1777);
		}
	}

	private static class EventTimeTriggeringStatefulProcessFunction extends CoProcessFunction<Integer, String, String> {

		private static final long serialVersionUID = 1L;

		private final ValueStateDescriptor<String> state =
				new ValueStateDescriptor<>("seen-element", StringSerializer.INSTANCE);

		@Override
		public void processElement1(Integer value, Context ctx, Collector<String> out) throws Exception {
			out.collect("INPUT1:" + value);
			getRuntimeContext().getState(state).update("" + value);
			ctx.timerService().registerEventTimeTimer(ctx.timerService().currentWatermark() + 5);
		}

		@Override
		public void processElement2(String value, Context ctx, Collector<String> out) throws Exception {
			out.collect("INPUT2:" + value);
			getRuntimeContext().getState(state).update(value);
			ctx.timerService().registerEventTimeTimer(ctx.timerService().currentWatermark() + 5);
		}


		@Override
		public void onTimer(
				long timestamp,
				OnTimerContext ctx,
				Collector<String> out) throws Exception {
			assertEquals(TimeDomain.EVENT_TIME, ctx.timeDomain());
			out.collect("STATE:" + getRuntimeContext().getState(state).value());
		}
	}

	private static class ProcessingTimeTriggeringProcessFunction extends CoProcessFunction<Integer, String, String> {

		private static final long serialVersionUID = 1L;

		@Override
		public void processElement1(Integer value, Context ctx, Collector<String> out) throws Exception {
			out.collect("INPUT1:" + value);
			ctx.timerService().registerProcessingTimeTimer(5);
		}

		@Override
		public void processElement2(String value, Context ctx, Collector<String> out) throws Exception {
			out.collect("INPUT2:" + value);
			ctx.timerService().registerProcessingTimeTimer(6);
		}

		@Override
		public void onTimer(
				long timestamp,
				OnTimerContext ctx,
				Collector<String> out) throws Exception {

			assertEquals(TimeDomain.PROCESSING_TIME, ctx.timeDomain());
			out.collect("" + 1777);
		}
	}

	private static class ProcessingTimeQueryingProcessFunction extends CoProcessFunction<Integer, String, String> {

		private static final long serialVersionUID = 1L;

		@Override
		public void processElement1(Integer value, Context ctx, Collector<String> out) throws Exception {
			out.collect(value + "PT:" + ctx.timerService().currentProcessingTime() + " TS:" + ctx.timestamp());
		}

		@Override
		public void processElement2(String value, Context ctx, Collector<String> out) throws Exception {
			out.collect(value + "PT:" + ctx.timerService().currentProcessingTime() + " TS:" + ctx.timestamp());
		}

		@Override
		public void onTimer(
				long timestamp,
				OnTimerContext ctx,
				Collector<String> out) throws Exception {
		}
	}

	private static class ProcessingTimeTriggeringStatefulProcessFunction extends CoProcessFunction<Integer, String, String> {

		private static final long serialVersionUID = 1L;

		private final ValueStateDescriptor<String> state =
				new ValueStateDescriptor<>("seen-element", StringSerializer.INSTANCE);

		@Override
		public void processElement1(Integer value, Context ctx, Collector<String> out) throws Exception {
			out.collect("INPUT1:" + value);
			getRuntimeContext().getState(state).update("" + value);
			ctx.timerService().registerProcessingTimeTimer(ctx.timerService().currentProcessingTime() + 5);
		}

		@Override
		public void processElement2(String value, Context ctx, Collector<String> out) throws Exception {
			out.collect("INPUT2:" + value);
			getRuntimeContext().getState(state).update(value);
			ctx.timerService().registerProcessingTimeTimer(ctx.timerService().currentProcessingTime() + 5);
		}


		@Override
		public void onTimer(
				long timestamp,
				OnTimerContext ctx,
				Collector<String> out) throws Exception {
			assertEquals(TimeDomain.PROCESSING_TIME, ctx.timeDomain());
			out.collect("STATE:" + getRuntimeContext().getState(state).value());
		}
	}

	private static class BothTriggeringProcessFunction extends CoProcessFunction<Integer, String, String> {

		private static final long serialVersionUID = 1L;

		@Override
		public void processElement1(Integer value, Context ctx, Collector<String> out) throws Exception {
			ctx.timerService().registerEventTimeTimer(6);
		}

		@Override
		public void processElement2(String value, Context ctx, Collector<String> out) throws Exception {
			ctx.timerService().registerProcessingTimeTimer(5);
		}


		@Override
		public void onTimer(
				long timestamp,
				OnTimerContext ctx,
				Collector<String> out) throws Exception {
			if (TimeDomain.EVENT_TIME.equals(ctx.timeDomain())) {
				out.collect("EVENT:1777");
			} else {
				out.collect("PROC:1777");
			}
		}
	}
}
