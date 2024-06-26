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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.stratosphere.pact.common.contract.CoGroupContract;
import eu.stratosphere.pact.common.contract.Contract;
import eu.stratosphere.pact.common.contract.CrossContract;
import eu.stratosphere.pact.common.contract.DataSinkContract;
import eu.stratosphere.pact.common.contract.DataSourceContract;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.MatchContract;
import eu.stratosphere.pact.common.contract.OutputContractConfigurable;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.plan.Visitable;
import eu.stratosphere.pact.common.plan.Visitor;
import eu.stratosphere.pact.common.stub.Stub;
import eu.stratosphere.pact.compiler.CompilerException;
import eu.stratosphere.pact.compiler.Costs;
import eu.stratosphere.pact.compiler.DataStatistics;
import eu.stratosphere.pact.compiler.GlobalProperties;
import eu.stratosphere.pact.compiler.LocalProperties;
import eu.stratosphere.pact.compiler.OutputContract;
import eu.stratosphere.pact.compiler.costs.CostEstimator;
import eu.stratosphere.pact.runtime.task.util.TaskConfig.LocalStrategy;

/**
 * This class represents a node in the internal representation of the PACT plan. The internal
 * representation is used by the optimizer to determine the algorithms to be used
 * and to create the Nephele schedule for the runtime system.
 * 
 * @author Fabian Hüske (fabian.hueske@tu-berlin.de)
 * @author Stephan Ewen (stephan.ewen@tu -berlin.de)
 */
public abstract class OptimizerNode implements Visitable<OptimizerNode> {
	// ------------------------------------------------------------------------
	// Internal classes
	// ------------------------------------------------------------------------

	/**
	 * An enumeration describing the type of the PACT.
	 */
	public enum PactType {
		Cogroup(CoGroupContract.class), Cross(CrossContract.class), DataSource(DataSourceContract.class), DataSink(
				DataSinkContract.class), Map(MapContract.class), Match(MatchContract.class), Reduce(
				ReduceContract.class);

		private Class<? extends Contract> clazz; // The class describing the contract

		/**
		 * Private constructor to set enum attributes.
		 * 
		 * @param clazz
		 *        The class of the actual PACT contract represented by this enum constant.
		 */
		private PactType(Class<? extends Contract> clazz) {
			this.clazz = clazz;
		}

		/**
		 * Gets the class of the actual PACT contract represented by this enum constant.
		 * 
		 * @return The class of the actual PACT contract.
		 */
		public Class<? extends Contract> getPactClass() {
			return this.clazz;
		}

		/**
		 * Utility method that gets the enum constant for a PACT class.
		 * 
		 * @param pactClass
		 *        The PACT class to find the enum constant for.
		 * @return The enum constant for the given pact class.
		 */
		public static PactType getType(Class<? extends Contract> pactClass) {
			PactType[] values = PactType.values();
			for (int i = 0; i < values.length; i++) {
				if (pactClass == values[i].clazz) {
					return values[i];
				}
			}
			return null;
		}
	}

	// ------------------------------------------------------------------------
	// Members
	// ------------------------------------------------------------------------

	private final Contract pactContract; // The contract (Reduce / Match / DataSource / ...)

	private final OutputContract outputContract; // the outputContract

	private List<PactConnection> outgoingConnections; // The links to succeeding nodes

	private List<InterestingProperties> intProps; // the interesting properties of this node

	protected LocalProperties localProps; // local properties of the data produced by this node

	protected GlobalProperties globalProps; // global properties of the data produced by this node

	protected List<UnclosedBranchDescriptor> openBranches; // stack of branches in the sub-graph that are not yet

	// rejoined
	protected Map<OptimizerNode, OptimizerNode> branchPlan; // the actual plan alternative chosen at a specific branch

	// point

	protected LocalStrategy localStrategy; // The local strategy (sorting / hashing, ...)

	protected Costs nodeCosts; // the costs incurred by this node

	protected Costs cumulativeCosts; // the cumulative costs of all operators in the sub-tree

	// of and including this node

	protected long estimatedOutputSize = -1;

