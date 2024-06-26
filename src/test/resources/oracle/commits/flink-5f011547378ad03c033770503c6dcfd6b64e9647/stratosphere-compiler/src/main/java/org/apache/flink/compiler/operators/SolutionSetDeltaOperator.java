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

package org.apache.flink.compiler.operators;

import java.util.Collections;
import java.util.List;

import org.apache.flink.api.common.operators.util.FieldList;
import org.apache.flink.compiler.dag.SingleInputNode;
import org.apache.flink.compiler.dataproperties.GlobalProperties;
import org.apache.flink.compiler.dataproperties.LocalProperties;
import org.apache.flink.compiler.dataproperties.RequestedGlobalProperties;
import org.apache.flink.compiler.dataproperties.RequestedLocalProperties;
import org.apache.flink.compiler.plan.Channel;
import org.apache.flink.compiler.plan.SingleInputPlanNode;
import org.apache.flink.runtime.operators.DriverStrategy;

/**
 *
 */
public class SolutionSetDeltaOperator extends OperatorDescriptorSingle {

	public SolutionSetDeltaOperator(FieldList partitioningFields) {
		super(partitioningFields);
	}
	
	@Override
	public DriverStrategy getStrategy() {
		return DriverStrategy.UNARY_NO_OP;
	}

	@Override
	public SingleInputPlanNode instantiate(Channel in, SingleInputNode node) {
		return new SingleInputPlanNode(node, "SolutionSet Delta", in, DriverStrategy.UNARY_NO_OP);
	}

	@Override
	protected List<RequestedGlobalProperties> createPossibleGlobalProperties() {
		RequestedGlobalProperties partProps = new RequestedGlobalProperties();
		partProps.setHashPartitioned(this.keyList);
		return Collections.singletonList(partProps);
	}

	@Override
	protected List<RequestedLocalProperties> createPossibleLocalProperties() {
		return Collections.singletonList(new RequestedLocalProperties());
	}
	
	@Override
	public GlobalProperties computeGlobalProperties(GlobalProperties gProps) {
		return gProps;
	}
	
	@Override
	public LocalProperties computeLocalProperties(LocalProperties lProps) {
		return lProps;
	}
}
