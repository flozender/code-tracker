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

package eu.stratosphere.nephele.jobmanager.accumulators;

import java.util.HashMap;
import java.util.Map;

import org.apache.flink.api.common.accumulators.Accumulator;
import org.apache.flink.api.common.accumulators.AccumulatorHelper;

/**
 * Simple class wrapping a map of accumulators for a single job. Just for better
 * handling.
 */
public class JobAccumulators {

	private final Map<String, Accumulator<?, ?>> accumulators = new HashMap<String, Accumulator<?, ?>>();

	public Map<String, Accumulator<?, ?>> getAccumulators() {
		return this.accumulators;
	}

	public void processNew(Map<String, Accumulator<?, ?>> newAccumulators) {
		AccumulatorHelper.mergeInto(this.accumulators, newAccumulators);
	}
}
