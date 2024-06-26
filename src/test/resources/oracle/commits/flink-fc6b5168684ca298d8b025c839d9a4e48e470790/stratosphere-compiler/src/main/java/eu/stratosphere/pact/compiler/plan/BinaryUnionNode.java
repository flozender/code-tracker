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

package eu.stratosphere.pact.compiler.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.stratosphere.api.functions.AbstractStub;
import eu.stratosphere.api.operators.Contract;
import eu.stratosphere.api.operators.DualInputContract;
import eu.stratosphere.api.operators.util.UserCodeClassWrapper;
import eu.stratosphere.pact.compiler.CompilerException;
import eu.stratosphere.pact.compiler.DataStatistics;
import eu.stratosphere.pact.compiler.costs.CostEstimator;
import eu.stratosphere.pact.compiler.dataproperties.GlobalProperties;
import eu.stratosphere.pact.compiler.dataproperties.InterestingProperties;
import eu.stratosphere.pact.compiler.dataproperties.RequestedGlobalProperties;
import eu.stratosphere.pact.compiler.dataproperties.RequestedLocalProperties;
import eu.stratosphere.pact.compiler.operators.BinaryUnionOpDescriptor;
import eu.stratosphere.pact.compiler.operators.OperatorDescriptorDual;
import eu.stratosphere.pact.compiler.plan.candidate.Channel;
import eu.stratosphere.pact.compiler.plan.candidate.PlanNode;
import eu.stratosphere.pact.runtime.shipping.ShipStrategyType;

/**
 * The Optimizer representation of a <i>Union</i>. A Union is automatically
 * inserted before any node which has more than one incoming connection per
 * input.
 */
public class BinaryUnionNode extends TwoInputNode {
	
	private Set<RequestedGlobalProperties> channelProps;

	public BinaryUnionNode(OptimizerNode pred1, OptimizerNode pred2) {
		super(new UnionPlaceholderContract());
		
		this.input1 = new PactConnection(pred1, this, pred1.getMaxDepth() + 1);
		this.input2 = new PactConnection(pred2, this, pred2.getMaxDepth() + 1);
		
		pred1.addOutgoingConnection(this.input1);
		pred2.addOutgoingConnection(this.input2);
	}
	
	public BinaryUnionNode(OptimizerNode pred1, OptimizerNode pred2, ShipStrategyType preSet) {
		this(pred1, pred2);
		
		if (preSet != null) {
			this.input1.setShipStrategy(preSet);
			this.input2.setShipStrategy(preSet);
		}
	}

	@Override
	public String getName() {
		return "Union";
	}
	
	@Override
	public void setInputs(Map<Contract, OptimizerNode> contractToNode) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected List<OperatorDescriptorDual> getPossibleProperties() {
		return new ArrayList<OperatorDescriptorDual>();
	}
	
	@Override
	public void computeUnionOfInterestingPropertiesFromSuccessors() {
		super.computeUnionOfInterestingPropertiesFromSuccessors();
		// clear all local properties, as they are destroyed anyways
		getInterestingProperties().getLocalProperties().clear();
	}
	
	@Override
	public void computeInterestingPropertiesForInputs(CostEstimator estimator) { 
		final InterestingProperties props = getInterestingProperties();
		
		// if no other properties exist, add the pruned trivials back
		if (props.getGlobalProperties().isEmpty()) {
			props.addGlobalProperties(new RequestedGlobalProperties());
		}
		props.addLocalProperties(new RequestedLocalProperties());
		this.input1.setInterestingProperties(props.clone());
		this.input2.setInterestingProperties(props.clone());
		
		this.channelProps = props.getGlobalProperties();
	}
	
