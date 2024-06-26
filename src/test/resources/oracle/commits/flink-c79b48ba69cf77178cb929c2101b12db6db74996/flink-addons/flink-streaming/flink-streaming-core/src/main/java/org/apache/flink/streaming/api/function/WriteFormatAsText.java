/***********************************************************************************************************************
 *
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
 *
 **********************************************************************************************************************/

package org.apache.flink.streaming.api.function;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.flink.api.java.tuple.Tuple;

/**
 * Writes tuples in text format.
 *
 * @param <IN>
 *            Input tuple type
 */
public class WriteFormatAsText<IN extends Tuple> extends WriteFormat<IN> {
	private static final long serialVersionUID = 1L;

	@Override
	public void write(String path, ArrayList<IN> tupleList) {
		try {
			PrintWriter outStream = new PrintWriter(new BufferedWriter(new FileWriter(path, true)));
			for (IN tupleToWrite : tupleList) {
				outStream.println(tupleToWrite);
			}
			outStream.close();
		} catch (IOException e) {
			throw new RuntimeException("Exception occured while writing file " + path, e);
		}
	}
}
