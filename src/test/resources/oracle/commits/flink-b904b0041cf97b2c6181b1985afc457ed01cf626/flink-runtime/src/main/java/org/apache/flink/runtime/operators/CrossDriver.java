/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.flink.runtime.operators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.flink.api.common.functions.CrossFunction;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.runtime.memorymanager.MemoryManager;
import org.apache.flink.runtime.operators.resettable.BlockResettableMutableObjectIterator;
import org.apache.flink.runtime.operators.resettable.SpillingResettableMutableObjectIterator;
import org.apache.flink.runtime.operators.util.TaskConfig;
import org.apache.flink.util.Collector;
import org.apache.flink.util.MutableObjectIterator;

/**
 * Cross task which is executed by a Nephele task manager. The task has two
 * inputs and one or multiple outputs. It is provided with a CrossFunction
 * implementation.
 * <p>
 * The CrossTask builds the Cartesian product of the pairs of its two inputs. Each element (pair of pairs) is handed to
 * the <code>cross()</code> method of the CrossFunction.
 * 
 * @see org.apache.flink.api.common.functions.CrossFunction
 */
public class CrossDriver<T1, T2, OT> implements PactDriver<CrossFunction<T1, T2, OT>, OT> {
	
	private static final Logger LOG = LoggerFactory.getLogger(CrossDriver.class);
	
	
	private PactTaskContext<CrossFunction<T1, T2, OT>, OT> taskContext;
	
	private MemoryManager memManager;
	
	private SpillingResettableMutableObjectIterator<?> spillIter;
	
	private BlockResettableMutableObjectIterator<?> blockIter;
	
	private int  memPagesForBlockSide;
	
	private int memPagesForSpillingSide;

	private boolean blocked;
	
	private boolean firstIsOuter;
	
	private volatile boolean running;
	
	// ------------------------------------------------------------------------


	@Override
	public void setup(PactTaskContext<CrossFunction<T1, T2, OT>, OT> context) {
		this.taskContext = context;
		this.running = true;
	}


	@Override
	public int getNumberOfInputs() {
		return 2;
	}


	@Override
	public Class<CrossFunction<T1, T2, OT>> getStubType() {
		@SuppressWarnings("unchecked")
		final Class<CrossFunction<T1, T2, OT>> clazz = (Class<CrossFunction<T1, T2, OT>>) (Class<?>) CrossFunction.class;
		return clazz;
	}
	

	@Override
	public boolean requiresComparatorOnInput() {
		return false;
	}


	@Override
	public void prepare() throws Exception {
		final TaskConfig config = this.taskContext.getTaskConfig();
		final DriverStrategy ls = config.getDriverStrategy();
		
		switch (ls)
		{
		case NESTEDLOOP_BLOCKED_OUTER_FIRST:
			this.blocked = true;
			this.firstIsOuter = true;
			break;
		case NESTEDLOOP_BLOCKED_OUTER_SECOND:
			this.blocked = true;
			this.firstIsOuter = false;
			break;
		case NESTEDLOOP_STREAMED_OUTER_FIRST:
			this.blocked = false;
			this.firstIsOuter = true;
			break;
		case NESTEDLOOP_STREAMED_OUTER_SECOND:
			this.blocked = false;
			this.firstIsOuter = false;
			break;
		default:
			throw new RuntimeException("Invalid local strategy for CROSS: " + ls);
		}
		
		this.memManager = this.taskContext.getMemoryManager();
		final int numPages = this.memManager.computeNumberOfPages(config.getRelativeMemoryDriver());
		
		if (numPages < 2) {
			throw new RuntimeException(	"The Cross task was initialized with too little memory. " +
					"Cross requires at least 2 memory pages.");
		}
		
		// divide memory between spilling and blocking side
		if (ls == DriverStrategy.NESTEDLOOP_STREAMED_OUTER_FIRST || ls == DriverStrategy.NESTEDLOOP_STREAMED_OUTER_SECOND) {
			this.memPagesForSpillingSide = numPages;
			this.memPagesForBlockSide = 0;
		} else {
			if (numPages > 32) {
				this.memPagesForSpillingSide = 2;
			} else {
				this.memPagesForSpillingSide =  1;
			}
			this.memPagesForBlockSide = numPages - this.memPagesForSpillingSide;
		}
	}


