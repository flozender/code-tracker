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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.JobException;
import org.apache.flink.runtime.jobgraph.AbstractJobVertex;
import org.apache.flink.runtime.jobgraph.DistributionPattern;
import org.apache.flink.runtime.jobgraph.JobID;


public class PointwisePatternTest {

	private final JobID jobId = new JobID();
	private final String jobName = "Test Job Sample Name";
	private final Configuration cfg = new Configuration();
	
	@Test
	public void testNToN() {
		final int N = 23;
		
		AbstractJobVertex v1 = new AbstractJobVertex("vertex1");
		AbstractJobVertex v2 = new AbstractJobVertex("vertex2");
	
		v1.setParallelism(N);
		v2.setParallelism(N);
	
		v2.connectNewDataSetAsInput(v1, DistributionPattern.POINTWISE);
	
		List<AbstractJobVertex> ordered = new ArrayList<AbstractJobVertex>(Arrays.asList(v1, v2));

		ExecutionGraph eg = new ExecutionGraph(jobId, jobName, cfg);
		try {
			eg.attachJobGraph(ordered);
		}
		catch (JobException e) {
			e.printStackTrace();
			fail("Job failed with exception: " + e.getMessage());
		}
		
		ExecutionJobVertex target = eg.getAllVertices().get(v2.getID());
		
		for (ExecutionVertex ev : target.getTaskVertices()) {
			assertEquals(1, ev.getNumberOfInputs());
			
			ExecutionEdge[] inEdges = ev.getInputEdges(0);
			assertEquals(1, inEdges.length);
			
			assertEquals(ev.getParallelSubtaskIndex(), inEdges[0].getSource().getPartition());
		}
	}
	
	@Test
	public void test2NToN() {
		final int N = 17;
		
		AbstractJobVertex v1 = new AbstractJobVertex("vertex1");
		AbstractJobVertex v2 = new AbstractJobVertex("vertex2");
	
		v1.setParallelism(2 * N);
		v2.setParallelism(N);
	
		v2.connectNewDataSetAsInput(v1, DistributionPattern.POINTWISE);
	
		List<AbstractJobVertex> ordered = new ArrayList<AbstractJobVertex>(Arrays.asList(v1, v2));

		ExecutionGraph eg = new ExecutionGraph(jobId, jobName, cfg);
		try {
			eg.attachJobGraph(ordered);
		}
		catch (JobException e) {
			e.printStackTrace();
			fail("Job failed with exception: " + e.getMessage());
		}
		
		ExecutionJobVertex target = eg.getAllVertices().get(v2.getID());
		
		for (ExecutionVertex ev : target.getTaskVertices()) {
			assertEquals(1, ev.getNumberOfInputs());
			
			ExecutionEdge[] inEdges = ev.getInputEdges(0);
			assertEquals(2, inEdges.length);
			
			assertEquals(ev.getParallelSubtaskIndex() * 2, inEdges[0].getSource().getPartition());
			assertEquals(ev.getParallelSubtaskIndex() * 2 + 1, inEdges[1].getSource().getPartition());
		}
	}
	
	@Test
	public void test3NToN() {
		final int N = 17;
		
		AbstractJobVertex v1 = new AbstractJobVertex("vertex1");
		AbstractJobVertex v2 = new AbstractJobVertex("vertex2");
	
		v1.setParallelism(3 * N);
		v2.setParallelism(N);
	
		v2.connectNewDataSetAsInput(v1, DistributionPattern.POINTWISE);
	
		List<AbstractJobVertex> ordered = new ArrayList<AbstractJobVertex>(Arrays.asList(v1, v2));

		ExecutionGraph eg = new ExecutionGraph(jobId, jobName, cfg);
		try {
			eg.attachJobGraph(ordered);
		}
		catch (JobException e) {
			e.printStackTrace();
			fail("Job failed with exception: " + e.getMessage());
		}
		
		ExecutionJobVertex target = eg.getAllVertices().get(v2.getID());
		
		for (ExecutionVertex ev : target.getTaskVertices()) {
			assertEquals(1, ev.getNumberOfInputs());
			
			ExecutionEdge[] inEdges = ev.getInputEdges(0);
			assertEquals(3, inEdges.length);
			
			assertEquals(ev.getParallelSubtaskIndex() * 3, inEdges[0].getSource().getPartition());
			assertEquals(ev.getParallelSubtaskIndex() * 3 + 1, inEdges[1].getSource().getPartition());
			assertEquals(ev.getParallelSubtaskIndex() * 3 + 2, inEdges[2].getSource().getPartition());
		}
	}
	
	@Test
	public void testNTo2N() {
		final int N = 41;
		
		AbstractJobVertex v1 = new AbstractJobVertex("vertex1");
		AbstractJobVertex v2 = new AbstractJobVertex("vertex2");
	
		v1.setParallelism(N);
		v2.setParallelism(2 * N);
	
		v2.connectNewDataSetAsInput(v1, DistributionPattern.POINTWISE);
	
		List<AbstractJobVertex> ordered = new ArrayList<AbstractJobVertex>(Arrays.asList(v1, v2));

		ExecutionGraph eg = new ExecutionGraph(jobId, jobName, cfg);
		try {
			eg.attachJobGraph(ordered);
		}
		catch (JobException e) {
			e.printStackTrace();
			fail("Job failed with exception: " + e.getMessage());
		}
		
		ExecutionJobVertex target = eg.getAllVertices().get(v2.getID());
		
		for (ExecutionVertex ev : target.getTaskVertices()) {
			assertEquals(1, ev.getNumberOfInputs());
			
			ExecutionEdge[] inEdges = ev.getInputEdges(0);
			assertEquals(1, inEdges.length);
			
			assertEquals(ev.getParallelSubtaskIndex() / 2, inEdges[0].getSource().getPartition());
		}
	}
	
