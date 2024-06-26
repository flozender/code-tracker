/**
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

package org.apache.flink.runtime.profiling.impl.types;

import java.io.IOException;

import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.jobgraph.JobID;

public abstract class InternalExecutionVertexProfilingData implements InternalProfilingData {

	private final ExecutionAttemptID executionId;

	private final JobID jobId;

	
	public InternalExecutionVertexProfilingData() {
		this.jobId = new JobID();
		this.executionId = new ExecutionAttemptID();
	}

	public InternalExecutionVertexProfilingData(JobID jobID, ExecutionAttemptID executionId) {
		this.jobId = jobID;
		this.executionId = executionId;
	}

	public ExecutionAttemptID getExecutionAttemptId() {
		return this.executionId;
	}

	public JobID getJobID() {
		return this.jobId;
	}

	@Override
	public void read(DataInputView in) throws IOException {
		this.jobId.read(in);
		this.executionId.read(in);
	}

	@Override
	public void write(DataOutputView out) throws IOException {
		this.jobId.write(out);
		this.executionId.write(out);
	}
}