	@Override
	public void run() throws Exception {
		if (this.blocked) {
			if (this.firstIsOuter) {
				runBlockedOuterFirst();
			} else {
				runBlockedOuterSecond();
			}
		} else {
			if (this.firstIsOuter) {
				runStreamedOuterFirst();
			} else {
				runStreamedOuterSecond();
			}
		}
	}


	@Override
	public void cleanup() throws Exception {
		if (this.spillIter != null) {
			this.spillIter.close();
			this.spillIter = null;
		}
		if (this.blockIter != null) {
			this.blockIter.close();
			this.blockIter = null;
		}
	}
	

	@Override
	public void cancel() {
		this.running = false;
	}

	private void runBlockedOuterFirst() throws Exception {
		if (LOG.isDebugEnabled())  {
			LOG.debug(this.taskContext.formatLogString("Running Cross with Block-Nested-Loops: " +
					"First input is outer (blocking) side, second input is inner (spilling) side."));
		}
			
		final MutableObjectIterator<T1> in1 = this.taskContext.getInput(0);
		final MutableObjectIterator<T2> in2 = this.taskContext.getInput(1);
		
		final TypeSerializer<T1> serializer1 = this.taskContext.<T1>getInputSerializer(0).getSerializer();
		final TypeSerializer<T2> serializer2 = this.taskContext.<T2>getInputSerializer(1).getSerializer();
		
		final BlockResettableMutableObjectIterator<T1> blockVals = 
				new BlockResettableMutableObjectIterator<T1>(this.memManager, in1, serializer1, this.memPagesForBlockSide,
							this.taskContext.getOwningNepheleTask());
		this.blockIter = blockVals;
		
		final SpillingResettableMutableObjectIterator<T2> spillVals = new SpillingResettableMutableObjectIterator<T2>(
				in2, serializer2, this.memManager, this.taskContext.getIOManager(), this.memPagesForSpillingSide,
				this.taskContext.getOwningNepheleTask());
		this.spillIter = spillVals;
		
		T1 val1;
		final T1 val1Reuse = serializer1.createInstance();
		T2 val2;
		final T2 val2Reuse = serializer2.createInstance();
		T2 val2Copy = serializer2.createInstance();
		
		final CrossFunction<T1, T2, OT> crosser = this.taskContext.getStub();
		final Collector<OT> collector = this.taskContext.getOutputCollector();
		
		// for all blocks
		do {
			// for all values from the spilling side
			while (this.running && ((val2 = spillVals.next(val2Reuse)) != null)) {
				// for all values in the block
				while ((val1 = blockVals.next(val1Reuse)) != null) {
					val2Copy = serializer2.copy(val2, val2Copy);
					collector.collect(crosser.cross(val1,val2Copy));
					//crosser.cross(val1, val2Copy, collector);
				}
				blockVals.reset();
			}
			spillVals.reset();
		}
		while (this.running && blockVals.nextBlock());
	}
	
	private void runBlockedOuterSecond() throws Exception {
		if (LOG.isDebugEnabled())  {
			LOG.debug(this.taskContext.formatLogString("Running Cross with Block-Nested-Loops: " +
					"First input is inner (spilling) side, second input is outer (blocking) side."));
		}
		
		final MutableObjectIterator<T1> in1 = this.taskContext.getInput(0);
		final MutableObjectIterator<T2> in2 = this.taskContext.getInput(1);
		
		final TypeSerializer<T1> serializer1 = this.taskContext.<T1>getInputSerializer(0).getSerializer();
		final TypeSerializer<T2> serializer2 = this.taskContext.<T2>getInputSerializer(1).getSerializer();
		
		final SpillingResettableMutableObjectIterator<T1> spillVals = new SpillingResettableMutableObjectIterator<T1>(
				in1, serializer1, this.memManager, this.taskContext.getIOManager(), this.memPagesForSpillingSide,
				this.taskContext.getOwningNepheleTask());
		this.spillIter = spillVals;
		
		final BlockResettableMutableObjectIterator<T2> blockVals = 
				new BlockResettableMutableObjectIterator<T2>(this.memManager, in2, serializer2, this.memPagesForBlockSide,
						this.taskContext.getOwningNepheleTask());
		this.blockIter = blockVals;
		
		T1 val1;
		final T1 val1Reuse = serializer1.createInstance();
		T1 val1Copy = serializer1.createInstance();
		T2 val2;
		final T2 val2Reuse = serializer2.createInstance();

		final CrossFunction<T1, T2, OT> crosser = this.taskContext.getStub();
		final Collector<OT> collector = this.taskContext.getOutputCollector();
		
		// for all blocks
		do {
			// for all values from the spilling side
			while (this.running && ((val1 = spillVals.next(val1Reuse)) != null)) {
				// for all values in the block
				while (this.running && ((val2 = blockVals.next(val2Reuse)) != null)) {
					val1Copy = serializer1.copy(val1, val1Copy);
					collector.collect(crosser.cross(val1Copy, val2));
					//crosser.cross(val1Copy, val2, collector);
				}
				blockVals.reset();
			}
			spillVals.reset();
		}
		while (this.running && blockVals.nextBlock());
	}
	
