/***********************************************************************************************************************
 *
 * Copyright (C) 2010-2014 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.streaming.examples.window.wordcount;

import java.io.BufferedReader;
import java.io.FileReader;

import eu.stratosphere.api.java.tuple.Tuple2;
import eu.stratosphere.streaming.api.function.SourceFunction;
import eu.stratosphere.util.Collector;

public class WindowWordCountSource extends SourceFunction<Tuple2<String, Long>> {
	private static final long serialVersionUID = 1L;
	
	private String line = "";
	private Tuple2<String, Long> outRecord = new Tuple2<String, Long>();
	private Long timestamp = 0L;

	// Reads the lines of the input file and adds a timestamp to it.
	@Override
	public void invoke(Collector<Tuple2<String, Long>> collector) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("src/test/resources/testdata/hamlet.txt"));
		while(true){
			line = br.readLine();
			if(line==null){
				break;
			}
			if (line != "") {
				line=line.replaceAll("[\\-\\+\\.\\^:,]", "");
				outRecord.f0 = line;
				outRecord.f1 = timestamp;
				collector.collect(outRecord);
				timestamp++;
			}
		}		
	}
}
