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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.flink.compiler.dag.TwoInputNode;
import org.apache.flink.compiler.dataproperties.GlobalProperties;
import org.apache.flink.compiler.dataproperties.LocalProperties;
import org.apache.flink.compiler.dataproperties.PartitioningProperty;
import org.apache.flink.compiler.dataproperties.RequestedGlobalProperties;
import org.apache.flink.compiler.dataproperties.RequestedLocalProperties;
import org.apache.flink.compiler.plan.Channel;
import org.apache.flink.compiler.plan.DualInputPlanNode;


public abstract class CartesianProductDescriptor extends OperatorDescriptorDual {
	
	private final boolean allowBroadcastFirst;
	private final boolean allowBroadcastSecond;
	
	
	protected CartesianProductDescriptor(boolean allowBroadcastFirst, boolean allowBroadcastSecond) {
		if (!(allowBroadcastFirst | allowBroadcastSecond)) {
			throw new IllegalArgumentException();
		}

		this.allowBroadcastFirst = allowBroadcastFirst;
		this.allowBroadcastSecond = allowBroadcastSecond;
	}
	
	
	@Override
	protected List<GlobalPropertiesPair> createPossibleGlobalProperties() {
		ArrayList<GlobalPropertiesPair> pairs = new ArrayList<GlobalPropertiesPair>();
		
		if (this.allowBroadcastFirst) {
			// replicate first
			RequestedGlobalProperties replicated1 = new RequestedGlobalProperties();
			replicated1.setFullyReplicated();
			RequestedGlobalProperties any2 = new RequestedGlobalProperties();
			pairs.add(new GlobalPropertiesPair(replicated1, any2));
		}
		
		if (this.allowBroadcastSecond) {
			// replicate second
			RequestedGlobalProperties any1 = new RequestedGlobalProperties();
			RequestedGlobalProperties replicated2 = new RequestedGlobalProperties();
			replicated2.setFullyReplicated();
			pairs.add(new GlobalPropertiesPair(any1, replicated2));
		}

		return pairs;
	}
	
	@Override
	protected List<LocalPropertiesPair> createPossibleLocalProperties() {
		// all properties are possible
		return Collections.singletonList(new LocalPropertiesPair(
			new RequestedLocalProperties(), new RequestedLocalProperties()));
	}

	@Override
	public boolean areCoFulfilled(RequestedLocalProperties requested1, RequestedLocalProperties requested2,
			LocalProperties produced1, LocalProperties produced2) {
		return true;
	}
	
	@Override
	public DualInputPlanNode instantiate(Channel in1, Channel in2, TwoInputNode node) {
		return new DualInputPlanNode(node, "Cross("+node.getPactContract().getName()+")", in1, in2, getStrategy());
	}
	
	@Override
	public GlobalProperties computeGlobalProperties(GlobalProperties in1, GlobalProperties in2) {
		GlobalProperties gp = GlobalProperties.combine(in1, in2);
		if (gp.getUniqueFieldCombination() != null && gp.getUniqueFieldCombination().size() > 0 &&
					gp.getPartitioning() == PartitioningProperty.RANDOM)
		{
			gp.setAnyPartitioning(gp.getUniqueFieldCombination().iterator().next().toFieldList());
		}
		gp.clearUniqueFieldCombinations();
		return gp;
	}
}
