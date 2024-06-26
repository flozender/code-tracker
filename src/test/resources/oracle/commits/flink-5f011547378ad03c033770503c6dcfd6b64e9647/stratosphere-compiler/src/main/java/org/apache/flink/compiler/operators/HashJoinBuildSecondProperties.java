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
import org.apache.flink.compiler.CompilerException;
import org.apache.flink.compiler.dag.TwoInputNode;
import org.apache.flink.compiler.dataproperties.LocalProperties;
import org.apache.flink.compiler.dataproperties.RequestedLocalProperties;
import org.apache.flink.compiler.plan.Channel;
import org.apache.flink.compiler.plan.DualInputPlanNode;
import org.apache.flink.runtime.operators.DriverStrategy;

public final class HashJoinBuildSecondProperties extends AbstractJoinDescriptor {
	
	public HashJoinBuildSecondProperties(FieldList keys1, FieldList keys2) {
		super(keys1, keys2);
	}

	@Override
	public DriverStrategy getStrategy() {
		return DriverStrategy.HYBRIDHASH_BUILD_SECOND;
	}

	@Override
	protected List<LocalPropertiesPair> createPossibleLocalProperties() {
		// all properties are possible
		return Collections.singletonList(new LocalPropertiesPair(
			new RequestedLocalProperties(), new RequestedLocalProperties()));
	}
	
	@Override
	public boolean areCoFulfilled(RequestedLocalProperties requested1, RequestedLocalProperties requested2,
			LocalProperties produced1, LocalProperties produced2)
	{
		return true;
	}

	@Override
	public DualInputPlanNode instantiate(Channel in1, Channel in2, TwoInputNode node) {
		DriverStrategy strategy;
		
		if (!in2.isOnDynamicPath() && in1.isOnDynamicPath()) {
			// sanity check that the first input is cached and remove that cache
			if (!in2.getTempMode().isCached()) {
				throw new CompilerException("No cache at point where static and dynamic parts meet.");
			}
			
			in2.setTempMode(in2.getTempMode().makeNonCached());
			strategy = DriverStrategy.HYBRIDHASH_BUILD_SECOND_CACHED;
		}
		else {
			strategy = DriverStrategy.HYBRIDHASH_BUILD_SECOND;
		}
		return new DualInputPlanNode(node, "Join("+node.getPactContract().getName()+")", in1, in2, strategy, this.keys1, this.keys2);
	}
	
	@Override
	public LocalProperties computeLocalProperties(LocalProperties in1, LocalProperties in2) {
		return new LocalProperties();
	}
}
