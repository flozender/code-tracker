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

package org.apache.flink.api.java.record.operators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.flink.api.common.operators.Operator;
import org.apache.flink.api.common.operators.RecordOperator;
import org.apache.flink.api.common.operators.base.CollectorMapOperatorBase;
import org.apache.flink.api.common.operators.util.UserCodeClassWrapper;
import org.apache.flink.api.common.operators.util.UserCodeObjectWrapper;
import org.apache.flink.api.common.operators.util.UserCodeWrapper;
import org.apache.flink.api.java.record.functions.FunctionAnnotation;
import org.apache.flink.api.java.record.functions.MapFunction;
import org.apache.flink.types.Key;
import org.apache.flink.types.Record;

/**
 * MapOperator that applies a {@link MapFunction} to each record independently.
 * 
 * @see MapFunction
 */
public class MapOperator extends CollectorMapOperatorBase<Record, Record, MapFunction> implements RecordOperator {
	
	private static String DEFAULT_NAME = "<Unnamed Mapper>";
	
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Creates a Builder with the provided {@link MapFunction} implementation.
	 * 
	 * @param udf The {@link MapFunction} implementation for this Map operator.
	 */
	public static Builder builder(MapFunction udf) {
		return new Builder(new UserCodeObjectWrapper<MapFunction>(udf));
	}
	
	/**
	 * Creates a Builder with the provided {@link MapFunction} implementation.
	 * 
	 * @param udf The {@link MapFunction} implementation for this Map operator.
	 */
	public static Builder builder(Class<? extends MapFunction> udf) {
		return new Builder(new UserCodeClassWrapper<MapFunction>(udf));
	}
	
	/**
	 * The private constructor that only gets invoked from the Builder.
	 * @param builder
	 */
	protected MapOperator(Builder builder) {
		super(builder.udf, OperatorInfoHelper.unary(), builder.name);
		
		if (builder.inputs != null && !builder.inputs.isEmpty()) {
			setInput(Operator.createUnionCascade(builder.inputs));
		}
		
		setBroadcastVariables(builder.broadcastInputs);
		setSemanticProperties(FunctionAnnotation.readSingleConstantAnnotations(builder.udf));
	}
	

	@Override
	public Class<? extends Key<?>>[] getKeyClasses() {
		return emptyClassArray();
	}

	// --------------------------------------------------------------------------------------------
	
	/**
	 * Builder pattern, straight from Joshua Bloch's Effective Java (2nd Edition).
	 */
	public static class Builder {
		
		/* The required parameters */
		private final UserCodeWrapper<MapFunction> udf;
		
		/* The optional parameters */
		private List<Operator<Record>> inputs;
		private Map<String, Operator<Record>> broadcastInputs;
		private String name = DEFAULT_NAME;
		
		/**
		 * Creates a Builder with the provided {@link MapFunction} implementation.
		 * 
		 * @param udf The {@link MapFunction} implementation for this Map operator.
		 */
		private Builder(UserCodeWrapper<MapFunction> udf) {
			this.udf = udf;
			this.inputs = new ArrayList<Operator<Record>>();
			this.broadcastInputs = new HashMap<String, Operator<Record>>();
		}
		
		/**
		 * Sets the input.
		 * 
		 * @param input The input.
		 */
		public Builder input(Operator<Record> input) {
			Validate.notNull(input, "The input must not be null");
			
			this.inputs.clear();
			this.inputs.add(input);
			return this;
		}
		
		/**
		 * Sets one or several inputs (union).
		 * 
		 * @param inputs
		 */
		public Builder input(Operator<Record>...inputs) {
			this.inputs.clear();
			for (Operator<Record> c : inputs) {
				this.inputs.add(c);
			}
			return this;
		}
		
		/**
		 * Sets the inputs.
		 * 
		 * @param inputs
		 */
		public Builder inputs(List<Operator<Record>> inputs) {
			this.inputs = inputs;
			return this;
		}
		
		/**
		 * Binds the result produced by a plan rooted at {@code root} to a 
		 * variable used by the UDF wrapped in this operator.
		 */
		public Builder setBroadcastVariable(String name, Operator<Record> input) {
			this.broadcastInputs.put(name, input);
			return this;
		}
		
		/**
		 * Binds multiple broadcast variables.
		 */
		public Builder setBroadcastVariables(Map<String, Operator<Record>> inputs) {
			this.broadcastInputs.clear();
			this.broadcastInputs.putAll(inputs);
			return this;
		}
		
		/**
		 * Sets the name of this operator.
		 * 
		 * @param name
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		/**
		 * Creates and returns a MapOperator from using the values given 
		 * to the builder.
		 * 
		 * @return The created operator
		 */
		public MapOperator build() {
			if (name == null) {
				name = udf.getUserCodeClass().getName();
			}
			return new MapOperator(this);
		}
	}
}