	@Override
	public List<PlanNode> getAlternativePlans(CostEstimator estimator) {
		// check if we have a cached version
		if (this.cachedPlans != null) {
			return this.cachedPlans;
		}

		// step down to all producer nodes and calculate alternative plans
		final List<? extends PlanNode> subPlans1 = getFirstPredecessorNode().getAlternativePlans(estimator);
		final List<? extends PlanNode> subPlans2 = getSecondPredecessorNode().getAlternativePlans(estimator);
		
		final ArrayList<PlanNode> outputPlans = new ArrayList<PlanNode>();
		
		final BinaryUnionOpDescriptor operator = new BinaryUnionOpDescriptor();
		final RequestedLocalProperties noLocalProps = new RequestedLocalProperties();
		
		final int dop = getDegreeOfParallelism();
		final int subPerInstance = getSubtasksPerInstance();
		final int numInstances = dop / subPerInstance + (dop % subPerInstance == 0 ? 0 : 1);
		final int inDop1 = getFirstPredecessorNode().getDegreeOfParallelism();
		final int inSubPerInstance1 = getFirstPredecessorNode().getSubtasksPerInstance();
		final int inNumInstances1 = inDop1 / inSubPerInstance1 + (inDop1 % inSubPerInstance1 == 0 ? 0 : 1);
		final int inDop2 = getSecondPredecessorNode().getDegreeOfParallelism();
		final int inSubPerInstance2 = getSecondPredecessorNode().getSubtasksPerInstance();
		final int inNumInstances2 = inDop2 / inSubPerInstance2 + (inDop2 % inSubPerInstance2 == 0 ? 0 : 1);
		
		final boolean globalDopChange1 = numInstances != inNumInstances1;
		final boolean globalDopChange2 = numInstances != inNumInstances2;
		final boolean localDopChange1 = numInstances == inNumInstances1 & subPerInstance != inSubPerInstance1;
		final boolean localDopChange2 = numInstances == inNumInstances2 & subPerInstance != inSubPerInstance2;
		
		// enumerate all pairwise combination of the children's plans together with
		// all possible operator strategy combination
		
		// create all candidates
		for (PlanNode child1 : subPlans1) {
			for (PlanNode child2 : subPlans2) {
				
				// check that the children go together. that is the case if they build upon the same
				// candidate at the joined branch plan. 
				if (!areBranchCompatible(child1, child2)) {
					continue;
				}
				
				for (RequestedGlobalProperties igps: this.channelProps) {
					// create a candidate channel for the first input. mark it cached, if the connection says so
					Channel c1 = new Channel(child1, this.input1.getMaterializationMode());
					if (this.input1.getShipStrategy() == null) {
						// free to choose the ship strategy
						igps.parameterizeChannel(c1, globalDopChange1, localDopChange1);
						
						// if the DOP changed, make sure that we cancel out properties, unless the
						// ship strategy preserves/establishes them even under changing DOPs
						if (globalDopChange1 && !c1.getShipStrategy().isNetworkStrategy()) {
							c1.getGlobalProperties().reset();
						}
						if (localDopChange1 && !(c1.getShipStrategy().isNetworkStrategy() || 
									c1.getShipStrategy().compensatesForLocalDOPChanges())) {
							c1.getGlobalProperties().reset();
						}
					} else {
						// ship strategy fixed by compiler hint
						if (this.keys1 != null) {
							c1.setShipStrategy(this.input1.getShipStrategy(), this.keys1.toFieldList());
						} else {
							c1.setShipStrategy(this.input1.getShipStrategy());
						}
						
						if (globalDopChange1) {
							c1.adjustGlobalPropertiesForFullParallelismChange();
						} else if (localDopChange1) {
							c1.adjustGlobalPropertiesForLocalParallelismChange();
						}
					}
					
					// create a candidate channel for the first input. mark it cached, if the connection says so
					Channel c2 = new Channel(child2, this.input2.getMaterializationMode());
					if (this.input2.getShipStrategy() == null) {
						// free to choose the ship strategy
						igps.parameterizeChannel(c2, globalDopChange2, localDopChange2);
						
						// if the DOP changed, make sure that we cancel out properties, unless the
						// ship strategy preserves/establishes them even under changing DOPs
						if (globalDopChange2 && !c2.getShipStrategy().isNetworkStrategy()) {
							c2.getGlobalProperties().reset();
						}
						if (localDopChange2 && !(c2.getShipStrategy().isNetworkStrategy() || 
									c2.getShipStrategy().compensatesForLocalDOPChanges())) {
							c2.getGlobalProperties().reset();
						}
					} else {
						// ship strategy fixed by compiler hint
						if (this.keys2 != null) {
							c2.setShipStrategy(this.input2.getShipStrategy(), this.keys2.toFieldList());
						} else {
							c2.setShipStrategy(this.input2.getShipStrategy());
						}
						
						if (globalDopChange2) {
							c2.adjustGlobalPropertiesForFullParallelismChange();
						} else if (localDopChange2) {
							c2.adjustGlobalPropertiesForLocalParallelismChange();
						}
					}
					
					// get the global properties and clear unique fields (not preserved anyways during the union)
					GlobalProperties p1 = c1.getGlobalProperties();
					GlobalProperties p2 = c2.getGlobalProperties();
					p1.clearUniqueFieldCombinations();
					p2.clearUniqueFieldCombinations();
					
					// adjust the partitionings, if they exist but are not equal. this may happen when both channels have a
					// partitioning that fulfills the requirements, but both are incompatible. For example may a property requirement
					// be ANY_PARTITIONING on fields (0) and one channel is range partitioned on that field, the other is hash
					// partitioned on that field. 
					if (!igps.isTrivial() && !(p1.equals(p2))) {
						if (c1.getShipStrategy() == ShipStrategyType.FORWARD && c2.getShipStrategy() != ShipStrategyType.FORWARD) {
							// adjust c2 to c1
							c2 = c2.clone();
							p1.parameterizeChannel(c2,globalDopChange2);
						} else if (c2.getShipStrategy() == ShipStrategyType.FORWARD && c1.getShipStrategy() != ShipStrategyType.FORWARD) {
							// adjust c1 to c2
							c1 = c1.clone();
							p2.parameterizeChannel(c1,globalDopChange1);
						} else if (c1.getShipStrategy() == ShipStrategyType.FORWARD && c2.getShipStrategy() == ShipStrategyType.FORWARD) {
							boolean adjustC1 = c1.getEstimatedOutputSize() <= 0 || c2.getEstimatedOutputSize() <= 0 ||
									c1.getEstimatedOutputSize() <= c2.getEstimatedOutputSize();
							if (adjustC1) {
								c2 = c2.clone();
								p1.parameterizeChannel(c2, globalDopChange2);
							} else {
								c1 = c1.clone();
								p2.parameterizeChannel(c1, globalDopChange1);
							}
						} else {
							// this should never happen, as it implies both realize a different strategy, which is
							// excluded by the check that the required strategies must match
							throw new CompilerException("Bug in Plan Enumeration for Union Node.");
						}
					}
					
					instantiate(operator, c1, c2, outputPlans, estimator, igps, igps, noLocalProps, noLocalProps);
				}
			}
		}

		// cost and prune the plans
		for (PlanNode node : outputPlans) {
			estimator.costOperator(node);
		}
		prunePlanAlternatives(outputPlans);
		outputPlans.trimToSize();

		this.cachedPlans = outputPlans;
		return outputPlans;
	}
	
