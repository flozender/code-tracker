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

package org.apache.flink.runtime.operators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.flink.api.common.functions.GenericCoGrouper;
import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.api.common.typeutils.TypePairComparatorFactory;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.runtime.operators.sort.SortMergeCoGroupIterator;
import org.apache.flink.runtime.operators.util.CoGroupTaskIterator;
import org.apache.flink.runtime.operators.util.TaskConfig;
import org.apache.flink.util.Collector;
import org.apache.flink.util.MutableObjectIterator;

/**
 * CoGroup task which is executed by a Nephele task manager. The task has two
 * inputs and one or multiple outputs. It is provided with a CoGroupFunction
 * implementation.
 * <p>
 * The CoGroupTask group all pairs that share the same key from both inputs. Each for each key, the sets of values that
 * were pair with that key of both inputs are handed to the <code>coGroup()</code> method of the CoGroupFunction.
 * 
 * @see org.apache.flink.api.java.record.functions.CoGroupFunction
 */
public class CoGroupDriver<IT1, IT2, OT> implements PactDriver<GenericCoGrouper<IT1, IT2, OT>, OT> {
	
	private static final Log LOG = LogFactory.getLog(CoGroupDriver.class);
	
	
	private PactTaskContext<GenericCoGrouper<IT1, IT2, OT>, OT> taskContext;
	
	private CoGroupTaskIterator<IT1, IT2> coGroupIterator;				// the iterator that does the actual cogroup
	
	private volatile boolean running;

	// ------------------------------------------------------------------------


	@Override
	public void setup(PactTaskContext<GenericCoGrouper<IT1, IT2, OT>, OT> context) {
		this.taskContext = context;
		this.running = true;
	}
	

	@Override
	public int getNumberOfInputs() {
		return 2;
	}
	

	@Override
	public Class<GenericCoGrouper<IT1, IT2, OT>> getStubType() {
		@SuppressWarnings("unchecked")
		final Class<GenericCoGrouper<IT1, IT2, OT>> clazz = (Class<GenericCoGrouper<IT1, IT2, OT>>) (Class<?>) GenericCoGrouper.class;
		return clazz;
	}
	

	@Override
	public boolean requiresComparatorOnInput() {
		return true;
	}
	

	@Override
	public void prepare() throws Exception
	{
		final TaskConfig config = this.taskContext.getTaskConfig();
		if (config.getDriverStrategy() != DriverStrategy.CO_GROUP) {
			throw new Exception("Unrecognized driver strategy for CoGoup driver: " + config.getDriverStrategy().name());
		}
		
		final MutableObjectIterator<IT1> in1 = this.taskContext.getInput(0);
		final MutableObjectIterator<IT2> in2 = this.taskContext.getInput(1);
		
		// get the key positions and types
		final TypeSerializer<IT1> serializer1 = this.taskContext.<IT1>getInputSerializer(0).getSerializer();
		final TypeSerializer<IT2> serializer2 = this.taskContext.<IT2>getInputSerializer(1).getSerializer();
		final TypeComparator<IT1> groupComparator1 = this.taskContext.getInputComparator(0);
		final TypeComparator<IT2> groupComparator2 = this.taskContext.getInputComparator(1);
		
		final TypePairComparatorFactory<IT1, IT2> pairComparatorFactory = config.getPairComparatorFactory(
					this.taskContext.getUserCodeClassLoader());
		if (pairComparatorFactory == null) {
			throw new Exception("Missing pair comparator factory for CoGroup driver");
		}

		// create CoGropuTaskIterator according to provided local strategy.
		this.coGroupIterator = new SortMergeCoGroupIterator<IT1, IT2>(in1, in2,
				serializer1, groupComparator1,  serializer2, groupComparator2,
				pairComparatorFactory.createComparator12(groupComparator1, groupComparator2));
		
		// open CoGroupTaskIterator - this triggers the sorting and blocks until the iterator is ready
		this.coGroupIterator.open();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.taskContext.formatLogString("CoGroup task iterator ready."));
		}
	}
	

	@Override
	public void run() throws Exception
	{
		final GenericCoGrouper<IT1, IT2, OT> coGroupStub = this.taskContext.getStub();
		final Collector<OT> collector = this.taskContext.getOutputCollector();
		final CoGroupTaskIterator<IT1, IT2> coGroupIterator = this.coGroupIterator;
		
		while (this.running && coGroupIterator.next()) {
			coGroupStub.coGroup(coGroupIterator.getValues1(), coGroupIterator.getValues2(), collector);
		}
	}


	@Override
	public void cleanup() throws Exception {
		if (this.coGroupIterator != null) {
			this.coGroupIterator.close();
			this.coGroupIterator = null;
		}
	}


	@Override
	public void cancel() throws Exception {
		this.running = false;
		cleanup();
	}
}
