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

package org.apache.flink.runtime.jobgraph;

import java.util.ArrayList;
import java.util.List;

/**
 * An intermediate data set is the data set produced by an operator - either a
 * source or any intermediate operation.
 * 
 * Intermediate data sets may be read by other operators, materialized, or
 * discarded.
 */
public class IntermediateDataSet implements java.io.Serializable {
	
	private static final long serialVersionUID = 1L;

	
	private final IntermediateDataSetID id; 		// the identifier
	
	private final AbstractJobVertex producer;		// the operation that produced this data set
	
	private final List<JobEdge> consumers = new ArrayList<JobEdge>();
	
	// --------------------------------------------------------------------------------------------
	
	public IntermediateDataSet(AbstractJobVertex producer) {
		this(new IntermediateDataSetID(), producer);
	}
	
	public IntermediateDataSet(IntermediateDataSetID id, AbstractJobVertex producer) {
		if (id == null || producer == null) {
			throw new NullPointerException();
		}
		
		this.id = id;
		this.producer = producer;
	}

	// --------------------------------------------------------------------------------------------
	
	public IntermediateDataSetID getId() {
		return id;
	}

	public AbstractJobVertex getProducer() {
		return producer;
	}
	
	public List<JobEdge> getConsumers() {
		return this.consumers;
	}
	
	// --------------------------------------------------------------------------------------------
	
	public void addConsumer(JobEdge edge) {
		this.consumers.add(edge);
	}
	
	// --------------------------------------------------------------------------------------------
	
	@Override
	public String toString() {
		return "Intermediate Data Set (" + id + ")";
	}
}
