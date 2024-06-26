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

package org.apache.flink.compiler.dag;

import java.util.Collections;
import java.util.List;

import org.apache.flink.api.common.operators.base.BulkIterationBase.PartialSolutionPlaceHolder;
import org.apache.flink.compiler.DataStatistics;
import org.apache.flink.compiler.dataproperties.GlobalProperties;
import org.apache.flink.compiler.dataproperties.LocalProperties;
import org.apache.flink.compiler.plan.BulkPartialSolutionPlanNode;
import org.apache.flink.compiler.plan.Channel;
import org.apache.flink.compiler.plan.PlanNode;

/**
 * The optimizer's internal representation of the partial solution that is input to a bulk iteration.
 */
public class BulkPartialSolutionNode extends AbstractPartialSolutionNode {
	
	private final BulkIterationNode iterationNode;
	
	
	public BulkPartialSolutionNode(PartialSolutionPlaceHolder<?> psph, BulkIterationNode iterationNode) {
		super(psph);
		this.iterationNode = iterationNode;
	}

	// --------------------------------------------------------------------------------------------
	
	public void setCandidateProperties(GlobalProperties gProps, LocalProperties lProps, Channel initialInput) {
		if (this.cachedPlans != null) {
			throw new IllegalStateException();
		} else {
			this.cachedPlans = Collections.<PlanNode>singletonList(new BulkPartialSolutionPlanNode(this, "PartialSolution ("+this.getPactContract().getName()+")", gProps, lProps, initialInput));
		}
	}
	
	public BulkPartialSolutionPlanNode getCurrentPartialSolutionPlanNode() {
		if (this.cachedPlans != null) {
			return (BulkPartialSolutionPlanNode) this.cachedPlans.get(0);
		} else {
			throw new IllegalStateException();
		}
	}
	
	public BulkIterationNode getIterationNode() {
		return this.iterationNode;
	}
	
	@Override
	public void computeOutputEstimates(DataStatistics statistics) {
		copyEstimates(this.iterationNode.getPredecessorNode());
	}
	
	// --------------------------------------------------------------------------------------------

	/**
	 * Gets the contract object for this data source node.
	 * 
	 * @return The contract.
	 */
	@Override
	public PartialSolutionPlaceHolder<?> getPactContract() {
		return (PartialSolutionPlaceHolder<?>) super.getPactContract();
	}

	@Override
	public String getName() {
		return "Bulk Partial Solution";
	}

	@Override
	public void computeUnclosedBranchStack() {
		if (this.openBranches != null) {
			return;
		}

		OptimizerNode inputToIteration = this.iterationNode.getPredecessorNode();
		
		addClosedBranches(inputToIteration.closedBranchingNodes);
		List<UnclosedBranchDescriptor> fromInput = inputToIteration.getBranchesForParent(this.iterationNode.getIncomingConnection());
		this.openBranches = (fromInput == null || fromInput.isEmpty()) ? Collections.<UnclosedBranchDescriptor>emptyList() : fromInput;
	}
}
