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

import com.google.common.base.Joiner;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.FloatSerializer;
import org.apache.flink.api.common.typeutils.base.IntSerializer;
import org.apache.flink.api.common.typeutils.base.IntValueSerializer;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.api.common.typeutils.base.VoidSerializer;
import org.apache.flink.runtime.operators.testutils.DummyEnvironment;
import org.apache.flink.types.IntValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Generic tests for the partitioned state part of {@link AbstractStateBackend}.
 */
public abstract class StateBackendTestBase<B extends AbstractStateBackend> {

	protected B backend;

	protected abstract B getStateBackend() throws Exception;

	protected abstract void cleanup() throws Exception;

	@Before
	public void setup() throws Exception {
		this.backend = getStateBackend();
	}

	@After
	public void teardown() throws Exception {
		this.backend.dispose();
		cleanup();
	}

	@Test
	public void testValueState() throws Exception {

			backend.initializeForJob(new DummyEnvironment("test", 1, 0), "test_op", IntSerializer.INSTANCE);

			ValueStateDescriptor<String> kvId = new ValueStateDescriptor<>("id", null, StringSerializer.INSTANCE);
			ValueState<String> state = backend.getPartitionedState(null, VoidSerializer.INSTANCE, kvId);

			@SuppressWarnings("unchecked")
			KvState<Integer, Void, ValueState<String>, ValueStateDescriptor<String>, B> kv =
					(KvState<Integer, Void, ValueState<String>, ValueStateDescriptor<String>, B>) state;

			// some modifications to the state
			kv.setCurrentKey(1);
			assertNull(state.value());
			state.update("1");
			kv.setCurrentKey(2);
			assertNull(state.value());
			state.update("2");
			kv.setCurrentKey(1);
			assertEquals("1", state.value());

			// draw a snapshot
			KvStateSnapshot<Integer, Void, ValueState<String>, ValueStateDescriptor<String>, B> snapshot1 =
					kv.snapshot(682375462378L, 2);

			// make some more modifications
			kv.setCurrentKey(1);
			state.update("u1");
			kv.setCurrentKey(2);
			state.update("u2");
			kv.setCurrentKey(3);
			state.update("u3");

			// draw another snapshot
			KvStateSnapshot<Integer, Void, ValueState<String>, ValueStateDescriptor<String>, B> snapshot2 =
					kv.snapshot(682375462379L, 4);

			// validate the original state
			kv.setCurrentKey(1);
			assertEquals("u1", state.value());
			kv.setCurrentKey(2);
			assertEquals("u2", state.value());
			kv.setCurrentKey(3);
			assertEquals("u3", state.value());

			kv.dispose();

//			 restore the first snapshot and validate it
			KvState<Integer, Void, ValueState<String>, ValueStateDescriptor<String>, B> restored1 = snapshot1.restoreState(
					backend,
					IntSerializer.INSTANCE,
					this.getClass().getClassLoader(), 10);

			@SuppressWarnings("unchecked")
			ValueState<String> restored1State = (ValueState<String>) restored1;

			restored1.setCurrentKey(1);
			assertEquals("1", restored1State.value());
			restored1.setCurrentKey(2);
			assertEquals("2", restored1State.value());

			restored1.dispose();

			// restore the second snapshot and validate it
			KvState<Integer, Void, ValueState<String>, ValueStateDescriptor<String>, B> restored2 = snapshot2.restoreState(
					backend,
					IntSerializer.INSTANCE,
					this.getClass().getClassLoader(), 10);

			@SuppressWarnings("unchecked")
			ValueState<String> restored2State = (ValueState<String>) restored2;

			restored2.setCurrentKey(1);
			assertEquals("u1", restored2State.value());
			restored2.setCurrentKey(2);
			assertEquals("u2", restored2State.value());
			restored2.setCurrentKey(3);
			assertEquals("u3", restored2State.value());
	}

