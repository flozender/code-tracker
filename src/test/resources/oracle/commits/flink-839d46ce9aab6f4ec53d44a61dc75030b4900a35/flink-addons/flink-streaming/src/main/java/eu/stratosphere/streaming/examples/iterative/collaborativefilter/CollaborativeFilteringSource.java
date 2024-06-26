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

package eu.stratosphere.streaming.examples.iterative.collaborativefilter;

import java.io.BufferedReader;
import java.io.FileReader;

import eu.stratosphere.api.java.tuple.Tuple3;
import eu.stratosphere.streaming.api.invokable.UserSourceInvokable;
import eu.stratosphere.streaming.api.streamrecord.StreamRecord;

public class CollaborativeFilteringSource extends UserSourceInvokable {
	private static final long serialVersionUID = 1L;
	
	private BufferedReader br = null;
	private String line = new String();
	private StreamRecord outRecord = new StreamRecord(new Tuple3<Integer, Integer, Integer>());
	
	@Override
	public void invoke() throws Exception {
		// TODO Auto-generated method stub
		br = new BufferedReader(new FileReader(
				"src/test/resources/testdata/MovieLens100k.data"));
		while (true) {
			line = br.readLine();
			if (line == null) {
				break;
			}
			if (line != "") {
				String[] items=line.split("\t");
				outRecord.setInteger(0, Integer.valueOf(items[0]));
				outRecord.setInteger(1, Integer.valueOf(items[1]));
				outRecord.setInteger(2, Integer.valueOf(items[2]));
				emit(outRecord);
				performanceCounter.count();
			}
			line = br.readLine();
		}		
	}

}
