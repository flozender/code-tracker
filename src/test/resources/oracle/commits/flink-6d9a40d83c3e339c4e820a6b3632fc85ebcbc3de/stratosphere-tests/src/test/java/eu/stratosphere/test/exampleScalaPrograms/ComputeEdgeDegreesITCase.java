/***********************************************************************************************************************
 * Copyright (C) 2010-2013 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.test.exampleScalaPrograms;

import org.apache.flink.examples.scala.graph.ComputeEdgeDegrees;

import eu.stratosphere.api.common.Plan;
import eu.stratosphere.configuration.Configuration;

public class ComputeEdgeDegreesITCase extends eu.stratosphere.test.recordJobTests.ComputeEdgeDegreesITCase {

	public ComputeEdgeDegreesITCase(Configuration config) {
		super(config);
	}
	
	@Override
	protected Plan getTestJob() {
		ComputeEdgeDegrees computeDegrees = new ComputeEdgeDegrees();
		return computeDegrees.getScalaPlan(
				config.getInteger("ComputeEdgeDegreesTest#NumSubtasks", DOP),
				edgesPath, resultPath);
	}
}
