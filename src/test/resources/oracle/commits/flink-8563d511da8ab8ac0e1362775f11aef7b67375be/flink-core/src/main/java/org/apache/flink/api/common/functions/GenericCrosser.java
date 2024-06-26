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

package org.apache.flink.api.common.functions;

import org.apache.flink.util.Collector;


/**
 * @param <V1> First input type
 * @param <V2> Second input type
 * @param <O> Output type
 */
public interface GenericCrosser<V1, V2, O> extends Function {

	/**
	 * User defined function for the cross operator.
	 * 
	 * @param record1 Record from first input
	 * @param record2 Record from the second input
	 * @param out Collector to submit resulting records.
	 * @throws Exception
	 */
	void cross(V1 record1, V2 record2, Collector<O> out) throws Exception;
}
