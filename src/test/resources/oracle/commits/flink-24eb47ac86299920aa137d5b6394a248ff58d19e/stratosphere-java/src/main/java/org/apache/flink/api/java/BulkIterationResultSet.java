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
package org.apache.flink.api.java;

import org.apache.flink.types.TypeInformation;

public class BulkIterationResultSet<T> extends DataSet<T> {

	private final IterativeDataSet<T> iterationHead;

	private final DataSet<T> nextPartialSolution;

	private final DataSet<?> terminationCriterion;

	BulkIterationResultSet(ExecutionEnvironment context,
						TypeInformation<T> type,
						IterativeDataSet<T> iterationHead,
						DataSet<T> nextPartialSolution) {
		this(context, type, iterationHead, nextPartialSolution, null);
	}

	BulkIterationResultSet(ExecutionEnvironment context,
		TypeInformation<T> type, IterativeDataSet<T> iterationHead,
		DataSet<T> nextPartialSolution, DataSet<?> terminationCriterion)
	{
		super(context, type);
		this.iterationHead = iterationHead;
		this.nextPartialSolution = nextPartialSolution;
		this.terminationCriterion = terminationCriterion;
	}

	public IterativeDataSet<T> getIterationHead() {
		return iterationHead;
	}

	public DataSet<T> getNextPartialSolution() {
		return nextPartialSolution;
	}

	public DataSet<?> getTerminationCriterion() {
		return terminationCriterion;
	}
}