	private void runStreamedOuterFirst() throws Exception {
		if (LOG.isDebugEnabled())  {
			LOG.debug(this.taskContext.formatLogString("Running Cross with Nested-Loops: " +
					"First input is outer side, second input is inner (spilling) side."));
		}
		
		final MutableObjectIterator<T1> in1 = this.taskContext.getInput(0);
		final MutableObjectIterator<T2> in2 = this.taskContext.getInput(1);
		
		final TypeSerializer<T1> serializer1 = this.taskContext.<T1>getInputSerializer(0).getSerializer();
		final TypeSerializer<T2> serializer2 = this.taskContext.<T2>getInputSerializer(1).getSerializer();
		
		final SpillingResettableMutableObjectIterator<T2> spillVals = new SpillingResettableMutableObjectIterator<T2>(
				in2, serializer2, this.memManager, this.taskContext.getIOManager(), this.memPagesForSpillingSide,
				this.taskContext.getOwningNepheleTask());
		this.spillIter = spillVals;
		
		T1 val1;
		final T1 val1Reuse = serializer1.createInstance();
		T1 val1Copy = serializer1.createInstance();
		T2 val2;
		final T2 val2Reuse = serializer2.createInstance();

		final CrossFunction<T1, T2, OT> crosser = this.taskContext.getStub();
		final Collector<OT> collector = this.taskContext.getOutputCollector();
		
		// for all blocks
		while (this.running && ((val1 = in1.next(val1Reuse)) != null)) {
			// for all values from the spilling side
			while (this.running && ((val2 = spillVals.next(val2Reuse)) != null)) {
				val1Copy = serializer1.copy(val1, val1Copy);
				collector.collect(crosser.cross(val1Copy, val2));
				//crosser.cross(val1Copy, val2, collector);
			}
			spillVals.reset();
		}
	}
	
	private void runStreamedOuterSecond() throws Exception {
		if (LOG.isDebugEnabled())  {
			LOG.debug(this.taskContext.formatLogString("Running Cross with Nested-Loops: " +
					"First input is inner (spilling) side, second input is outer side."));
		}
		final MutableObjectIterator<T1> in1 = this.taskContext.getInput(0);
		final MutableObjectIterator<T2> in2 = this.taskContext.getInput(1);
		
		final TypeSerializer<T1> serializer1 = this.taskContext.<T1>getInputSerializer(0).getSerializer();
		final TypeSerializer<T2> serializer2 = this.taskContext.<T2>getInputSerializer(1).getSerializer();
		
		final SpillingResettableMutableObjectIterator<T1> spillVals = new SpillingResettableMutableObjectIterator<T1>(
				in1, serializer1, this.memManager, this.taskContext.getIOManager(), this.memPagesForSpillingSide,
				this.taskContext.getOwningNepheleTask());
		this.spillIter = spillVals;
		
		T1 val1;
		final T1 val1Reuse = serializer1.createInstance();
		T2 val2;
		final T2 val2Reuse = serializer2.createInstance();
		T2 val2Copy = serializer2.createInstance();
		
		final CrossFunction<T1, T2, OT> crosser = this.taskContext.getStub();
		final Collector<OT> collector = this.taskContext.getOutputCollector();
		
		// for all blocks
		while (this.running && (val2 = in2.next(val2Reuse)) != null) {
			// for all values from the spilling side
			while (this.running && (val1 = spillVals.next(val1Reuse)) != null) {
				val2Copy = serializer2.copy(val2, val2Copy);
				collector.collect(crosser.cross(val1, val2Copy));
				//crosser.cross(val1, val2Copy, collector);
			}
			spillVals.reset();
		}
	}
}
