/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.functions;

import org.apache.flink.api.common.functions.Function;

/**
 * A {@code TimestampAssigner} assigns event time timestamps to elements.
 * These timestamps are used by all functions that operate on event time,
 * for example event time windows.
 * 
 * <p>Timestamps are represented in milliseconds since the Epoch
 * (midnight, January 1, 1970 UTC).
 * 
 * <p>A timestamp assigner that assigns to each element a timestamp via
 * {@link System#currentTimeMillis()} effectively realizes "ingestion time"
 * semantics.
 * 
 * @param <T> The type of the elements to which this assigner assigns timestamps.
 */
public interface TimestampAssigner<T> extends Function {

	/**
	 * Assigns a timestamp to an element, in milliseconds since the Epoch.
	 * 
	 * <p>The method gets the previously assigned timestamp of the element.
	 * That previous timestamp may have been assigned from a previous assigner,
	 * by ingestion time, or be simply uninitialized. In the latter case, the
	 * timestamp will be a negative value.
	 *
	 * @param element The element that the timestamp is wil be assigned to.
	 * @param previousElementTimestamp The previous internal timestamp of the element,
	 *                                 or a negative value, if no timestamp has been assigned, yet.   
	 * @return The new timestamp.
	 */
	long extractTimestamp(T element, long previousElementTimestamp);
}