	protected long estimatedNumRecords = -1;

	protected long estimatedKeyCardinality = -1;

	private int degreeOfParallelism = -1; // the number of parallel instances of this node

	private int instancesPerMachine = -1; // the number of parallel instance that will run on the

	// same machine

	private int memoryPerTask; // the amount of memory dedicated to each task

	private int id = -1; // the id for this node.

	private boolean pFlag = false; // flag for the internal pruning algorithm

	// ------------------------------------------------------------------------
	// Constructor / Setup
	// ------------------------------------------------------------------------

	/**
	 * Creates a new node for the optimizer plan.
	 * 
	 * @param pactContract
	 *        The PACT that the node represents.
	 */
	public OptimizerNode(Contract pactContract) {
		if (pactContract == null) {
			throw new NullPointerException("The contract must not ne null.");
		}

		this.pactContract = pactContract;

		this.outgoingConnections = null;

		this.localProps = new LocalProperties();
		this.globalProps = new GlobalProperties();

		this.outputContract = determineOutputContractFromStub();
	}

	/**
	 * This is an internal copy-constructor that is used to create copies of the nodes
	 * for plan enumeration. This constructor copies all properties (deep, if objects)
	 * except the outgoing connections and the costs. The connections are omitted,
	 * because the copied nodes are used in different enumerated trees/graphs. The
	 * costs are only computed for the actual alternative plan, so they are not
	 * copied themselves.
	 * 
	 * @param toClone
	 *        The node to clone.
	 * @param globalProps
	 *        The global properties of this copy.
	 * @param globalProps
	 *        The local properties of this copy.
	 */
	protected OptimizerNode(OptimizerNode toClone, GlobalProperties globalProps, LocalProperties localProps) {
		this.pactContract = toClone.pactContract;
		this.localStrategy = toClone.localStrategy;

		this.outputContract = toClone.outputContract;

		this.localProps = localProps;
		this.globalProps = globalProps;

		this.estimatedOutputSize = toClone.estimatedOutputSize;
		this.estimatedKeyCardinality = toClone.estimatedKeyCardinality;
		this.estimatedNumRecords = toClone.estimatedNumRecords;

		this.id = toClone.id;
		this.degreeOfParallelism = toClone.degreeOfParallelism;
		this.instancesPerMachine = toClone.instancesPerMachine;

		// check, if this node branches. if yes, this candidate must be associated with
		// the branching template node.
		if (toClone.isBranching()) {
			this.branchPlan = new HashMap<OptimizerNode, OptimizerNode>(6);
			this.branchPlan.put(toClone, this);
		}
	}

	// ------------------------------------------------------------------------
	// Abstract methods that implement node specific behavior
	// and the pact type specific optimization methods.
	// ------------------------------------------------------------------------

	/**
	 * Gets the name of this node. This returns either the name of the PACT, or
	 * a string marking the node as a data source or a data sink.
	 * 
	 * @return The node name.
	 */
	public abstract String getName();

	/**
	 * This function is for plan translation purposes. Upon invocation, the implementing subclasses should
	 * examine its contained contract and look at the contracts that feed their data into that contract.
	 * The method should then create a <tt>PactConnection</tt> for each of those inputs.
	 * <p>
	 * In addition, the nodes must set the shipping strategy of the connection, if a suitable optimizer hint is found.
	 * 
	 * @param contractToNode
	 *        The map to translate the contracts to their corresponding optimizer nodes.
	 */
	public abstract void setInputs(Map<Contract, OptimizerNode> contractToNode);

	/**
	 * This method needs to be overridden by subclasses to return the children.
	 * 
	 * @return The list of incoming links.
	 */
	public abstract List<PactConnection> getIncomingConnections();

	/**
	 * Causes this node to compute its output estimates (such as number of rows, size in bytes)
	 * based on the inputs and the compiler hints. The compiler hints are instantiated with conservative
	 * default values which are used if no other values are provided. Nodes may access the statistics to
	 * determine relevant information.
	 * 
	 * @param statistics
	 *        The statistics object which may be accessed to get statistical information.
	 *        The parameter may be null, if no statistics are available.
	 */
	public abstract void computeOutputEstimates(DataStatistics statistics);

