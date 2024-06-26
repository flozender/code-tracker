/**
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

package org.apache.flink.runtime.executiongraph;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.jobgraph.AbstractJobVertex;
import org.apache.flink.runtime.jobgraph.DistributionPattern;
import org.apache.flink.runtime.jobgraph.JobID;
import org.apache.flink.runtime.jobmanager.scheduler.SlotSharingGroup;
import org.junit.Test;

public class VertexSlotSharingTest {

	/*
	 * Test setup:
	 * - v1 is isolated, no slot sharing
	 * - v2 and v3 (not connected) share slots
	 * - v4 and v5 (connected) share slots
	 */
	@Test
	public void testAssignSlotSharingGroup() {
		try {
			AbstractJobVertex v1 = new AbstractJobVertex("v1");
			AbstractJobVertex v2 = new AbstractJobVertex("v2");
			AbstractJobVertex v3 = new AbstractJobVertex("v3");
			AbstractJobVertex v4 = new AbstractJobVertex("v4");
			AbstractJobVertex v5 = new AbstractJobVertex("v5");
			
			v1.setParallelism(4);
			v2.setParallelism(5);
			v3.setParallelism(7);
			v4.setParallelism(1);
			v5.setParallelism(11);
			
			v2.connectNewDataSetAsInput(v1, DistributionPattern.POINTWISE);
			v5.connectNewDataSetAsInput(v4, DistributionPattern.POINTWISE);
			
			SlotSharingGroup jg1 = new SlotSharingGroup();
			v2.setSlotSharingGroup(jg1);
			v3.setSlotSharingGroup(jg1);
			
			SlotSharingGroup jg2 = new SlotSharingGroup();
			v4.setSlotSharingGroup(jg2);
			v5.setSlotSharingGroup(jg2);
			
			List<AbstractJobVertex> vertices = new ArrayList<AbstractJobVertex>(Arrays.asList(v1, v2, v3, v4, v5));
			
			ExecutionGraph eg = new ExecutionGraph(new JobID(), "test job", new Configuration());
			eg.attachJobGraph(vertices);
			
			// verify that the vertices are all in the same slot sharing group
			SlotSharingGroup group1 = null;
			SlotSharingGroup group2 = null;
			
			// verify that v1 tasks have no slot sharing group
			assertNull(eg.getJobVertex(v1.getID()).getSlotSharingGroup());
			
			// v2 and v3 are shared
			group1 = eg.getJobVertex(v2.getID()).getSlotSharingGroup();
			assertNotNull(group1);
			assertEquals(group1, eg.getJobVertex(v3.getID()).getSlotSharingGroup());
			
			assertEquals(2, group1.getJobVertexIds().size());
			assertTrue(group1.getJobVertexIds().contains(v2.getID()));
			assertTrue(group1.getJobVertexIds().contains(v3.getID()));
			
			// v4 and v5 are shared
			group2 = eg.getJobVertex(v4.getID()).getSlotSharingGroup();
			assertNotNull(group2);
			assertEquals(group2, eg.getJobVertex(v5.getID()).getSlotSharingGroup());
			
			assertEquals(2, group1.getJobVertexIds().size());
			assertTrue(group2.getJobVertexIds().contains(v4.getID()));
			assertTrue(group2.getJobVertexIds().contains(v5.getID()));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
