/**
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

package org.apache.flink.streaming.connectors.twitter;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.examples.function.JSONParseFlatMap;
import org.apache.flink.util.Collector;
import org.apache.sling.commons.json.JSONException;

/**
 * This program demonstrate the use of TwitterSource. 
 * Its aim is to count the frequency of the languages of tweets
 */
public class TwitterLocal {

	private static final int PARALLELISM = 1;
	private static final int SOURCE_PARALLELISM = 1;
	private static final int NUMBEROFTWEETS = 100;
	
	/**
	 * FlatMapFunction to determine the language of tweets if possible 
	 */
	public static class SelectLanguageFlatMap extends
			JSONParseFlatMap<String, String> {

		private static final long serialVersionUID = 1L;

		/**
		 * Select the language from the incoming JSON text
		 */
		@Override
		public void flatMap(String value, Collector<String> out) throws Exception {
			try{
				out.collect(getString(value, "lang"));
			}
			catch (JSONException e){
				out.collect("");
			}
		}

	}

	public static void main(String[] args) {

		String path = new String();

		if (args != null && args.length == 1) {
			path = args[0];
		} else {
			System.err.println("USAGE:\nTwitterLocal <pathToPropertiesFile>");
			return;
		}

		StreamExecutionEnvironment env = StreamExecutionEnvironment
				.createLocalEnvironment(PARALLELISM);

		DataStream<String> streamSource = env.addSource(new TwitterSource(path, NUMBEROFTWEETS),
				SOURCE_PARALLELISM);


		DataStream<Tuple2<String, Integer>> dataStream = streamSource
				.flatMap(new SelectLanguageFlatMap())
				.partitionBy(0)
				.map(new MapFunction<String, Tuple2<String, Integer>>() {

					private static final long serialVersionUID = 1L;
					
					@Override
					public Tuple2<String, Integer> map(String value) throws Exception {
						return new Tuple2<String, Integer>(value, 1);
					}
				})
				.groupBy(0)
				.sum(1);

		dataStream.print();

		env.execute();
	}
}