	/**
	 * Tells the node to compute the interesting properties for its inputs. The interesting properties
	 * for the node itself must have been computed before.
	 * The node must then see how many of interesting properties it preserves and add its own.
	 */
	public abstract void computeInterestingPropertiesForInputs(CostEstimator estimator);

	/**
	 * This method causes the node to compute the description of open branches in its sub-plan. An open branch
	 * describes, that a (transitive) child node had multiple outputs, which have not all been re-joined in the
	 * sub-plan. This method needs to set the <code>openBranches</code> field to a stack of unclosed branches, the
	 * latest one top. A branch is considered closed, if some later node sees all of the branching node's outputs,
	 * no matter if there have been more branches to different paths in the meantime.
	 */
	public abstract void computeUnclosedBranchStack();

	/**
	 * Computes the plan alternatives for this node, an implicitly for all nodes that are children of
	 * this node. This method must determine for each alternative the global and local properties
	 * and the costs. This method may recursively call <code>getAlternatives()</code> on its children
	 * to get their plan alternatives, and build its own alternatives on top of those.
	 * 
	 * @param estimator
	 *        The cost estimator used to estimate the costs of each plan alternative.
	 * @return A list containing all plan alternatives.
	 */
	public abstract List<? extends OptimizerNode> getAlternativePlans(CostEstimator estimator);

	/**
	 * This method implements the visit of a depth-first graph traversing visitor. Implementors must first
	 * call the <code>preVisit()</code> method, then hand the visitor to their children, and finally call
	 * the <code>postVisit()</code> method.
	 * 
	 * @param visitor
	 *        The graph traversing visitor.
	 * @see eu.stratosphere.pact.common.plan.Visitable#accept(eu.stratosphere.pact.common.plan.Visitor)
	 */
	public abstract void accept(Visitor<OptimizerNode> visitor);

	/**
	 * Checks, whether this node requires memory for its tasks or not.
	 * 
	 * @return True, if this node contains logic that requires memory usage, false otherwise.
	 */
	public abstract boolean isMemoryConsumer();

	// ------------------------------------------------------------------------
	// Getters / Setters
	// ------------------------------------------------------------------------

	/**
	 * Gets the ID of this node. If the id has not yet been set, this method returns -1;
	 * 
	 * @return This node's id, or -1, if not yet set.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the ID of this node.
	 * 
	 * @param id
	 *        The id for this node.
	 */
	public void SetId(int id) {
		this.id = id;
	}

	/**
	 * Adds a new outgoing connection to this node.
	 * 
	 * @param pactConnection
	 *        The connection to add.
	 */
	public void addOutgoingConnection(PactConnection pactConnection) {
		if (this.outgoingConnections == null) {
			this.outgoingConnections = new ArrayList<PactConnection>();
		} else {
			if (this.outgoingConnections.size() == 64) {
				throw new CompilerException("Cannot currently handle node with more than 64 outputs.");
			}
		}

		this.outgoingConnections.add(pactConnection);
	}

	/**
	 * The list of outgoing connections from this node to succeeding tasks.
	 * 
	 * @return The list of outgoing connections.
	 */
	public List<PactConnection> getOutgoingConnections() {
		return this.outgoingConnections == null ? Collections.<PactConnection> emptyList() : this.outgoingConnections;
	}

	/**
	 * Gets the object that specifically describes the contract of this node.
	 * 
	 * @return This node's contract.
	 */
	public Contract getPactContract() {
		return this.pactContract;
	}

	/**
	 * Gets the type of the PACT as a <tt>PactType</tt> enumeration constant for this node.
	 * 
	 * @return The type of the PACT.
	 */
	public PactType getPactType() {
		return PactType.getType(pactContract.getClass());
	}

	/**
	 * Gets the output contract declared on the user function that is wrapped in the PACT of this node.
	 * 
	 * @return The declared output contract, or <tt>OutputContract.None</tt>, if none was declared.
	 */
	public OutputContract getOutputContract() {
		return outputContract;
	}