	@Test
	@SuppressWarnings("unchecked,rawtypes")
	public void testListState() {
		try {
			backend.initializeForJob(new DummyEnvironment("test", 1, 0), "test_op", IntSerializer.INSTANCE);

			ListStateDescriptor<String> kvId = new ListStateDescriptor<>("id", StringSerializer.INSTANCE);
			ListState<String> state = backend.getPartitionedState(null, VoidSerializer.INSTANCE, kvId);

			@SuppressWarnings("unchecked")
			KvState<Integer, Void, ListState<String>, ListStateDescriptor<String>, B> kv =
					(KvState<Integer, Void, ListState<String>, ListStateDescriptor<String>, B>) state;

			Joiner joiner = Joiner.on(",");
			// some modifications to the state
			kv.setCurrentKey(1);
			assertEquals("", joiner.join(state.get()));
			state.add("1");
			kv.setCurrentKey(2);
			assertEquals("", joiner.join(state.get()));
			state.add("2");
			kv.setCurrentKey(1);
			assertEquals("1", joiner.join(state.get()));

			// draw a snapshot
			KvStateSnapshot<Integer, Void, ListState<String>, ListStateDescriptor<String>, B> snapshot1 =
					kv.snapshot(682375462378L, 2);

			// make some more modifications
			kv.setCurrentKey(1);
			state.add("u1");
			kv.setCurrentKey(2);
			state.add("u2");
			kv.setCurrentKey(3);
			state.add("u3");

			// draw another snapshot
			KvStateSnapshot<Integer, Void, ListState<String>, ListStateDescriptor<String>, B> snapshot2 =
					kv.snapshot(682375462379L, 4);

			// validate the original state
			kv.setCurrentKey(1);
			assertEquals("1,u1", joiner.join(state.get()));
			kv.setCurrentKey(2);
			assertEquals("2,u2", joiner.join(state.get()));
			kv.setCurrentKey(3);
			assertEquals("u3", joiner.join(state.get()));

			kv.dispose();

			// restore the first snapshot and validate it
			KvState<Integer, Void, ListState<String>, ListStateDescriptor<String>, B> restored1 = snapshot1.restoreState(
					backend,
					IntSerializer.INSTANCE,
					this.getClass().getClassLoader(), 10);

			@SuppressWarnings("unchecked")
			ListState<String> restored1State = (ListState<String>) restored1;

			restored1.setCurrentKey(1);
			assertEquals("1", joiner.join(restored1State.get()));
			restored1.setCurrentKey(2);
			assertEquals("2", joiner.join(restored1State.get()));

			restored1.dispose();

			// restore the second snapshot and validate it
			KvState<Integer, Void, ListState<String>, ListStateDescriptor<String>, B> restored2 = snapshot2.restoreState(
					backend,
					IntSerializer.INSTANCE,
					this.getClass().getClassLoader(), 20);

			@SuppressWarnings("unchecked")
			ListState<String> restored2State = (ListState<String>) restored2;

			restored2.setCurrentKey(1);
			assertEquals("1,u1", joiner.join(restored2State.get()));
			restored2.setCurrentKey(2);
			assertEquals("2,u2", joiner.join(restored2State.get()));
			restored2.setCurrentKey(3);
			assertEquals("u3", joiner.join(restored2State.get()));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	@SuppressWarnings("unchecked,rawtypes")
	public void testReducingState() {
		try {
			backend.initializeForJob(new DummyEnvironment("test", 1, 0), "test_op", IntSerializer.INSTANCE);

			ReducingStateDescriptor<String> kvId = new ReducingStateDescriptor<>("id",
				new ReduceFunction<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String reduce(String value1, String value2) throws Exception {
						return value1 + "," + value2;
					}
				},
				StringSerializer.INSTANCE);
			ReducingState<String> state = backend.getPartitionedState(null, VoidSerializer.INSTANCE, kvId);

			@SuppressWarnings("unchecked")
			KvState<Integer, Void, ReducingState<String>, ReducingStateDescriptor<String>, B> kv =
				(KvState<Integer, Void, ReducingState<String>, ReducingStateDescriptor<String>, B>) state;

			Joiner joiner = Joiner.on(",");
			// some modifications to the state
			kv.setCurrentKey(1);
			assertEquals(null, state.get());
			state.add("1");
			kv.setCurrentKey(2);
			assertEquals(null, state.get());
			state.add("2");
			kv.setCurrentKey(1);
			assertEquals("1", state.get());

			// draw a snapshot
			KvStateSnapshot<Integer, Void, ReducingState<String>, ReducingStateDescriptor<String>, B> snapshot1 =
				kv.snapshot(682375462378L, 2);

			// make some more modifications
			kv.setCurrentKey(1);
			state.add("u1");
			kv.setCurrentKey(2);
			state.add("u2");
			kv.setCurrentKey(3);
			state.add("u3");

			// draw another snapshot
			KvStateSnapshot<Integer, Void, ReducingState<String>, ReducingStateDescriptor<String>, B> snapshot2 =
				kv.snapshot(682375462379L, 4);

			// validate the original state
			kv.setCurrentKey(1);
			assertEquals("1,u1", state.get());
			kv.setCurrentKey(2);
			assertEquals("2,u2", state.get());
			kv.setCurrentKey(3);
			assertEquals("u3", state.get());

			kv.dispose();

			// restore the first snapshot and validate it
			KvState<Integer, Void, ReducingState<String>, ReducingStateDescriptor<String>, B> restored1 = snapshot1.restoreState(
				backend,
				IntSerializer.INSTANCE,
				this.getClass().getClassLoader(), 10);

			@SuppressWarnings("unchecked")
			ReducingState<String> restored1State = (ReducingState<String>) restored1;

			restored1.setCurrentKey(1);
			assertEquals("1", restored1State.get());
			restored1.setCurrentKey(2);
			assertEquals("2", restored1State.get());

			restored1.dispose();

			// restore the second snapshot and validate it
			KvState<Integer, Void, ReducingState<String>, ReducingStateDescriptor<String>, B> restored2 = snapshot2.restoreState(
				backend,
				IntSerializer.INSTANCE,
				this.getClass().getClassLoader(), 20);

			@SuppressWarnings("unchecked")
			ReducingState<String> restored2State = (ReducingState<String>) restored2;

			restored2.setCurrentKey(1);
			assertEquals("1,u1", restored2State.get());
			restored2.setCurrentKey(2);
			assertEquals("2,u2", restored2State.get());
			restored2.setCurrentKey(3);
			assertEquals("u3", restored2State.get());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	@Test
	public void testValueStateRestoreWithWrongSerializers() {
		try {
			backend.initializeForJob(new DummyEnvironment("test", 1, 0),
				"test_op",
				IntSerializer.INSTANCE);

			ValueStateDescriptor<String> kvId = new ValueStateDescriptor<>("id",
				null,
				StringSerializer.INSTANCE);
			ValueState<String> state = backend.getPartitionedState(null,
				VoidSerializer.INSTANCE,
				kvId);

			@SuppressWarnings("unchecked")
			KvState<Integer, Void, ValueState<String>, ValueStateDescriptor<String>, B> kv =
				(KvState<Integer, Void, ValueState<String>, ValueStateDescriptor<String>, B>) state;

			kv.setCurrentKey(1);
			state.update("1");
			kv.setCurrentKey(2);
			state.update("2");

			KvStateSnapshot<Integer, Void, ValueState<String>, ValueStateDescriptor<String>, B> snapshot =
				kv.snapshot(682375462378L, System.currentTimeMillis());

			@SuppressWarnings("unchecked")
			TypeSerializer<Integer> fakeIntSerializer =
				(TypeSerializer<Integer>) (TypeSerializer<?>) FloatSerializer.INSTANCE;

			try {
				snapshot.restoreState(backend, fakeIntSerializer, getClass().getClassLoader(), 1);
				fail("should recognize wrong serializers");
			} catch (IllegalArgumentException e) {
				// expected
			} catch (Exception e) {
				fail("wrong exception");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testListStateRestoreWithWrongSerializers() {
		try {
			backend.initializeForJob(new DummyEnvironment("test", 1, 0), "test_op", IntSerializer.INSTANCE);

			ListStateDescriptor<String> kvId = new ListStateDescriptor<>("id", StringSerializer.INSTANCE);
			ListState<String> state = backend.getPartitionedState(null, VoidSerializer.INSTANCE, kvId);

			@SuppressWarnings("unchecked")
			KvState<Integer, Void, ListState<String>, ListStateDescriptor<String>, B> kv =
					(KvState<Integer, Void, ListState<String>, ListStateDescriptor<String>, B>) state;

			kv.setCurrentKey(1);
			state.add("1");
			kv.setCurrentKey(2);
			state.add("2");

			KvStateSnapshot<Integer, Void, ListState<String>, ListStateDescriptor<String>, B> snapshot =
					kv.snapshot(682375462378L, System.currentTimeMillis());

			kv.dispose();

			@SuppressWarnings("unchecked")
			TypeSerializer<Integer> fakeIntSerializer =
					(TypeSerializer<Integer>) (TypeSerializer<?>) FloatSerializer.INSTANCE;

			try {
				snapshot.restoreState(backend, fakeIntSerializer, getClass().getClassLoader(), 1);
				fail("should recognize wrong serializers");
			} catch (IllegalArgumentException e) {
				// expected
			} catch (Exception e) {
				fail("wrong exception " + e);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testReducingStateRestoreWithWrongSerializers() {
		try {
			backend.initializeForJob(new DummyEnvironment("test", 1, 0), "test_op", IntSerializer.INSTANCE);

			ReducingStateDescriptor<String> kvId = new ReducingStateDescriptor<>("id",
				new ReduceFunction<String>() {
					@Override
					public String reduce(String value1, String value2) throws Exception {
						return value1 + "," + value2;
					}
				},
				StringSerializer.INSTANCE);
			ReducingState<String> state = backend.getPartitionedState(null, VoidSerializer.INSTANCE, kvId);

			@SuppressWarnings("unchecked")
			KvState<Integer, Void, ReducingState<String>, ReducingStateDescriptor<String>, B> kv =
				(KvState<Integer, Void, ReducingState<String>, ReducingStateDescriptor<String>, B>) state;

			kv.setCurrentKey(1);
			state.add("1");
			kv.setCurrentKey(2);
			state.add("2");

			KvStateSnapshot<Integer, Void, ReducingState<String>, ReducingStateDescriptor<String>, B> snapshot =
				kv.snapshot(682375462378L, System.currentTimeMillis());

			kv.dispose();

			@SuppressWarnings("unchecked")
			TypeSerializer<Integer> fakeIntSerializer =
				(TypeSerializer<Integer>) (TypeSerializer<?>) FloatSerializer.INSTANCE;

			try {
				snapshot.restoreState(backend, fakeIntSerializer, getClass().getClassLoader(), 1);
				fail("should recognize wrong serializers");
			} catch (IllegalArgumentException e) {
				// expected
			} catch (Exception e) {
				fail("wrong exception " + e);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testCopyDefaultValue() {
		try {
			backend.initializeForJob(new DummyEnvironment("test", 1, 0), "test_op", IntSerializer.INSTANCE);

			ValueStateDescriptor<IntValue> kvId = new ValueStateDescriptor<>("id", new IntValue(-1), IntValueSerializer.INSTANCE);
			ValueState<IntValue> state = backend.getPartitionedState(null, VoidSerializer.INSTANCE, kvId);

			@SuppressWarnings("unchecked")
			KvState<Integer, Void, ValueState<IntValue>, ValueStateDescriptor<IntValue>, B> kv =
					(KvState<Integer, Void, ValueState<IntValue>, ValueStateDescriptor<IntValue>, B>) state;

			kv.setCurrentKey(1);
			IntValue default1 = state.value();

			kv.setCurrentKey(2);
			IntValue default2 = state.value();

			assertNotNull(default1);
			assertNotNull(default2);
			assertEquals(default1, default2);
			assertFalse(default1 == default2);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
