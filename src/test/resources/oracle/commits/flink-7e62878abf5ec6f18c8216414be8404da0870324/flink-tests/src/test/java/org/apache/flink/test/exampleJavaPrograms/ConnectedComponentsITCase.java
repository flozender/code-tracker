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

package org.apache.flink.test.exampleJavaPrograms;

import java.io.BufferedReader;

import org.apache.flink.example.java.graph.ConnectedComponents;
import org.apache.flink.test.testdata.ConnectedComponentsData;
import org.apache.flink.test.util.JavaProgramTestBase;

public class ConnectedComponentsITCase extends JavaProgramTestBase {
	
	private static final long SEED = 0xBADC0FFEEBEEFL;
	
	private static final int NUM_VERTICES = 1000;
	
	private static final int NUM_EDGES = 10000;

	
	private String verticesPath;
	private String edgesPath;
	private String resultPath;
	
	
	@Override
	protected void preSubmit() throws Exception {
		verticesPath = createTempFile("vertices.txt", ConnectedComponentsData.getEnumeratingVertices(NUM_VERTICES));
		edgesPath = createTempFile("edges.txt", ConnectedComponentsData.getRandomOddEvenEdges(NUM_EDGES, NUM_VERTICES, SEED));
		resultPath = getTempFilePath("results");
	}
	
	@Override
	protected void testProgram() throws Exception {
		ConnectedComponents.main(verticesPath, edgesPath, resultPath, "100");
	}

	@Override
	protected void postSubmit() throws Exception {
		for (BufferedReader reader : getResultReader(resultPath)) {
			ConnectedComponentsData.checkOddEvenResult(reader);
		}
	}
}
