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

package eu.stratosphere.compiler.plan;

import org.apache.flink.runtime.operators.DriverStrategy;

import eu.stratosphere.compiler.dag.DataSinkNode;

/**
 * Plan candidate node for data flow sinks.
 */
public class SinkPlanNode extends SingleInputPlanNode
{
	/**
	 * Constructs a new sink candidate node that uses <i>NONE</i> as its local strategy. Note that
	 * local sorting and range partitioning are handled by the incoming channel already.
	 * 
	 * @param template The template optimizer node that this candidate is created for.
	 */
	public SinkPlanNode(DataSinkNode template, String nodeName, Channel input) {
		super(template, nodeName, input, DriverStrategy.NONE);
		
		this.globalProps = input.getGlobalProperties().clone();
		this.localProps = input.getLocalProperties().clone();
	}
	
	public DataSinkNode getSinkNode() {
		if (this.template instanceof DataSinkNode) {
			return (DataSinkNode) this.template;
		} else {
			throw new RuntimeException();
		}
	}
}
