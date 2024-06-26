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

import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.api.common.operators.base.GenericDataSourceBase;
import org.apache.flink.types.Record;

/**
 * Abstract superclass for data sources in a Pact plan.
 *
 * @param <T> The type of input format invoked by instances of this data source.
 */
public class GenericDataSource<T extends InputFormat<Record, ?>> extends GenericDataSourceBase<Record, T> {

	/**
	 * Creates a new instance for the given file using the given input format.
	 * 
	 * @param format The {@link InputFormat} implementation used to read the data.
	 * @param name The given name for the Pact, used in plans, logs and progress messages.
	 */
	public GenericDataSource(T format,String name) {
		super(format, OperatorInfoHelper.source(), name);
	}
	
	/**
	 * Creates a new instance for the given file using the given input format, using the default name.
	 * 
	 * @param format The {@link InputFormat} implementation used to read the data.
	 */
	public GenericDataSource(T format) {
		super(format, OperatorInfoHelper.source());
	}
	
	/**
	 * Creates a new instance for the given file using the given input format.
	 * 
	 * @param format The {@link InputFormat} implementation used to read the data.
	 * @param name The given name for the Pact, used in plans, logs and progress messages.
	 */
	public GenericDataSource(Class<? extends T> format, String name) {
		super(format, OperatorInfoHelper.source(), name);
	}
	
	/**
	 * Creates a new instance for the given file using the given input format, using the default name.
	 * 
	 * @param format The {@link InputFormat} implementation used to read the data.
	 */
	public GenericDataSource(Class<? extends T> format) {
		super(format, OperatorInfoHelper.source());
	}
}
