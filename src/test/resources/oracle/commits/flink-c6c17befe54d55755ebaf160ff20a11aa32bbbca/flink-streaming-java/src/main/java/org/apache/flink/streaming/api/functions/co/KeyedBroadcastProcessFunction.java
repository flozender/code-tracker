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

package org.apache.flink.streaming.api.functions.co;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.util.Collector;

/**
 * A function to be applied to a
 * {@link org.apache.flink.streaming.api.datastream.BroadcastConnectedStream BroadcastConnectedStream} that
 * connects {@link org.apache.flink.streaming.api.datastream.BroadcastStream BroadcastStream}, i.e. a stream
 * with broadcast state, with a {@link org.apache.flink.streaming.api.datastream.KeyedStream KeyedStream}.
 *
 * <p>The stream with the broadcast state can be created using the
 * {@link org.apache.flink.streaming.api.datastream.KeyedStream#broadcast(MapStateDescriptor)
 * keyedStream.broadcast(MapStateDescriptor)} method.
 *
 * <p>The user has to implement two methods:
 * <ol>
 *     <li>the {@link #processBroadcastElement(Object, Context, Collector)} which will be applied to
 *     each element in the broadcast side
 *     <li> and the {@link #processElement(Object, KeyedReadOnlyContext, Collector)} which will be applied to the
 *     non-broadcasted/keyed side.
 * </ol>
 *
 * <p>The {@code processElementOnBroadcastSide()} takes as an argument (among others) a context that allows it to
 * read/write to the broadcast state and also apply a transformation to all (local) keyed states, while the
 * {@code processElement()} has read-only access to the broadcast state, but can read/write to the keyed state and
 * register timers.
 *
 * @param <IN1> The input type of the keyed (non-broadcast) side.
 * @param <IN2> The input type of the broadcast side.
 * @param <OUT> The output type of the operator.
 */
@PublicEvolving
public abstract class KeyedBroadcastProcessFunction<IN1, IN2, OUT> extends BaseBroadcastProcessFunction {

	private static final long serialVersionUID = -2584726797564976453L;

	/**
	 * This method is called for each element in the (non-broadcast)
	 * {@link org.apache.flink.streaming.api.datastream.KeyedStream keyed stream}.
	 *
	 * <p>It can output zero or more elements using the {@link Collector} parameter,
	 * query the current processing/event time, and also query and update the local keyed state.
	 * In addition, it can get a {@link TimerService} for registering timers and querying the time.
	 * Finally, it has <b>read-only</b> access to the broadcast state.
	 * The context is only valid during the invocation of this method, do not store it.
	 *
	 * @param value The stream element.
	 * @param ctx A {@link KeyedReadOnlyContext} that allows querying the timestamp of the element,
	 *            querying the current processing/event time and iterating the broadcast state
	 *            with <b>read-only</b> access.
	 *            The context is only valid during the invocation of this method, do not store it.
	 * @param out The collector to emit resulting elements to
	 * @throws Exception The function may throw exceptions which cause the streaming program
	 *                   to fail and go into recovery.
	 */
	public abstract void processElement(final IN1 value, final KeyedReadOnlyContext ctx, final Collector<OUT> out) throws Exception;

	/**
	 * This method is called for each element in the
	 * {@link org.apache.flink.streaming.api.datastream.BroadcastStream broadcast stream}.
	 *
	 * <p>It can output zero or more elements using the {@link Collector} parameter,
	 * query the current processing/event time, and also query and update the internal
	 * {@link org.apache.flink.api.common.state.BroadcastState broadcast state}. These can
	 * be done through the provided {@link Context}.
	 * The context is only valid during the invocation of this method, do not store it.
	 *
	 * @param value The stream element.
	 * @param ctx A {@link Context} that allows querying the timestamp of the element,
	 *            querying the current processing/event time and updating the broadcast state.
	 *            The context is only valid during the invocation of this method, do not store it.
	 * @param out The collector to emit resulting elements to
	 * @throws Exception The function may throw exceptions which cause the streaming program
	 *                   to fail and go into recovery.
	 */
	public abstract void processBroadcastElement(final IN2 value, final Context ctx, final Collector<OUT> out) throws Exception;

	/**
	 * Called when a timer set using {@link TimerService} fires.
	 *
	 * @param timestamp The timestamp of the firing timer.
	 * @param ctx An {@link OnTimerContext} that allows querying the timestamp of the firing timer,
	 *            querying the current processing/event time, iterating the broadcast state
	 *            with <b>read-only</b> access, querying the {@link TimeDomain} of the firing timer
	 *            and getting a {@link TimerService} for registering timers and querying the time.
	 *            The context is only valid during the invocation of this method, do not store it.
	 * @param out The collector for returning result values.
	 *
	 * @throws Exception This method may throw exceptions. Throwing an exception will cause the operation
	 *                   to fail and may trigger recovery.
	 */
	public void onTimer(final long timestamp, final OnTimerContext ctx, final Collector<OUT> out) throws Exception {
		// the default implementation does nothing.
	}

	/**
	 * A {@link BaseBroadcastProcessFunction.Context context} available to the keyed stream side of
	 * a {@link org.apache.flink.streaming.api.datastream.BroadcastConnectedStream} (if any).
	 *
	 * <p>Apart from the basic functionality of a {@link BaseBroadcastProcessFunction.Context context},
	 * this also allows to get a <b>read-only</b> {@link Iterable} over the elements stored in the
	 * broadcast state and a {@link TimerService} for querying time and registering timers.
	 */
	public abstract class KeyedReadOnlyContext extends ReadOnlyContext {

		/**
		 * A {@link TimerService} for querying time and registering timers.
		 */
		public abstract TimerService timerService();
	}

	/**
	 * Information available in an invocation of {@link #onTimer(long, OnTimerContext, Collector)}.
	 */
	public abstract class OnTimerContext extends KeyedReadOnlyContext {

		/**
		 * The {@link TimeDomain} of the firing timer, i.e. if it is
		 * event or processing time timer.
		 */
		public abstract TimeDomain timeDomain();
	}
}
