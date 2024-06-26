/***********************************************************************************************************************
 *
 * Copyright (C) 2010-2013 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.pact.generic.contract;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import eu.stratosphere.api.functions.Stub;
import eu.stratosphere.api.operators.GenericDataSink;
import eu.stratosphere.api.operators.GenericDataSource;
import eu.stratosphere.api.operators.base.GenericCoGroupContract;
import eu.stratosphere.api.operators.base.GenericCrossContract;
import eu.stratosphere.api.operators.base.GenericMapContract;
import eu.stratosphere.api.operators.base.GenericMatchContract;
import eu.stratosphere.api.operators.base.GenericReduceContract;
import eu.stratosphere.api.operators.util.ContractUtil;
import eu.stratosphere.pact.common.io.DelimitedInputFormat;
import eu.stratosphere.pact.common.io.DelimitedOutputFormat;
import eu.stratosphere.pact.common.stubs.CoGroupStub;
import eu.stratosphere.pact.common.stubs.CrossStub;
import eu.stratosphere.pact.common.stubs.MapStub;
import eu.stratosphere.pact.common.stubs.MatchStub;
import eu.stratosphere.pact.common.stubs.ReduceStub;
import eu.stratosphere.types.PactRecord;
import eu.stratosphere.util.Collector;

/**
 * Tests {@link ContractUtil}.
 * 
 * @author Arvid Heise
 */
public class ContractUtilTest {
	/**
	 * Test {@link ContractUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnCoGroupForCoGroupStub() {
		final Class<?> result = ContractUtil.getContractClass(CoGrouper.class);
		assertEquals(GenericCoGroupContract.class, result);
	}

	/**
	 * Test {@link ContractUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnCrossForCrossStub() {
		final Class<?> result = ContractUtil.getContractClass(Crosser.class);
		assertEquals(GenericCrossContract.class, result);
	}

	/**
	 * Test {@link ContractUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnMapForMapStub() {
		final Class<?> result = ContractUtil.getContractClass(Mapper.class);
		assertEquals(GenericMapContract.class, result);
	}

	/**
	 * Test {@link ContractUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnMatchForMatchStub() {
		final Class<?> result = ContractUtil.getContractClass(Matcher.class);
		assertEquals(GenericMatchContract.class, result);
	}

	/**
	 * Test {@link ContractUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnNullForStub() {
		final Class<?> result = ContractUtil.getContractClass(Stub.class);
		assertEquals(null, result);
	}

	/**
	 * Test {@link ContractUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnReduceForReduceStub() {
		final Class<?> result = ContractUtil.getContractClass(Reducer.class);
		assertEquals(GenericReduceContract.class, result);
	}

	/**
	 * Test {@link ContractUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnSinkForOutputFormat() {
		final Class<?> result = ContractUtil.getContractClass(DelimitedOutputFormat.class);
		assertEquals(GenericDataSink.class, result);
	}

	/**
	 * Test {@link ContractUtil#getContractClass(Class)}
	 */
	@Test
	public void getContractClassShouldReturnSourceForInputFormat() {
		final Class<?> result = ContractUtil.getContractClass(DelimitedInputFormat.class);
		assertEquals(GenericDataSource.class, result);
	}

	static class CoGrouper extends CoGroupStub {
		/* (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.CoGroupStub#coGroup(java.util.Iterator, java.util.Iterator, eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void coGroup(Iterator<PactRecord> records1, Iterator<PactRecord> records2, Collector<PactRecord> out) {
		}
	}

	static class Crosser extends CrossStub {
		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.CrossStub#cross(eu.stratosphere.pact.common.type.PactRecord,
		 * eu.stratosphere.pact.common.type.PactRecord, eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void cross(PactRecord record1, PactRecord record2, Collector<PactRecord> out) {
		}
	}

	static class Mapper extends MapStub {
		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.MapStub#map(eu.stratosphere.pact.common.type.PactRecord,
		 * eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void map(PactRecord record, Collector<PactRecord> out) throws Exception {
		}
	}

	static class Matcher extends MatchStub {
		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.MatchStub#match(eu.stratosphere.pact.common.type.PactRecord,
		 * eu.stratosphere.pact.common.type.PactRecord, eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void match(PactRecord value1, PactRecord value2, Collector<PactRecord> out) throws Exception {
		}
	}

	static class Reducer extends ReduceStub {
		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stubs.ReduceStub#reduce(java.util.Iterator,
		 * eu.stratosphere.pact.common.stubs.Collector)
		 */
		@Override
		public void reduce(Iterator<PactRecord> records, Collector<PactRecord> out) throws Exception {
		}
	}
}
