/***********************************************************************************************************************
 *
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
 *
 **********************************************************************************************************************/

package org.apache.flink.api.java.tuple;

import junit.framework.Assert;

import org.apache.flink.api.java.tuple.Tuple2;
import org.junit.Test;

public class Tuple2Test {

	@Test
	public void testSwapValues() {
		Tuple2<String, Integer> toSwap = new Tuple2<String, Integer>(new String("Test case"), 25);
		Tuple2<Integer, String> swapped = toSwap.swap();

		Assert.assertEquals(swapped.f0, toSwap.f1);

		Assert.assertEquals(swapped.f1, toSwap.f0);
	}
}
