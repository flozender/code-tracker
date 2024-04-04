/**
 *
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
 *
 */

package org.apache.flink.streaming.api.invokable.operator;

import java.util.ArrayList;
import java.util.List;

import org.apache.flink.api.java.functions.GroupReduceFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.streaming.api.invokable.UserTaskInvokable;
import org.apache.flink.streaming.api.streamrecord.StreamRecord;
import org.apache.flink.util.Collector;

public class BatchReduceInvokable<IN extends Tuple, OUT extends Tuple> extends
		UserTaskInvokable<IN, OUT> {
	private static final long serialVersionUID = 1L;
	private List<IN> tupleBatch;
	private int counter;
	private int batchSize;

	private GroupReduceFunction<IN, OUT> reducer;

	public BatchReduceInvokable(GroupReduceFunction<IN, OUT> reduceFunction, int batchSize) {
		this.reducer = reduceFunction;
		this.tupleBatch = new ArrayList<IN>();
		this.counter = 0;
		this.batchSize = batchSize;
	}

	@Override
	public void invoke(StreamRecord<IN> record, Collector<OUT> collector) throws Exception {

		tupleBatch.add(record.getTuple());
		counter++;
		if (counter >= batchSize) {
			counter = 0;
			reducer.reduce(tupleBatch.iterator(), collector);
			tupleBatch.clear();
		}

	}
}