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
package org.apache.flink.api.common.typeutils.base.array;

import org.apache.flink.api.common.typeutils.SerializerTestBase;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.array.BooleanPrimitiveArraySerializer;
import org.apache.flink.api.common.typeutils.base.array.LongPrimitiveArraySerializer;

/**
 * A test for the {@link LongPrimitiveArraySerializer}.
 */
public class BooleanPrimitiveArraySerializerTest extends SerializerTestBase<boolean[]> {

	@Override
	protected TypeSerializer<boolean[]> createSerializer() {
		return new BooleanPrimitiveArraySerializer();
	}

	@Override
	protected Class<boolean[]> getTypeClass() {
		return boolean[].class;
	}
	
	@Override
	protected int getLength() {
		return -1;
	}

	@Override
	protected boolean[][] getTestData() {
		return new boolean[][] {
			new boolean[] {true, false, true, true, true, true, false, true},
			new boolean[] {},
			new boolean[] {false, true, false, false, false, false, true, false}
		};
	}
}
