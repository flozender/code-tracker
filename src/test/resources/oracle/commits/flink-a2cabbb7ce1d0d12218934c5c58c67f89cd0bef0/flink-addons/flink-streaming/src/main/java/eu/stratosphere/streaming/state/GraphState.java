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

package eu.stratosphere.streaming.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphState {
	public Map<Integer, Set<Integer>> vertices = null;

	public GraphState() {
		vertices = new HashMap<Integer, Set<Integer>>();
	}

	public void insertDirectedEdge(int sourceNode, int targetNode) {
		if (!vertices.containsKey(sourceNode)) {
			vertices.put(sourceNode, new HashSet<Integer>());
		}
		vertices.get(sourceNode).add(targetNode);
	}
	
	public void insertUndirectedEdge(int sourceNode, int targetNode){
		if(!vertices.containsKey(sourceNode)){
			vertices.put(sourceNode, new HashSet<Integer>());
		}
		if(!vertices.containsKey(targetNode)){
			vertices.put(targetNode, new HashSet<Integer>());
		}
		vertices.get(sourceNode).add(targetNode);
		vertices.get(targetNode).add(sourceNode);
	}
}
