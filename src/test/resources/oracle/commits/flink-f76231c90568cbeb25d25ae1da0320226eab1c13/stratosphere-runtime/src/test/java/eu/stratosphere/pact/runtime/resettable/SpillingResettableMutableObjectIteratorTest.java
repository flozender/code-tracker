/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.pact.runtime.resettable;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.stratosphere.api.typeutils.TypeSerializer;
import eu.stratosphere.nephele.services.iomanager.IOManager;
import eu.stratosphere.nephele.services.memorymanager.MemoryManager;
import eu.stratosphere.nephele.services.memorymanager.spi.DefaultMemoryManager;
import eu.stratosphere.nephele.template.AbstractInvokable;
import eu.stratosphere.pact.runtime.plugable.pactrecord.PactRecordSerializer;
import eu.stratosphere.pact.runtime.test.util.DummyInvokable;
import eu.stratosphere.pact.runtime.test.util.MutableObjectIteratorWrapper;
import eu.stratosphere.types.PactInteger;
import eu.stratosphere.types.PactRecord;
import eu.stratosphere.util.MutableObjectIterator;
import junit.framework.Assert;

public class SpillingResettableMutableObjectIteratorTest
{
	private static final int NUM_TESTRECORDS = 50000;

	private static final int MEMORY_CAPACITY = 10 * 1024 * 1024;

	private IOManager ioman;

	private MemoryManager memman;

	private MutableObjectIterator<PactRecord> reader;

	private final TypeSerializer<PactRecord> serializer = PactRecordSerializer.get();


	@Before
	public void startup()
	{
		// set up IO and memory manager
		this.memman = new DefaultMemoryManager(MEMORY_CAPACITY);
		this.ioman = new IOManager();

		// create test objects
		final ArrayList<PactRecord> objects = new ArrayList<PactRecord>(NUM_TESTRECORDS);

		for (int i = 0; i < NUM_TESTRECORDS; ++i) {
			PactRecord tmp = new PactRecord(new PactInteger(i));
			objects.add(tmp);
		}
		this.reader = new MutableObjectIteratorWrapper(objects.iterator());
	}

	@After
	public void shutdown()
	{
		this.ioman.shutdown();
		if (!this.ioman.isProperlyShutDown()) {
			Assert.fail("I/O Manager Shutdown was not completed properly.");
		}
		this.ioman = null;

		if (!this.memman.verifyEmpty()) {
			Assert.fail("A memory leak has occurred: Not all memory was properly returned to the memory manager.");
		}
		this.memman.shutdown();
		this.memman = null;
	}

	/**
	 * Tests the resettable iterator with too little memory, so that the data
	 * has to be written to disk.
	 */
	@Test
	public void testResettableIterator()
	{
		try {
			final AbstractInvokable memOwner = new DummyInvokable();
	
			// create the resettable Iterator
			SpillingResettableMutableObjectIterator<PactRecord> iterator = new SpillingResettableMutableObjectIterator<PactRecord>(
				this.reader, this.serializer, this.memman, this.ioman, 2 * 32 * 1024, memOwner);
	
			// open the iterator
			try {
				iterator.open();
			} catch (IOException e) {
				Assert.fail("Could not open resettable iterator:" + e.getMessage());
			}
			// now test walking through the iterator
			int count = 0;
			PactRecord target = new PactRecord();
			while (iterator.next(target))
				Assert.assertEquals("In initial run, element " + count + " does not match expected value!", count++,
					target.getField(0, PactInteger.class).getValue());
			Assert.assertEquals("Too few elements were deserialzied in initial run!", NUM_TESTRECORDS, count);
			// test resetting the iterator a few times
			for (int j = 0; j < 10; ++j) {
				count = 0;
				iterator.reset();
				// now we should get the same results
				while (iterator.next(target))
					Assert.assertEquals("After reset nr. " + j + 1 + " element " + count
						+ " does not match expected value!", count++, target.getField(0, PactInteger.class).getValue());
				Assert.assertEquals("Too few elements were deserialzied after reset nr. " + j + 1 + "!", NUM_TESTRECORDS,
					count);
			}
			// close the iterator
			iterator.close();
		} catch (Exception ex)  {
			ex.printStackTrace();
			Assert.fail("Test encountered an exception.");
		}
	}

	/**
	 * Tests the resettable iterator with enough memory so that all data is kept locally in memory.
	 */
	@Test
	public void testResettableIteratorInMemory()
	{
		try {
			final AbstractInvokable memOwner = new DummyInvokable();
	
			// create the resettable Iterator
			SpillingResettableMutableObjectIterator<PactRecord> iterator = new SpillingResettableMutableObjectIterator<PactRecord>(
				this.reader, this.serializer, this.memman, this.ioman, 20 * 32 * 1024, memOwner);
			// open the iterator
			try {
				iterator.open();
			} catch (IOException e) {
				Assert.fail("Could not open resettable iterator:" + e.getMessage());
			}
			// now test walking through the iterator
			int count = 0;
			PactRecord target = new PactRecord();
			while (iterator.next(target))
				Assert.assertEquals("In initial run, element " + count + " does not match expected value!", count++,
					target.getField(0, PactInteger.class).getValue());
			Assert.assertEquals("Too few elements were deserialzied in initial run!", NUM_TESTRECORDS, count);
			// test resetting the iterator a few times
			for (int j = 0; j < 10; ++j) {
				count = 0;
				iterator.reset();
				// now we should get the same results
				while (iterator.next(target))
					Assert.assertEquals("After reset nr. " + j + 1 + " element " + count
						+ " does not match expected value!", count++, target.getField(0, PactInteger.class).getValue());
				Assert.assertEquals("Too few elements were deserialzied after reset nr. " + j + 1 + "!", NUM_TESTRECORDS,
					count);
			}
			// close the iterator
			iterator.close();
		} catch (Exception ex)  {
			ex.printStackTrace();
			Assert.fail("Test encountered an exception.");
		}
	}
}