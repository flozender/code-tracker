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

package org.apache.flink.api.common.operators.util;

import static org.junit.Assert.assertEquals;

import org.apache.flink.api.common.functions.Function;
import org.apache.flink.api.common.functions.GenericCoGrouper;
import org.apache.flink.api.common.functions.GenericCollectorMap;
import org.apache.flink.api.common.functions.GenericCrosser;
import org.apache.flink.api.common.functions.GenericGroupReduce;
import org.apache.flink.api.common.functions.GenericJoiner;
import org.apache.flink.api.common.io.DelimitedInputFormat;
import org.apache.flink.api.common.io.FileOutputFormat;
import org.apache.flink.api.common.operators.base.CoGroupOperatorBase;
import org.apache.flink.api.common.operators.base.CollectorMapOperatorBase;
import org.apache.flink.api.common.operators.base.CrossOperatorBase;
import org.apache.flink.api.common.operators.base.GenericDataSinkBase;
import org.apache.flink.api.common.operators.base.GenericDataSourceBase;
import org.apache.flink.api.common.operators.base.GroupReduceOperatorBase;
import org.apache.flink.api.common.operators.base.JoinOperatorBase;
import org.apache.flink.api.common.operators.util.OperatorUtil;
import org.apache.flink.types.IntValue;
import org.junit.Test;

/**
 * Tests {@link OperatorUtil}.
 */
public class OperatorUtilTest {
	/**
	 * Test {@link OperatorUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnCoGroupForCoGroupStub() {
		final Class<?> result = OperatorUtil.getContractClass(CoGrouper.class);
		assertEquals(CoGroupOperatorBase.class, result);
	}

	/**
	 * Test {@link OperatorUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnCrossForCrossStub() {
		final Class<?> result = OperatorUtil.getContractClass(Crosser.class);
		assertEquals(CrossOperatorBase.class, result);
	}

	/**
	 * Test {@link OperatorUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnMapForMapStub() {
		final Class<?> result = OperatorUtil.getContractClass(Mapper.class);
		assertEquals(CollectorMapOperatorBase.class, result);
	}

	/**
	 * Test {@link OperatorUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnMatchForMatchStub() {
		final Class<?> result = OperatorUtil.getContractClass(Matcher.class);
		assertEquals(JoinOperatorBase.class, result);
	}

	/**
	 * Test {@link OperatorUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnNullForStub() {
		final Class<?> result = OperatorUtil.getContractClass(Function.class);
		assertEquals(null, result);
	}

	/**
	 * Test {@link OperatorUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnReduceForReduceStub() {
		final Class<?> result = OperatorUtil.getContractClass(Reducer.class);
		assertEquals(GroupReduceOperatorBase.class, result);
	}

	/**
	 * Test {@link OperatorUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnSinkForOutputFormat() {
		final Class<?> result = OperatorUtil.getContractClass(FileOutputFormat.class);
		assertEquals(GenericDataSinkBase.class, result);
	}

	/**
	 * Test {@link OperatorUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnSourceForInputFormat() {
		final Class<?> result = OperatorUtil.getContractClass(DelimitedInputFormat.class);
		assertEquals(GenericDataSourceBase.class, result);
	}

	static abstract class CoGrouper implements GenericCoGrouper<IntValue, IntValue, IntValue> {}

	static abstract class Crosser implements GenericCrosser<IntValue, IntValue, IntValue> {}

	static abstract class Mapper implements GenericCollectorMap<IntValue, IntValue> {}

	static abstract class Matcher implements GenericJoiner<IntValue, IntValue, IntValue> {}

	static abstract class Reducer implements GenericGroupReduce<IntValue, IntValue> {}
}