	/**
	 * Gets the degree of parallelism for the contract represented by this optimizer node.
	 * The degree of parallelism denotes how many parallel instances of the user function will be
	 * spawned during the execution. If this value is <code>-1</code>, then the system will take
	 * the default number of parallel instances.
	 * 
	 * @return The degree of parallelism.
	 */
	public int getDegreeOfParallelism() {
		return degreeOfParallelism;
	}

	/**
	 * Sets the degree of parallelism for the contract represented by this optimizer node.
	 * The degree of parallelism denotes how many parallel instances of the user function will be
	 * spawned during the execution. If this value is set to <code>-1</code>, then the system will take
	 * the default number of parallel instances.
	 * 
	 * @param degreeOfParallelism
	 *        The degree of parallelism to set.
	 * @throws IllegalArgumentException
	 *         If the degree of parallelism is smaller than one.
	 */
	public void setDegreeOfParallelism(int degreeOfParallelism) {
		if (degreeOfParallelism < 1) {
			throw new IllegalArgumentException();
		}

		this.degreeOfParallelism = degreeOfParallelism;
	}

	/**
	 * Gets the number of parallel instances of the contract that are
	 * to be executed on the same machine.
	 * 
	 * @return The number of instances per machine.
	 */
	public int getInstancesPerMachine() {
		return instancesPerMachine;
	}

	/**
	 * Sets the number of parallel instances of the contract that are
	 * to be executed on the same machine.
	 * 
	 * @param instancesPerMachine
	 *        The instances per machine.
	 * @throws IllegalArgumentException
	 *         If the number of instances per machine is smaller than one.
	 */
	public void setInstancesPerMachine(int instancesPerMachine) {
		if (instancesPerMachine < 1) {
			throw new IllegalArgumentException();
		}
		this.instancesPerMachine = instancesPerMachine;
	}

	/**
	 * Gets the memory dedicated to each task for this node.
	 * 
	 * @return The memory per task.
	 */
	public int getMemoryPerTask() {
		return memoryPerTask;
	}

	/**
	 * Sets the memory dedicated to each task for this node.
	 * 
	 * @param memoryPerTask
	 *        The memory per task.
	 */
	public void setMemoryPerTask(int memoryPerTask) {
		this.memoryPerTask = memoryPerTask;
	}

	/**
	 * Gets the costs incurred by this node. The costs reflect also the costs incurred by the shipping strategies
	 * of the incoming connections.
	 * 
	 * @return The node-costs, or null, if not yet set.
	 */
	public Costs getNodeCosts() {
		return nodeCosts;
	}

	/**
	 * Gets the cumulative costs of this nose. The cumulative costs are the the sum of the costs
	 * of this node and of all nodes in the subtree below this node.
	 * 
	 * @return The cumulative costs, or null, if not yet set.
	 */
	public Costs getCumulativeCosts() {
		return cumulativeCosts;
	}

	/**
	 * Gets the local strategy from this node. This determines for example for a <i>match</i> Pact whether
	 * to use a sort-merge or a hybrid hash strategy.
	 * 
	 * @return The local strategy.
	 */
	public LocalStrategy getLocalStrategy() {
		return localStrategy;
	}

	/**
	 * Sets the local strategy from this node. This determines the algorithms to be used to prepare the data inside a
	 * partition.
	 * 
	 * @param strategy
	 *        The local strategy to be set.
	 */
	public void setLocalStrategy(LocalStrategy strategy) {
		this.localStrategy = strategy;
	}

	/**
	 * Gets the properties that are interesting for this node to produce.
	 * 
	 * @return The interesting properties for this node, or null, if not yet computed.
	 */
	public List<InterestingProperties> getInterestingProperties() {
		return this.intProps;
	}

	/**
	 * Gets the local properties from this OptimizedNode.
	 * 
	 * @return The local properties.
	 */
	public LocalProperties getLocalProperties() {
		return localProps;
	}

