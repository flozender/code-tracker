/***********************************************************************************************************************
 *
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
 *
 **********************************************************************************************************************/
package org.apache.flink.api.java.operators;

import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.types.TypeInformation;

/**
 * Base class of all operators in the Java API.
 * 
 * @param <OUT> The type of the data set produced by this operator.
 * @param <O> The type of the operator, so that we can return it.
 */
public abstract class Operator<OUT, O extends Operator<OUT, O>> extends DataSet<OUT> {

	private String name;
	
	private int dop = -1;

	protected Operator(ExecutionEnvironment context, TypeInformation<OUT> resultType) {
		super(context, resultType);
	}
	
	/**
	 * Returns the type of the result of this operator.
	 * 
	 * @return The result type of the operator.
	 */
	public TypeInformation<OUT> getResultType() {
		return getType();
	}

	/**
	 * Returns the name of the operator. If no name has been set, it returns the name of the
	 * operation, or the name of the class implementing the function of this operator.
	 * 
	 * @return The name of the operator.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the degree of parallelism of this operator.
	 * 
	 * @return The degree of parallelism of this operator.
	 */
	public int getParallelism() {
		return this.dop;
	}

	/**
	 * Sets the name of this operator. This overrides the default name, which is either
	 * a generated description of the operation (such as for example "Aggregate(1:SUM, 2:MIN)")
	 * or the name the user-defined function or input/output format executed by the operator.
	 * 
	 * @param newName The name for this operator.
	 * @return The operator with a new name.
	 */
	public O name(String newName) {
		this.name = newName;
		@SuppressWarnings("unchecked")
		O returnType = (O) this;
		return returnType;
	}
	
	/**
	 * Sets the degree of parallelism for this operator.
	 * The degree must be 1 or more.
	 * 
	 * @param dop The degree of parallelism for this operator.
	 * @return The operator with set degree of parallelism.
	 */
	public O setParallelism(int dop) {
		if(dop < 1) {
			throw new IllegalArgumentException("The parallelism of an operator must be at least 1.");
		}
		this.dop = dop;
		
		@SuppressWarnings("unchecked")
		O returnType = (O) this;
		return returnType;
	}
}