	@Test
	public void testNTo7N() {
		final int N = 11;
		
		AbstractJobVertex v1 = new AbstractJobVertex("vertex1");
		AbstractJobVertex v2 = new AbstractJobVertex("vertex2");
	
		v1.setParallelism(N);
		v2.setParallelism(7 * N);
	
		v2.connectNewDataSetAsInput(v1, DistributionPattern.POINTWISE);
	
		List<AbstractJobVertex> ordered = new ArrayList<AbstractJobVertex>(Arrays.asList(v1, v2));

		ExecutionGraph eg = new ExecutionGraph(jobId, jobName, cfg);
		try {
			eg.attachJobGraph(ordered);
		}
		catch (JobException e) {
			e.printStackTrace();
			fail("Job failed with exception: " + e.getMessage());
		}
		
		ExecutionJobVertex target = eg.getAllVertices().get(v2.getID());
		
		for (ExecutionVertex ev : target.getTaskVertices()) {
			assertEquals(1, ev.getNumberOfInputs());
			
			ExecutionEdge[] inEdges = ev.getInputEdges(0);
			assertEquals(1, inEdges.length);
			
			assertEquals(ev.getParallelSubtaskIndex() / 7, inEdges[0].getSource().getPartition());
		}
	}
	
	@Test
	public void testLowHighIrregular() {
		testLowToHigh(3, 16);
		testLowToHigh(19, 21);
		testLowToHigh(15, 20);
		testLowToHigh(11, 31);
	}
	
	@Test
	public void testHighLowIrregular() {
		testHighToLow(16, 3);
		testHighToLow(21, 19);
		testHighToLow(20, 15);
		testHighToLow(31, 11);
	}
	
	private void testLowToHigh(int lowDop, int highDop) {
		if (highDop < lowDop) {
			throw new IllegalArgumentException();
		}
		
		final int factor = highDop / lowDop;
		final int delta = highDop % lowDop == 0 ? 0 : 1;
		
		AbstractJobVertex v1 = new AbstractJobVertex("vertex1");
		AbstractJobVertex v2 = new AbstractJobVertex("vertex2");
	
		v1.setParallelism(lowDop);
		v2.setParallelism(highDop);
	
		v2.connectNewDataSetAsInput(v1, DistributionPattern.POINTWISE);
	
		List<AbstractJobVertex> ordered = new ArrayList<AbstractJobVertex>(Arrays.asList(v1, v2));

		ExecutionGraph eg = new ExecutionGraph(jobId, jobName, cfg);
		try {
			eg.attachJobGraph(ordered);
		}
		catch (JobException e) {
			e.printStackTrace();
			fail("Job failed with exception: " + e.getMessage());
		}
		
		ExecutionJobVertex target = eg.getAllVertices().get(v2.getID());
		
		int[] timesUsed = new int[lowDop];
		
		for (ExecutionVertex ev : target.getTaskVertices()) {
			assertEquals(1, ev.getNumberOfInputs());
			
			ExecutionEdge[] inEdges = ev.getInputEdges(0);
			assertEquals(1, inEdges.length);
			
			
			timesUsed[inEdges[0].getSource().getPartition()]++;
		}
		
		for (int i = 0; i < timesUsed.length; i++) {
			assertTrue(timesUsed[i] >= factor && timesUsed[i] <= factor + delta);
		}
	}
	
	private void testHighToLow(int highDop, int lowDop) {
		if (highDop < lowDop) {
			throw new IllegalArgumentException();
		}
		
		final int factor = highDop / lowDop;
		final int delta = highDop % lowDop == 0 ? 0 : 1;
		
		AbstractJobVertex v1 = new AbstractJobVertex("vertex1");
		AbstractJobVertex v2 = new AbstractJobVertex("vertex2");
	
		v1.setParallelism(highDop);
		v2.setParallelism(lowDop);
	
		v2.connectNewDataSetAsInput(v1, DistributionPattern.POINTWISE);
	
		List<AbstractJobVertex> ordered = new ArrayList<AbstractJobVertex>(Arrays.asList(v1, v2));

		ExecutionGraph eg = new ExecutionGraph(jobId, jobName, cfg);
		try {
			eg.attachJobGraph(ordered);
		}
		catch (JobException e) {
			e.printStackTrace();
			fail("Job failed with exception: " + e.getMessage());
		}
		
		ExecutionJobVertex target = eg.getAllVertices().get(v2.getID());
		
		int[] timesUsed = new int[highDop];
		
		for (ExecutionVertex ev : target.getTaskVertices()) {
			assertEquals(1, ev.getNumberOfInputs());
			
			ExecutionEdge[] inEdges = ev.getInputEdges(0);
			assertTrue(inEdges.length >= factor && inEdges.length <= factor + delta);
			
			for (ExecutionEdge ee : inEdges) {
				timesUsed[ee.getSource().getPartition()]++;
			}
		}
		
		for (int i = 0; i < timesUsed.length; i++) {
			assertEquals(1, timesUsed[i]);
		}
	}
}