	@Override
	protected void readStubAnnotations() {}

	@Override
	protected void readConstantAnnotation() {}

	@Override
	public boolean isFieldConstant(int input, int fieldNumber) {
		return true;
	}
	
	@Override
	public void computeOutputEstimates(DataStatistics statistics) {
		OptimizerNode in1 = getFirstPredecessorNode();
		OptimizerNode in2 = getSecondPredecessorNode();
		
		this.estimatedNumRecords = in1.estimatedNumRecords > 0 && in2.estimatedNumRecords > 0 ?
				in1.estimatedNumRecords + in2.estimatedNumRecords : -1;
		this.estimatedOutputSize = in1.estimatedOutputSize > 0 && in2.estimatedOutputSize > 0 ?
			in1.estimatedOutputSize + in2.estimatedOutputSize : -1;
	}
	
	@Override
	protected long computeNumberOfStubCalls() {
		return this.estimatedNumRecords;
	}
	
	@Override
	protected double computeAverageRecordWidth() {
		if (this.estimatedNumRecords == -1 || this.estimatedOutputSize == -1) return -1;
		
		final double width = this.estimatedOutputSize / (double) this.estimatedNumRecords;

		// a record must have at least one byte...
		if(width < 1) {
			return 1;
		} else {
			return width;
		}
	}
	
	// ------------------------------------------------------------------------
	//  Mock classes that represents a contract without behavior.
	// ------------------------------------------------------------------------
	
	private static final class MockStub extends AbstractStub {}
	
	private static final class UnionPlaceholderContract extends DualInputContract<MockStub> {
		private UnionPlaceholderContract() {
			super(new UserCodeClassWrapper<MockStub>(MockStub.class), "Union");
		}
	}
}
