/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.nephele.profiling.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import eu.stratosphere.nephele.jobgraph.JobID;
import eu.stratosphere.nephele.managementgraph.ManagementVertexID;

/**
 * Through this interface it is possible to access profiling data about
 * the utilization of input gates.
 * 
 * @author stanik
 */
public class InputGateProfilingEvent extends VertexProfilingEvent {

	private int gateIndex;

	private int noRecordsAvailableCounter;

	public InputGateProfilingEvent(int gateIndex, int noRecordsAvailableCounter, ManagementVertexID vertexID,
			int profilingInterval, JobID jobID, long timestamp, long profilingTimestamp) {
		super(vertexID, profilingInterval, jobID, timestamp, profilingTimestamp);

		this.gateIndex = gateIndex;
		this.noRecordsAvailableCounter = noRecordsAvailableCounter;
	}

	public InputGateProfilingEvent() {
		super();
	}

	/**
	 * Returns the index of input gate.
	 * 
	 * @return the index of the input gate
	 */
	public int getGateIndex() {
		return this.gateIndex;
	}

	/**
	 * Returns the number of times no records were available
	 * on any of the channels attached to the input gate in
	 * the given profiling internval.
	 * 
	 * @return the number of times no records were available
	 */
	public int getNoRecordsAvailableCounter() {
		return this.noRecordsAvailableCounter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void read(DataInput in) throws IOException {
		super.read(in);

		this.gateIndex = in.readInt();
		this.noRecordsAvailableCounter = in.readInt();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);

		out.writeInt(this.gateIndex);
		out.writeInt(this.noRecordsAvailableCounter);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {

		if (!super.equals(obj)) {
			return false;
		}

		if (!(obj instanceof InputGateProfilingEvent)) {
			return false;
		}

		final InputGateProfilingEvent inputGateProfilingEvent = (InputGateProfilingEvent) obj;

		if (this.gateIndex != inputGateProfilingEvent.getGateIndex()) {
			return false;
		}

		if (this.noRecordsAvailableCounter != inputGateProfilingEvent.getNoRecordsAvailableCounter()) {
			return false;
		}

		return true;
	}
}