	/**
	 * Gets the global properties from this OptimizedNode.
	 * 
	 * @return The global properties.
	 */
	public GlobalProperties getGlobalProperties() {
		return globalProps;
	}

	/**
	 * Gets the estimated output size from this node.
	 * 
	 * @return The estimated output size.
	 */
	public long getEstimatedOutputSize() {
		return estimatedOutputSize;
	}

	/**
	 * Gets the estimated number of records in the output of this node.
	 * 
	 * @return The estimated number of records.
	 */
	public long getEstimatedNumRecords() {
		return estimatedNumRecords;
	}

	/**
	 * Gets the estimated key cardinality of this node's output.
	 * 
	 * @return The estimated key cardinality.
	 */
	public long getEstimatedKeyCardinality() {
		return estimatedKeyCardinality;
	}

	/**
	 * Checks whether this node has branching output. A node's output is branched, if it has more
	 * than one output connection.
	 * 
	 * @return True, if the node's output branches. False otherwise.
	 */
	public boolean isBranching() {
		return getOutgoingConnections() != null && getOutgoingConnections().size() > 1;
	}

	public void setCosts(Costs nodeCosts) {
		// set the node costs
		this.nodeCosts = nodeCosts;

		// the cumulative costs are the node costs plus the costs of all inputs
		this.cumulativeCosts = new Costs(0, 0);
		this.cumulativeCosts.addCosts(nodeCosts);

		for (PactConnection p : getIncomingConnections()) {
			this.cumulativeCosts.addCosts(p.getSourcePact().cumulativeCosts);

			// TODO: handle cycles such that costs are not added multiple times
		}
	}

	// ------------------------------------------------------------------------
	// Miscellaneous
	// ------------------------------------------------------------------------

