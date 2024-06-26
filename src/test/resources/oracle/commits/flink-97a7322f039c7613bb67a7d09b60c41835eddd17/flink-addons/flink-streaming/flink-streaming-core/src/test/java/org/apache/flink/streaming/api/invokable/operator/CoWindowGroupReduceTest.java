/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.invokable.operator;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.flink.api.common.functions.CoGroupFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.invokable.operator.co.CoGroupInvokable;
import org.apache.flink.streaming.api.invokable.util.TimeStamp;
import org.apache.flink.streaming.util.MockCoInvokable;
import org.apache.flink.util.Collector;
import org.junit.Test;

public class CoWindowGroupReduceTest{

	public static final class MyCoGroup1 implements CoGroupFunction<Integer, Integer, Integer> {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		@Override
		public void coGroup(Iterable<Integer> first, Iterable<Integer> second,
				Collector<Integer> out) throws Exception {
			Integer count1 = 0;
			for (Integer i : first) {
				count1++;
			}
			Integer count2 = 0;
			for (Integer i : second) {
				count2++;
			}
			out.collect(count1);
			out.collect(count2);

		}

	}

	public static final class MyCoGroup2 implements
			CoGroupFunction<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>, Integer> {

		private static final long serialVersionUID = 1L;

		@Override
		public void coGroup(Iterable<Tuple2<Integer, Integer>> first,
				Iterable<Tuple2<Integer, Integer>> second, Collector<Integer> out) throws Exception {

			Set<Integer> firstElements = new HashSet<Integer>();
			for (Tuple2<Integer, Integer> value : first) {
				firstElements.add(value.f1);
			}
			for (Tuple2<Integer, Integer> value : second) {
				if (firstElements.contains(value.f1)) {
					out.collect(value.f1);
				}
			}

		}

	}

	private static final class MyTS1 implements TimeStamp<Integer> {

		private static final long serialVersionUID = 1L;

		@Override
		public long getTimestamp(Integer value) {
			return value;
		}

		@Override
		public long getStartTime() {
			return 1;
		}

	}

	private static final class MyTS2 implements TimeStamp<Tuple2<Integer, Integer>> {

		private static final long serialVersionUID = 1L;

		@Override
		public long getTimestamp(Tuple2<Integer, Integer> value) {
			return value.f0;
		}

		@Override
		public long getStartTime() {
			return 1;
		}

	}

	@Test
	public void coWindowGroupReduceTest2() throws Exception {

		CoGroupInvokable<Integer, Integer, Integer> invokable1 = new CoGroupInvokable<Integer, Integer, Integer>(
				new MyCoGroup1(), 2, 1, new MyTS1(), new MyTS1());

		// Windowsize 2, slide 1
		// 1,2|2,3|3,4|4,5

		List<Integer> input11 = new ArrayList<Integer>();
		input11.add(1);
		input11.add(1);
		input11.add(2);
		input11.add(3);
		input11.add(3);

		List<Integer> input12 = new ArrayList<Integer>();
		input12.add(1);
		input12.add(2);
		input12.add(3);
		input12.add(3);
		input12.add(5);

		// Windows: (1,1,2)(1,1,2)|(2,3,3)(2,3,3)|(3,3)(3,3)|(5)(5)
		// expected output: 3,2|3,3|2,2|0,1

		List<Integer> expected1 = new ArrayList<Integer>();
		expected1.add(3);
		expected1.add(2);
		expected1.add(3);
		expected1.add(3);
		expected1.add(2);
		expected1.add(2);
		expected1.add(0);
		expected1.add(1);

		List<Integer> actual1 = MockCoInvokable.createAndExecute(invokable1, input11, input12);
		assertEquals(expected1, actual1);
		
		CoGroupInvokable<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>, Integer> invokable2 = new CoGroupInvokable<Tuple2<Integer,Integer>, Tuple2<Integer,Integer>, Integer>(new MyCoGroup2(), 2, 3, new MyTS2(),  new MyTS2());
		
		//WindowSize 2, slide 3
		//1,2|4,5|7,8|
		
		List<Tuple2<Integer,Integer>> input21 = new ArrayList<Tuple2<Integer,Integer>>();
		input21.add(new Tuple2<Integer, Integer>(1,1));
		input21.add(new Tuple2<Integer, Integer>(1,2));
		input21.add(new Tuple2<Integer, Integer>(2,3));
		input21.add(new Tuple2<Integer, Integer>(3,4));
		input21.add(new Tuple2<Integer, Integer>(3,5));
		input21.add(new Tuple2<Integer, Integer>(4,6));
		input21.add(new Tuple2<Integer, Integer>(4,7));
		input21.add(new Tuple2<Integer, Integer>(5,8));
		
		List<Tuple2<Integer,Integer>> input22 = new ArrayList<Tuple2<Integer,Integer>>();
		input22.add(new Tuple2<Integer, Integer>(1,1));
		input22.add(new Tuple2<Integer, Integer>(2,0));
		input22.add(new Tuple2<Integer, Integer>(2,2));
		input22.add(new Tuple2<Integer, Integer>(3,9));
		input22.add(new Tuple2<Integer, Integer>(3,4));
		input22.add(new Tuple2<Integer, Integer>(4,10));
		input22.add(new Tuple2<Integer, Integer>(5,8));
		input22.add(new Tuple2<Integer, Integer>(5,7));
		
		
		List<Integer> expected2 = new ArrayList<Integer>();
		expected2.add(1);
		expected2.add(2);
		expected2.add(8);
		expected2.add(7);
		
		List<Integer> actual2 = MockCoInvokable.createAndExecute(invokable2, input21, input22);
		assertEquals(expected2, actual2);
	}
}
