/***********************************************************************************************************************
 * Copyright (C) 2010-2013 by the Apache Flink project (http://flink.incubator.apache.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 **********************************************************************************************************************/

package org.apache.flink.runtime.iterative.event;

import java.io.IOException;
import java.util.Map;

import org.apache.flink.api.common.aggregators.Aggregator;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.types.Value;

public class WorkerDoneEvent extends IterationEventWithAggregators {
	
	private int workerIndex;
	
	public WorkerDoneEvent() {
		super();
	}

	public WorkerDoneEvent(int workerIndex, String aggregatorName, Value aggregate) {
		super(aggregatorName, aggregate);
		this.workerIndex = workerIndex;
	}
	
	public WorkerDoneEvent(int workerIndex, Map<String, Aggregator<?>> aggregators) {
		super(aggregators);
		this.workerIndex = workerIndex;
	}
	
	public int getWorkerIndex() {
		return workerIndex;
	}
	
	@Override
	public void write(DataOutputView out) throws IOException {
		out.writeInt(this.workerIndex);
		super.write(out);
	}
	
	@Override
	public void read(DataInputView in) throws IOException {
		this.workerIndex = in.readInt();
		super.read(in);
	}
}
