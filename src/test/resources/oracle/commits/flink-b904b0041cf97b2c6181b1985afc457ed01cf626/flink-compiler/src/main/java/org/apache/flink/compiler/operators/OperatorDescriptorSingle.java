/*
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


package org.apache.flink.compiler.operators;

import java.util.List;

import org.apache.flink.api.common.operators.util.FieldList;
import org.apache.flink.api.common.operators.util.FieldSet;
import org.apache.flink.compiler.dag.SingleInputNode;
import org.apache.flink.compiler.dataproperties.GlobalProperties;
import org.apache.flink.compiler.dataproperties.LocalProperties;
import org.apache.flink.compiler.dataproperties.RequestedGlobalProperties;
import org.apache.flink.compiler.dataproperties.RequestedLocalProperties;
import org.apache.flink.compiler.plan.Channel;
import org.apache.flink.compiler.plan.SingleInputPlanNode;

/**
 * 
 */
public abstract class OperatorDescriptorSingle implements AbstractOperatorDescriptor {
	
	protected final FieldSet keys;			// the set of key fields
	protected final FieldList keyList;		// the key fields with ordered field positions
	
	private List<RequestedGlobalProperties> globalProps;
	private List<RequestedLocalProperties> localProps;
	
	
	protected OperatorDescriptorSingle() {
		this(null);
	}
	
	protected OperatorDescriptorSingle(FieldSet keys) {
		this.keys = keys;
		this.keyList = keys == null ? null : keys.toFieldList();
	}
	
	
	public List<RequestedGlobalProperties> getPossibleGlobalProperties() {
		if (this.globalProps == null) {
			this.globalProps = createPossibleGlobalProperties();
		}
		return this.globalProps;
	}
	
	public List<RequestedLocalProperties> getPossibleLocalProperties() {
		if (this.localProps == null) {
			this.localProps = createPossibleLocalProperties();
		}
		return this.localProps;
	}
	
	protected abstract List<RequestedGlobalProperties> createPossibleGlobalProperties();
	
	protected abstract List<RequestedLocalProperties> createPossibleLocalProperties();
	
	public abstract SingleInputPlanNode instantiate(Channel in, SingleInputNode node);
	
	public abstract GlobalProperties computeGlobalProperties(GlobalProperties in);
	
	public abstract LocalProperties computeLocalProperties(LocalProperties in);
}