	/**
	 * Checks, if all outgoing connections have their interesting properties set from their target nodes.
	 * 
	 * @return True, if on all outgoing connections, the interesting properties are set. False otherwise.
	 */
	public boolean haveAllOutputConnectionInterestingProperties() {
		if (outgoingConnections == null) {
			return true;
		}

		for (PactConnection conn : outgoingConnections) {
			if (conn.getInterestingProperties() == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Computes all the interesting properties that are relevant to this node. The interesting
	 * properties are a union of the interesting properties on each outgoing connection.
	 * However, if two interesting properties on the outgoing connections overlap,
	 * the interesting properties will occur only once in this set. For that, this
	 * method deduplicates and merges the interesting properties.
	 * This method returns copies of the original interesting properties objects and
	 * leaves the original objects, contained by the connections, unchanged.
	 */
	public void computeInterestingProperties() {
		List<InterestingProperties> props = new ArrayList<InterestingProperties>();

		List<PactConnection> conns = getOutgoingConnections();
		if (conns != null) {
			for (PactConnection conn : conns) {
				List<InterestingProperties> ips = conn.getInterestingProperties();
				InterestingProperties.mergeUnionOfInterestingProperties(props, ips);
			}
		}

		this.intProps = props.isEmpty() ? Collections.<InterestingProperties> emptyList() : props;
	}

	/**
	 * Utility method that gets the output contract declared on a user function.
	 * 
	 * @return The <tt>OutputContract</tt> enum for the declared output contract, or <tt>OutputContract.None</tt>,
	 *         if none is declared.
	 * @throws CompilerException
	 *         Thrown, if more than one output contract is declared on the user function.
	 */
	protected OutputContract determineOutputContractFromStub() {
		Class<? extends Annotation> clazz = null;
		OutputContract oc = null;

		// Check whether output Contact is overridden
		if (getPactContract() instanceof OutputContractConfigurable) {
			clazz = ((OutputContractConfigurable) getPactContract()).getOutputContract();
			if (clazz != null) {
				OutputContract cc = OutputContract.getOutputContract(clazz);
				if (cc != null) {
					oc = cc;
				}
			}
		}

		// get all annotations on the class
		if (oc == null) {
			Class<? extends Stub<?, ?>> stubClass = getPactContract().getStubClass();
			Annotation[] allAnnotations = stubClass.getAnnotations();

			// for each annotation, see if it is a output contract annotation
			for (int i = 0; i < allAnnotations.length; i++) {
				clazz = allAnnotations[i].annotationType();

				// check if this is an output contract annotation
				if (clazz.getEnclosingClass().equals(eu.stratosphere.pact.common.contract.OutputContract.class)) {
					OutputContract cc = OutputContract.getOutputContract(clazz);
					if (cc != null) {
						if (oc == null) {
							oc = cc;
						} else {
							throw new CompilerException("Contract '" + pactContract.getName() + "' ("
								+ getPactType().name() + ") has more than one output contract.");
						}
					}
				}
			}
		}

		return oc == null ? OutputContract.None : oc;
	}

	/**
	 * Takes the given list of plans that are candidates for this node in the final plan and retains for each distinct
	 * set of interesting properties only the cheapest plan.
	 * 
	 * @param plans
	 *        The plans to prune.
	 */
	public <T extends OptimizerNode> void prunePlanAlternatives(List<T> plans) {
		// shortcut for the case that there is only one plan
		if (plans.size() == 1) {
			return;
		}

		// if we have unjoined branches, split the list of plans such that only those
		// with the same candidates at the branch points are compared
		// otherwise, we may end up with the case that no compatible plans are found at
		// nodes that join
		if (this.openBranches == null) {
			prunePlansWithCommonBranchAlternatives(plans);
		} else {
			// TODO brute force still
			List<T> result = new ArrayList<T>();
			List<T> turn = new ArrayList<T>();

			while (!plans.isEmpty()) {
				turn.clear();
				T determiner = plans.remove(plans.size() - 1);
				turn.add(determiner);

				for (int k = plans.size() - 1; k >= 0; k--) {
					boolean equal = true;
					T toCheck = plans.get(k);

					for (int b = 0; b < this.openBranches.size(); b++) {
						OptimizerNode brancher = this.openBranches.get(b).branchingNode;
						OptimizerNode cand1 = determiner.branchPlan.get(brancher);
						OptimizerNode cand2 = toCheck.branchPlan.get(brancher);
						if (cand1 != cand2) {
							equal = false;
							break;
						}
					}

					if (equal) {
						turn.add(plans.remove(k));
					}
				}

				// now that we have only plans with the same branch alternatives, prune!
				if (turn.size() > 1) {
					prunePlansWithCommonBranchAlternatives(turn);
				}
				result.addAll(turn);
			}

			// after all turns are complete
			plans.clear();
			plans.addAll(result);
		}
	}

	private final <T extends OptimizerNode> void prunePlansWithCommonBranchAlternatives(List<T> plans) {
		List<List<T>> toKeep = new ArrayList<List<T>>(intProps.size()); // for each interesting property, which plans
		// are cheapest
		for (int i = 0; i < intProps.size(); i++) {
			toKeep.add(null);
		}

		T cheapest = null; // the overall cheapest plan

		// go over all plans from the list
		for (T candidate : plans) {
			// check if that plan is the overall cheapest
			if (cheapest == null || (cheapest.getCumulativeCosts().compareTo(candidate.getCumulativeCosts()) > 0)) {
				cheapest = candidate;
			}

			// find the interesting properties that this plan matches
			for (int i = 0; i < intProps.size(); i++) {
				if (intProps.get(i).isMetBy(candidate)) {
					// the candidate meets them
					if (toKeep.get(i) == null) {
						// first one to meet the interesting properties, so store it
						List<T> l = new ArrayList<T>(2);
						l.add(candidate);
						toKeep.set(i, l);
					} else {
						// others met that one before
						// see if that one is more expensive and not more general than
						// one of the others. If so, drop it.
						List<T> l = toKeep.get(i);
						boolean met = false;
						boolean replaced = false;

						for (int k = 0; k < l.size(); k++) {
							T other = l.get(k);

							// check if the candidate is both cheaper and at least as general
							if (other.getGlobalProperties().isMetBy(candidate.getGlobalProperties())
								&& other.getLocalProperties().isMetBy(candidate.getLocalProperties())
								&& other.getCumulativeCosts().compareTo(candidate.getCumulativeCosts()) > 0) {
								// replace that one with the candidate
								l.set(k, replaced ? null : candidate);
								replaced = true;
								met = true;
							} else {
								// check if the previous plan is more general and not more expensive than the candidate
								met |= (candidate.getGlobalProperties().isMetBy(other.getGlobalProperties())
									&& candidate.getLocalProperties().isMetBy(other.getLocalProperties()) && candidate
									.getCumulativeCosts().compareTo(other.getCumulativeCosts()) >= 0);
							}
						}

						if (!met) {
							l.add(candidate);
						}
					}
				}
			}
		}

		// all plans are set now
		plans.clear();

		// add the cheapest plan
		if (cheapest != null) {
			plans.add(cheapest);
			cheapest.pFlag = true; // remember that that plan is in the set
		}

		Costs cheapestCosts = cheapest.cumulativeCosts;

		// add all others, which are optimal for some interesting properties
		for (int i = 0; i < toKeep.size(); i++) {
			List<T> l = toKeep.get(i);

			if (l != null) {
				Costs maxDelta = intProps.get(i).getMaximalCosts();

				for (T plan : l) {
					if (plan != null && !plan.pFlag) {
						plan.pFlag = true;

						// check, if that plan is not more than the delta above the costs of the
						if (!cheapestCosts.isOtherMoreThanDeltaAbove(plan.getCumulativeCosts(), maxDelta)) {
							plans.add(plan);
						}
					}
				}
			}
		}

		// reset the flags
		for (T p : plans) {
			p.pFlag = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder bld = new StringBuilder();

		bld.append(getName());
		bld.append(" (").append(getPactType().name()).append(") ");

		if (localStrategy != null) {
			bld.append('(');
			bld.append(getLocalStrategy().name());
			bld.append(") ");
		}

		for (PactConnection conn : getIncomingConnections()) {
			bld.append('(').append(conn.getShipStrategy() == null ? "null" : conn.getShipStrategy().name()).append(')');
		}

		return bld.toString();
	}

	// ------------------------------------------------------------------------
	// Handling of branches
	// ------------------------------------------------------------------------

	public boolean hasUnclosedBranches() {
		return openBranches != null && !openBranches.isEmpty();
	}

	protected List<UnclosedBranchDescriptor> getBranchesForParent(OptimizerNode parent) {
		if (outgoingConnections.size() == 1) {
			// return our own stack of open branches, because nothing is added
			return this.openBranches;
		} else if (outgoingConnections.size() > 1) {
			// we branch add a branch info to the stack
			List<UnclosedBranchDescriptor> branches = new ArrayList<UnclosedBranchDescriptor>(4);
			if (this.openBranches != null) {
				branches.addAll(this.openBranches);
			}

			// find out, which output number the connection to the parent
			int num;
			for (num = 0; num < outgoingConnections.size(); num++) {
				if (outgoingConnections.get(num).getTargetPact() == parent) {
					break;
				}
			}
			if (num >= outgoingConnections.size()) {
				throw new CompilerException("Error in compiler: "
					+ "Parent to get branch info for is not contained in the outgoing connections.");
			}

			// create the description and add it
			long bitvector = 0x1L << num;
			branches.add(new UnclosedBranchDescriptor(this, bitvector));
			return branches;
		} else {
			throw new CompilerException(
				"Error in compiler: Cannot get branch info for parent in a node woth no parents.");
		}
	}

	protected final class UnclosedBranchDescriptor {
		protected OptimizerNode branchingNode;

		protected long joinedPathsVector;

		/**
		 * @param branchingNode
		 * @param joinedPathsVector
		 */
		protected UnclosedBranchDescriptor(OptimizerNode branchingNode, long joinedPathsVector) {
			this.branchingNode = branchingNode;
			this.joinedPathsVector = joinedPathsVector;
		}

		public OptimizerNode getBranchingNode() {
			return branchingNode;
		}

		public long getJoinedPathsVector() {
			return joinedPathsVector;
		}
	}
}
