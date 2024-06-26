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

package org.apache.flink.runtime.operators.testutils;

import org.apache.flink.types.IntValue;
import org.apache.flink.types.Record;
import org.apache.flink.util.MutableObjectIterator;

/**
 * A simple iterator that returns an infinite amount of records resembling (0, 0) pairs.
 */
public class InfiniteInputIterator implements MutableObjectIterator<Record>
{
	private final IntValue val1 = new IntValue(0);
	private final IntValue val2 = new IntValue(0);
	
	@Override
	public Record next(Record reuse) {
		reuse.setField(0, val1);
		reuse.setField(1, val2);
		return reuse;
	}
}
