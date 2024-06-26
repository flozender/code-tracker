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

package org.apache.flink.api.java.record.functions;

import org.apache.flink.api.common.functions.AbstractFunction;
import org.apache.flink.api.common.functions.GenericCrosser;
import org.apache.flink.types.Record;
import org.apache.flink.util.Collector;

/**
 * The CrossFunction is the base class for functions that are invoked by a {@link org.apache.flink.api.java.operators.CrossOperator}.
 */
public abstract class CrossFunction extends AbstractFunction implements GenericCrosser<Record, Record, Record> {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * This method must be implemented to provide a user implementation of a cross.
	 * It is called for each element of the Cartesian product of both input sets.

	 * @param record1 The record from the second input.
	 * @param record2 The record from the second input.
	 * @param out A collector that collects all output records.
	 * 
	 * @throws Exception Implementations may forward exceptions, which are caught by the runtime. When the
	 *                   runtime catches an exception, it aborts the task and lets the fail-over logic
	 *                   decide whether to retry the task execution.
	 */
	@Override
	public abstract void cross(Record record1, Record record2, Collector<Record> out) throws Exception;
}
