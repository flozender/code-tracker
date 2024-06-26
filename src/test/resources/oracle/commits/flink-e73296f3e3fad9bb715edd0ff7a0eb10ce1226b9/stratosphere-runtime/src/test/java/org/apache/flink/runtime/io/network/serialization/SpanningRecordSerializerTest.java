/***********************************************************************************************************************
 * Copyright (C) 2010-2014 by the Apache Flink project (http://flink.incubator.apache.org)
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

package org.apache.flink.runtime.io.network.serialization;

import junit.framework.Assert;

import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.runtime.io.network.Buffer;
import org.apache.flink.runtime.io.network.serialization.SpanningRecordSerializer;
import org.apache.flink.runtime.io.network.serialization.RecordSerializer.SerializationResult;
import org.apache.flink.runtime.io.network.serialization.types.SerializationTestType;
import org.apache.flink.runtime.io.network.serialization.types.SerializationTestTypeFactory;
import org.apache.flink.runtime.io.network.serialization.types.Util;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

public class SpanningRecordSerializerTest {

	@Test
	public void testHasData() {
		final int SEGMENT_SIZE = 16;

		final SpanningRecordSerializer<SerializationTestType> serializer = new SpanningRecordSerializer<SerializationTestType>();
		final Buffer buffer = new Buffer(new MemorySegment(new byte[SEGMENT_SIZE]), SEGMENT_SIZE, null);
		final SerializationTestType randomIntRecord = Util.randomRecord(SerializationTestTypeFactory.INT);

		Assert.assertFalse(serializer.hasData());

		try {
			serializer.addRecord(randomIntRecord);
			Assert.assertTrue(serializer.hasData());

			serializer.setNextBuffer(buffer);
			Assert.assertTrue(serializer.hasData());

			serializer.clear();
			Assert.assertFalse(serializer.hasData());

			serializer.setNextBuffer(buffer);

			serializer.addRecord(randomIntRecord);
			Assert.assertTrue(serializer.hasData());

			serializer.addRecord(randomIntRecord);
			Assert.assertTrue(serializer.hasData());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testEmptyRecords() {
		final int SEGMENT_SIZE = 11;

		final SpanningRecordSerializer<SerializationTestType> serializer = new SpanningRecordSerializer<SerializationTestType>();
		final Buffer buffer = new Buffer(new MemorySegment(new byte[SEGMENT_SIZE]), SEGMENT_SIZE, null);

		try {
			Assert.assertEquals(SerializationResult.FULL_RECORD, serializer.setNextBuffer(buffer));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			SerializationTestType emptyRecord = new SerializationTestType() {
				@Override
				public SerializationTestType getRandom(Random rnd) {
					throw new UnsupportedOperationException();
				}

				@Override
				public int length() {
					throw new UnsupportedOperationException();
				}

				@Override
				public void write(DataOutputView out) throws IOException {
				}

				@Override
				public void read(DataInputView in) throws IOException {
				}

				@Override
				public int hashCode() {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean equals(Object obj) {
					throw new UnsupportedOperationException();
				}
			};

			SerializationResult result = serializer.addRecord(emptyRecord);
			Assert.assertEquals(SerializationResult.FULL_RECORD, result);

			result = serializer.addRecord(emptyRecord);
			Assert.assertEquals(SerializationResult.FULL_RECORD, result);

			result = serializer.addRecord(emptyRecord);
			Assert.assertEquals(SerializationResult.PARTIAL_RECORD_MEMORY_SEGMENT_FULL, result);

			result = serializer.setNextBuffer(buffer);
			Assert.assertEquals(SerializationResult.FULL_RECORD, result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testIntRecordsSpanningMultipleSegments() {
		final int SEGMENT_SIZE = 1;
		final int NUM_VALUES = 10;

		try {
			test(Util.randomRecords(NUM_VALUES, SerializationTestTypeFactory.INT), SEGMENT_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Test encountered an unexpected exception.");
		}
	}

	@Test
	public void testIntRecordsWithAlignedSegments() {
		final int SEGMENT_SIZE = 64;
		final int NUM_VALUES = 64;

		try {
			test(Util.randomRecords(NUM_VALUES, SerializationTestTypeFactory.INT), SEGMENT_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Test encountered an unexpected exception.");
		}
	}

	@Test
	public void testIntRecordsWithUnalignedSegments() {
		final int SEGMENT_SIZE = 31;
		final int NUM_VALUES = 248; // least common multiple => last record should align

		try {
			test(Util.randomRecords(NUM_VALUES, SerializationTestTypeFactory.INT), SEGMENT_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Test encountered an unexpected exception.");
		}
	}

	@Test
	public void testRandomRecords() {
		final int SEGMENT_SIZE = 127;
		final int NUM_VALUES = 100000;

		try {
			test(Util.randomRecords(NUM_VALUES), SEGMENT_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Test encountered an unexpected exception.");
		}
	}

	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Iterates over the provided records and tests whether the {@link SpanningRecordSerializer} returns the expected
	 * {@link SerializationResult} values.
	 * <p>
	 * Only a single {@link MemorySegment} will be allocated.
	 *
	 * @param records records to test
	 * @param segmentSize size for the {@link MemorySegment}
	 */
	private void test(Util.MockRecords records, int segmentSize) throws Exception {
		final int SERIALIZATION_OVERHEAD = 4; // length encoding

		final SpanningRecordSerializer<SerializationTestType> serializer = new SpanningRecordSerializer<SerializationTestType>();
		final Buffer buffer = new Buffer(new MemorySegment(new byte[segmentSize]), segmentSize, null);

		// -------------------------------------------------------------------------------------------------------------

		serializer.setNextBuffer(buffer);

		int numBytes = 0;
		for (SerializationTestType record : records) {
			SerializationResult result = serializer.addRecord(record);
			numBytes += record.length() + SERIALIZATION_OVERHEAD;

			if (numBytes < segmentSize) {
				Assert.assertEquals(SerializationResult.FULL_RECORD, result);
			} else if (numBytes == segmentSize) {
				Assert.assertEquals(SerializationResult.FULL_RECORD_MEMORY_SEGMENT_FULL, result);
				serializer.setNextBuffer(buffer);
				numBytes = 0;
			} else {
				Assert.assertEquals(SerializationResult.PARTIAL_RECORD_MEMORY_SEGMENT_FULL, result);

				while (result.isFullBuffer()) {
					numBytes -= segmentSize;
					result = serializer.setNextBuffer(buffer);
				}
			}
		}
	}
}
