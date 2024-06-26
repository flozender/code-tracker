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

package eu.stratosphere.pact.runtime.task.chaining;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.flink.api.common.functions.GenericCollectorMap;
import org.apache.flink.api.common.operators.util.UserCodeClassWrapper;
import org.apache.flink.api.java.record.functions.ReduceFunction;
import org.apache.flink.api.java.typeutils.runtime.record.RecordComparatorFactory;
import org.apache.flink.api.java.typeutils.runtime.record.RecordSerializerFactory;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.IntValue;
import org.apache.flink.types.Record;
import org.apache.flink.util.Collector;
import org.apache.flink.util.LogUtils;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;

import eu.stratosphere.pact.runtime.shipping.ShipStrategyType;
import eu.stratosphere.pact.runtime.task.CollectorMapDriver;
import eu.stratosphere.pact.runtime.task.DriverStrategy;
import eu.stratosphere.pact.runtime.task.MapTaskTest.MockMapStub;
import eu.stratosphere.pact.runtime.task.ReduceTaskTest.MockReduceStub;
import eu.stratosphere.pact.runtime.task.RegularPactTask;
import eu.stratosphere.pact.runtime.task.util.TaskConfig;
import eu.stratosphere.pact.runtime.test.util.TaskTestBase;
import eu.stratosphere.pact.runtime.test.util.UniformRecordGenerator;


public class ChainTaskTest extends TaskTestBase {
	
	private static final int MEMORY_MANAGER_SIZE = 1024 * 1024 * 3;

	private static final int NETWORK_BUFFER_SIZE = 1024;
	
	private final List<Record> outList = new ArrayList<Record>();
	
	@SuppressWarnings("unchecked")
	private final RecordComparatorFactory compFact = new RecordComparatorFactory(new int[]{0}, new Class[]{IntValue.class}, new boolean[] {true});
	private final RecordSerializerFactory serFact = RecordSerializerFactory.get();
	
	
	
	public ChainTaskTest() {
		// suppress log output, as this class produces errors on purpose to test exception handling
		LogUtils.initializeDefaultConsoleLogger(Level.OFF);
	}
	
	
	
	@Test
	public void testMapTask() {
		final int keyCnt = 100;
		final int valCnt = 20;

		final double memoryFraction = 1.0;
		
		try {
		
			// environment
			initEnvironment(MEMORY_MANAGER_SIZE, NETWORK_BUFFER_SIZE);
			addInput(new UniformRecordGenerator(keyCnt, valCnt, false), 0);
			addOutput(this.outList);
			
			// chained combine config
			{
				final TaskConfig combineConfig = new TaskConfig(new Configuration());
	
				// input
				combineConfig.addInputToGroup(0);
				combineConfig.setInputSerializer(serFact, 0);
				
				// output
				combineConfig.addOutputShipStrategy(ShipStrategyType.FORWARD);
				combineConfig.setOutputSerializer(serFact);
				
				// driver
				combineConfig.setDriverStrategy(DriverStrategy.SORTED_GROUP_COMBINE);
				combineConfig.setDriverComparator(compFact, 0);
				combineConfig.setRelativeMemoryDriver(memoryFraction);
				
				// udf
				combineConfig.setStubWrapper(new UserCodeClassWrapper<MockReduceStub>(MockReduceStub.class));
				
				getTaskConfig().addChainedTask(SynchronousChainedCombineDriver.class, combineConfig, "combine");
			}
			
			// chained map+combine
			{
				RegularPactTask<GenericCollectorMap<Record, Record>, Record> testTask = 
											new RegularPactTask<GenericCollectorMap<Record, Record>, Record>();
				registerTask(testTask, CollectorMapDriver.class, MockMapStub.class);
				
				try {
					testTask.invoke();
				} catch (Exception e) {
					e.printStackTrace();
					Assert.fail("Invoke method caused exception.");
				}
			}
			
			Assert.assertEquals(keyCnt, this.outList.size());
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testFailingMapTask() {
		int keyCnt = 100;
		int valCnt = 20;

		final long memorySize = 1024 * 1024 * 3;
		final int bufferSize = 1014*1024;
		final double memoryFraction = 1.0;
		
		try {
			// environment
			initEnvironment(memorySize, bufferSize);
			addInput(new UniformRecordGenerator(keyCnt, valCnt, false), 0);
			addOutput(this.outList);
	
			// chained combine config
			{
				final TaskConfig combineConfig = new TaskConfig(new Configuration());
	
				// input
				combineConfig.addInputToGroup(0);
				combineConfig.setInputSerializer(serFact, 0);
				
				// output
				combineConfig.addOutputShipStrategy(ShipStrategyType.FORWARD);
				combineConfig.setOutputSerializer(serFact);
				
				// driver
				combineConfig.setDriverStrategy(DriverStrategy.SORTED_GROUP_COMBINE);
				combineConfig.setDriverComparator(compFact, 0);
				combineConfig.setRelativeMemoryDriver(memoryFraction);
				
				// udf
				combineConfig.setStubWrapper(new UserCodeClassWrapper<MockFailingCombineStub>(MockFailingCombineStub.class));
				
				getTaskConfig().addChainedTask(SynchronousChainedCombineDriver.class, combineConfig, "combine");
			}
			
			// chained map+combine
			{
				final RegularPactTask<GenericCollectorMap<Record, Record>, Record> testTask = 
											new RegularPactTask<GenericCollectorMap<Record, Record>, Record>();
				
				super.registerTask(testTask, CollectorMapDriver.class, MockMapStub.class);
	
				boolean stubFailed = false;
				
				try {
					testTask.invoke();
				} catch (Exception e) {
					stubFailed = true;
				}
				
				Assert.assertTrue("Function exception was not forwarded.", stubFailed);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	public static final class MockFailingCombineStub extends ReduceFunction {
		private static final long serialVersionUID = 1L;
		
		private int cnt = 0;

		@Override
		public void reduce(Iterator<Record> records, Collector<Record> out) throws Exception {
			if (++this.cnt >= 5) {
				throw new RuntimeException("Expected Test Exception");
			}
			while(records.hasNext()) {
				out.collect(records.next());
			}
		}
	}
}
