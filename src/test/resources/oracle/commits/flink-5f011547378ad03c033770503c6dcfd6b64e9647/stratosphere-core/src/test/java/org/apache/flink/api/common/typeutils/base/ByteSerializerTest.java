/***********************************************************************************************************************
 *
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
 *
 **********************************************************************************************************************/
package org.apache.flink.api.common.typeutils.base;

import java.util.Random;

import org.apache.flink.api.common.typeutils.SerializerTestBase;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.ByteSerializer;

/**
 * A test for the {@link ByteSerializer}.
 */
public class ByteSerializerTest extends SerializerTestBase<Byte> {
	
	@Override
	protected TypeSerializer<Byte> createSerializer() {
		return new ByteSerializer();
	}
	
	@Override
	protected int getLength() {
		return 1;
	}
	
	@Override
	protected Class<Byte> getTypeClass() {
		return Byte.class;
	}
	
	@Override
	protected Byte[] getTestData() {
		Random rnd = new Random(874597969123412341L);
		byte byteArray[] = new byte[1];
		rnd.nextBytes(byteArray);
		
		return new Byte[] {new Byte((byte) 0), new Byte((byte) 1), new Byte((byte) -1), 
							Byte.MAX_VALUE, Byte.MIN_VALUE,
							new Byte(byteArray[0]), new Byte((byte) -byteArray[0])};
	}
}
