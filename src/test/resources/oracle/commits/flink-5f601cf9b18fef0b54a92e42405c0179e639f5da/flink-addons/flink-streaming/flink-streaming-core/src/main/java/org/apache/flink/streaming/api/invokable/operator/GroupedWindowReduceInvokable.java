/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.invokable.operator;

import java.io.IOException;

import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.streaming.api.invokable.util.TimeStamp;
import org.apache.flink.streaming.api.streamrecord.StreamRecord;

public class GroupedWindowReduceInvokable<OUT> extends GroupedBatchReduceInvokable<OUT> {

	private static final long serialVersionUID = 1L;

	private TimeStamp<OUT> timestamp;
	private long startTime;
	private long nextRecordTime;

	public GroupedWindowReduceInvokable(ReduceFunction<OUT> reduceFunction, long windowSize,
			long slideInterval, int keyPosition, TimeStamp<OUT> timestamp) {
		super(reduceFunction, windowSize, slideInterval, keyPosition);
		this.timestamp = timestamp;
		this.startTime = timestamp.getStartTime();
	}

	@Override
	protected StreamRecord<OUT> getNextRecord() throws IOException {
		reuse = recordIterator.next(reuse);
		if (reuse != null) {
			nextRecordTime = timestamp.getTimestamp(reuse.getObject());
		}
		return reuse;
	}

	@Override
	protected boolean batchNotFull() {
		if (nextRecordTime < startTime + granularity) {
			return true;
		} else {
			startTime += granularity;
			return false;
		}
	}

}