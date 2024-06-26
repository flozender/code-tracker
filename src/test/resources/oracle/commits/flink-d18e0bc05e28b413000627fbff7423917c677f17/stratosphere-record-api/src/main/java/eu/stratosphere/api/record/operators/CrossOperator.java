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

package eu.stratosphere.api.record.operators;

import java.util.ArrayList;
import java.util.List;

import eu.stratosphere.api.operators.Operator;
import eu.stratosphere.api.operators.base.CrossOperatorBase;
import eu.stratosphere.api.operators.util.UserCodeClassWrapper;
import eu.stratosphere.api.operators.util.UserCodeObjectWrapper;
import eu.stratosphere.api.operators.util.UserCodeWrapper;
import eu.stratosphere.api.record.functions.CrossFunction;
import eu.stratosphere.types.Key;


/**
 * CrossOperator represents a Cross InputContract of the PACT Programming Model.
 *  InputContracts are second-order functions. They have one or multiple input sets of records and a first-order
 *  user function (stub implementation).
 * <p> 
 * Cross works on two inputs and calls the first-order function of a {@link CrossFunction} 
 * for each combination of record from both inputs (each element of the Cartesian Product) independently.
 * 
 * @see CrossFunction
 */
public class CrossOperator extends CrossOperatorBase<CrossFunction> implements RecordOperator {
	
	private static String DEFAULT_NAME = "<Unnamed Crosser>";

	// --------------------------------------------------------------------------------------------
	
	/**
	 * Creates a Builder with the provided {@link CrossFunction} implementation.
	 * 
	 * @param udf The {@link CrossFunction} implementation for this Cross contract.
	 */
	public static Builder builder(CrossFunction udf) {
		return new Builder(new UserCodeObjectWrapper<CrossFunction>(udf));
	}
	
	/**
	 * Creates a Builder with the provided {@link CrossFunction} implementation.
	 * 
	 * @param udf The {@link CrossFunction} implementation for this Cross contract.
	 */
	public static Builder builder(Class<? extends CrossFunction> udf) {
		return new Builder(new UserCodeClassWrapper<CrossFunction>(udf));
	}
	
	/**
	 * The private constructor that only gets invoked from the Builder.
	 * @param builder
	 */
	protected CrossOperator(Builder builder) {
		super(builder.udf, builder.name);
		setFirstInputs(builder.inputs1);
		setSecondInputs(builder.inputs2);
	}
	
	/* (non-Javadoc)
	 * @see eu.stratosphere.api.record.operators.RecordOperator#getKeyClasses()
	 */
	@Override
	public Class<? extends Key>[] getKeyClasses() {
		return emptyClassArray();
	}
	
	// --------------------------------------------------------------------------------------------

	/**
	 * Builder pattern, straight from Joshua Bloch's Effective Java (2nd Edition).
	 */
	public static class Builder {
		
		/* The required parameters */
		private final UserCodeWrapper<CrossFunction> udf;
		
		/* The optional parameters */
		private List<Operator> inputs1;
		private List<Operator> inputs2;
		private String name = DEFAULT_NAME;
		
		/**
		 * Creates a Builder with the provided {@link CrossFunction} implementation.
		 * 
		 * @param udf The {@link CrossFunction} implementation for this Cross contract.
		 */
		protected Builder(UserCodeWrapper<CrossFunction> udf) {
			this.udf = udf;
			this.inputs1 = new ArrayList<Operator>();
			this.inputs2 = new ArrayList<Operator>();
		}
		
		/**
		 * Sets one or several inputs (union) for input 1.
		 * 
		 * @param inputs
		 */
		public Builder input1(Operator ...inputs) {
			this.inputs1.clear();
			for (Operator c : inputs) {
				this.inputs1.add(c);
			}
			return this;
		}
		
		/**
		 * Sets one or several inputs (union) for input 2.
		 * 
		 * @param inputs
		 */
		public Builder input2(Operator ...inputs) {
			this.inputs2.clear();
			for (Operator c : inputs) {
				this.inputs2.add(c);
			}
			return this;
		}
		
		/**
		 * Sets the first inputs.
		 * 
		 * @param inputs
		 */
		public Builder inputs1(List<Operator> inputs) {
			this.inputs1 = inputs;
			return this;
		}
		
		/**
		 * Sets the second inputs.
		 * 
		 * @param inputs
		 */
		public Builder inputs2(List<Operator> inputs) {
			this.inputs2 = inputs;
			return this;
		}
		
		/**
		 * Sets the name of this contract.
		 * 
		 * @param name
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		/**
		 * Creates and returns a CrossOperator from using the values given 
		 * to the builder.
		 * 
		 * @return The created contract
		 */
		public CrossOperator build() {
			return new CrossOperator(this);
		}
	}
}
