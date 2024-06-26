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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.flink.api.common.operators.DualInputSemanticProperties;
import org.apache.flink.api.java.functions.FunctionAnnotation;
import org.apache.flink.api.java.functions.SemanticPropUtil;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.TypeInformation;

import org.apache.flink.api.java.DataSet;

/**
 * The <tt>TwoInputUdfOperator</tt> is the base class of all binary operators that execute
 * user-defined functions (UDFs). The UDFs encapsulated by this operator are naturally UDFs that
 * have two inputs (such as {@link org.apache.flink.api.java.functions.JoinFunction} or 
 * {@link org.apache.flink.api.java.functions.CoGroupFunction}).
 * <p>
 * This class encapsulates utilities for the UDFs, such as broadcast variables, parameterization
 * through configuration objects, and semantic properties.
 *
 * @param <IN1> The data type of the first input data set.
 * @param <IN2> The data type of the second input data set.
 * @param <OUT> The data type of the returned data set.
 */
public abstract class TwoInputUdfOperator<IN1, IN2, OUT, O extends TwoInputUdfOperator<IN1, IN2, OUT, O>>
	extends TwoInputOperator<IN1, IN2, OUT, O> implements UdfOperator<O>
{
	private Configuration parameters;

	private Map<String, DataSet<?>> broadcastVariables;

	private DualInputSemanticProperties udfSemantics;

	// --------------------------------------------------------------------------------------------

	/**
	 * Creates a new operators with the two given data sets as inputs. The given result type
	 * describes the data type of the elements in the data set produced by the operator.
	 *
	 * @param input1 The data set for the first input.
	 * @param input2 The data set for the second input.
	 * @param resultType The type of the elements in the resulting data set.
	 */
	protected TwoInputUdfOperator(DataSet<IN1> input1, DataSet<IN2> input2, TypeInformation<OUT> resultType) {
		super(input1, input2, resultType);
	}
	
	protected void extractSemanticAnnotationsFromUdf(Class<?> udfClass) {
		Set<Annotation> annotations = FunctionAnnotation.readDualConstantAnnotations(udfClass);
		
		DualInputSemanticProperties dsp = SemanticPropUtil.getSemanticPropsDual(annotations,
					getInput1Type(), getInput2Type(), getResultType());

		setSemanticProperties(dsp);
	}

	// --------------------------------------------------------------------------------------------
	// Fluent API methods
	// --------------------------------------------------------------------------------------------

	@Override
	public O withParameters(Configuration parameters) {
		this.parameters = parameters;

		@SuppressWarnings("unchecked")
		O returnType = (O) this;
		return returnType;
	}

	@Override
	public O withBroadcastSet(DataSet<?> data, String name) {
		if (this.broadcastVariables == null) {
			this.broadcastVariables = new HashMap<String, DataSet<?>>();
		}

		this.broadcastVariables.put(name, data);

		@SuppressWarnings("unchecked")
		O returnType = (O) this;
		return returnType;
	}

	/**
	 * Adds a constant-set annotation for the first input of the UDF.
	 * 
	 * <p>
	 * Constant set annotations are used by the optimizer to infer the existence of data properties (sorted, partitioned, grouped).
	 * In certain cases, these annotations allow the optimizer to generate a more efficient execution plan which can lead to improved performance.
	 * Constant set annotations can only be specified if the first input and the output type of the UDF are of
	 * {@link org.apache.flink.api.java.tuple.Tuple} data types.
	 * 
	 * <p>
	 * A constant-set annotation is a set of constant field specifications. The constant field specification String "4->3" specifies, that this UDF copies the fourth field of 
	 * an input tuple to the third field of the output tuple. Field references are zero-indexed.
	 * 
	 * <p>
	 * <b>NOTICE: Constant set annotations are optional, but if given need to be correct. Otherwise, the program might produce wrong results!</b>
	 * 
	 * @param constantSetFirst A list of constant field specification Strings for the first input.
	 * @return This operator with an annotated constant field set for the first input.
	 */
	@SuppressWarnings("unchecked")
	public O withConstantSetFirst(String... constantSetFirst) {
		if (this.udfSemantics == null) {
			this.udfSemantics = new DualInputSemanticProperties();
		}
		
		SemanticPropUtil.getSemanticPropsDualFromString(this.udfSemantics, constantSetFirst, null,
				null, null, null, null, this.getInput1Type(), this.getInput2Type(), this.getResultType());

		O returnType = (O) this;
		return returnType;
	}
	
	/**
	 * Adds a constant-set annotation for the second input of the UDF.
	 * 
	 * <p>
	 * Constant set annotations are used by the optimizer to infer the existence of data properties (sorted, partitioned, grouped).
	 * In certain cases, these annotations allow the optimizer to generate a more efficient execution plan which can lead to improved performance.
	 * Constant set annotations can only be specified if the second input and the output type of the UDF are of
	 * {@link org.apache.flink.api.java.tuple.Tuple} data types.
	 * 
	 * <p>
	 * A constant-set annotation is a set of constant field specifications. The constant field specification String "4->3" specifies, that this UDF copies the fourth field of 
	 * an input tuple to the third field of the output tuple. Field references are zero-indexed.
	 * 
	 * <p>
	 * <b>NOTICE: Constant set annotations are optional, but if given need to be correct. Otherwise, the program might produce wrong results!</b>
	 * 
	 * @param constantSetSecond A list of constant field specification Strings for the second input.
	 * @return This operator with an annotated constant field set for the second input.
	 */
	@SuppressWarnings("unchecked")
	public O withConstantSetSecond(String... constantSetSecond) {
		if (this.udfSemantics == null) {
			this.udfSemantics = new DualInputSemanticProperties();
		}
		
		SemanticPropUtil.getSemanticPropsDualFromString(this.udfSemantics, null, constantSetSecond,
				null, null, null, null, this.getInput1Type(), this.getInput2Type(), this.getResultType());

		O returnType = (O) this;
		return returnType;
	}

	// --------------------------------------------------------------------------------------------
	// Accessors
	// --------------------------------------------------------------------------------------------

	@Override
	public Map<String, DataSet<?>> getBroadcastSets() {
		return this.broadcastVariables == null ?
				Collections.<String, DataSet<?>>emptyMap() :
				Collections.unmodifiableMap(this.broadcastVariables);
	}

	@Override
	public Configuration getParameters() {
		return this.parameters;
	}

	@Override
	public DualInputSemanticProperties getSematicProperties() {
		return this.udfSemantics;
	}

	/**
	 * Sets the semantic properties for the user-defined function (UDF). The semantic properties
	 * define how fields of tuples and other objects are modified or preserved through this UDF.
	 * The configured properties can be retrieved via {@link UdfOperator#getSematicProperties()}.
	 *
	 * @param properties The semantic properties for the UDF.
	 * @see UdfOperator#getSematicProperties()
	 */
	public void setSemanticProperties(DualInputSemanticProperties properties) {
		this.udfSemantics = properties;
	}
}
