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


package org.apache.flink.test.hadoopcompatibility.mapred;


import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.hadoopcompatibility.mapred.HadoopInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapred.FileInputFormat;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.fail;


public class HadoopInputFormatTest {


	public class DummyVoidKeyInputFormat<T> extends FileInputFormat<Void, T> {

		public DummyVoidKeyInputFormat() {
		}

		@Override
		public org.apache.hadoop.mapred.RecordReader<Void, T> getRecordReader(org.apache.hadoop.mapred.InputSplit inputSplit, JobConf jobConf, Reporter reporter) throws IOException {
			return null;
		}
	}
	
	
	@Test
	public void checkTypeInformation() {
		try {
			final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

			// Set up the Hadoop Input Format
			Job job = Job.getInstance();
			HadoopInputFormat<Void, Long> hadoopInputFormat = new HadoopInputFormat<Void, Long>( new DummyVoidKeyInputFormat(), Void.class, Long.class, new JobConf());

			TypeInformation<Tuple2<Void,Long>> tupleType = hadoopInputFormat.getProducedType();
			TypeInformation<Tuple2<Void,Long>> testTupleType = new TupleTypeInfo<Tuple2<Void,Long>>(BasicTypeInfo.VOID_TYPE_INFO, BasicTypeInfo.LONG_TYPE_INFO);
			
			if(tupleType.isTupleType()) {
				if(!((TupleTypeInfo)tupleType).equals(testTupleType)) {
					fail("Tuple type information was not set correctly!");
				}
			} else {
				fail("Type information was not set to tuple type information!");
			}

		}
		catch (Exception ex) {
			fail("Test failed due to a " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
		}

	}
	
}
