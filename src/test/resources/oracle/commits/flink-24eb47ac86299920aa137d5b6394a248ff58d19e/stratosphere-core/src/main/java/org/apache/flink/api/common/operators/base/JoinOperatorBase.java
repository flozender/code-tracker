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

package org.apache.flink.api.common.operators.base;

import org.apache.flink.api.common.functions.GenericJoiner;
import org.apache.flink.api.common.operators.BinaryOperatorInformation;
import org.apache.flink.api.common.operators.DualInputOperator;
import org.apache.flink.api.common.operators.util.UserCodeClassWrapper;
import org.apache.flink.api.common.operators.util.UserCodeObjectWrapper;
import org.apache.flink.api.common.operators.util.UserCodeWrapper;

/**
 * @see GenericJoiner
 */
public class JoinOperatorBase<IN1, IN2, OUT, FT extends GenericJoiner<IN1, IN2, OUT>> extends DualInputOperator<IN1, IN2, OUT, FT>
{
	public JoinOperatorBase(UserCodeWrapper<FT> udf, BinaryOperatorInformation<IN1, IN2, OUT> operatorInfo, int[] keyPositions1, int[] keyPositions2, String name) {
		super(udf, operatorInfo, keyPositions1, keyPositions2, name);
	}
	
	public JoinOperatorBase(FT udf, BinaryOperatorInformation<IN1, IN2, OUT> operatorInfo, int[] keyPositions1, int[] keyPositions2, String name) {
		super(new UserCodeObjectWrapper<FT>(udf), operatorInfo, keyPositions1, keyPositions2, name);
	}
	
	public JoinOperatorBase(Class<? extends FT> udf, BinaryOperatorInformation<IN1, IN2, OUT> operatorInfo, int[] keyPositions1, int[] keyPositions2, String name) {
		super(new UserCodeClassWrapper<FT>(udf), operatorInfo, keyPositions1, keyPositions2, name);
	}
}
