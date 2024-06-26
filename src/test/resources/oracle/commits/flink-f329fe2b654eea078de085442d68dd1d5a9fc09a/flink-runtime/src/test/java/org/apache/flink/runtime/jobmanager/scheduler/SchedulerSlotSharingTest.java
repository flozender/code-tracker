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

package org.apache.flink.runtime.jobmanager.scheduler;

import static org.apache.flink.runtime.jobmanager.scheduler.SchedulerTestUtils.areAllDistinct;
import static org.apache.flink.runtime.jobmanager.scheduler.SchedulerTestUtils.getTestVertex;
import static org.apache.flink.runtime.jobmanager.scheduler.SchedulerTestUtils.getTestVertexWithLocation;
import static org.apache.flink.runtime.jobmanager.scheduler.SchedulerTestUtils.getRandomInstance;
import static org.junit.Assert.*;

import org.junit.Test;

import org.apache.flink.runtime.instance.AllocatedSlot;
import org.apache.flink.runtime.instance.Instance;
import org.apache.flink.runtime.jobgraph.JobVertexID;

/**
 * Tests for the scheduler when scheduling tasks in slot sharing groups.
 */
public class SchedulerSlotSharingTest {
	
	@Test
	public void scheduleSingleVertexType() {
		try {
			JobVertexID jid1 = new JobVertexID();
			
			SlotSharingGroup sharingGroup = new SlotSharingGroup(jid1);
			
			Scheduler scheduler = new Scheduler();
			Instance i1 = getRandomInstance(2);
			Instance i2 = getRandomInstance(2);
			scheduler.newInstanceAvailable(i1);
			scheduler.newInstanceAvailable(i2);
			
			// schedule 4 tasks from the first vertex group
			AllocatedSlot s1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 0, 8), sharingGroup));
			AllocatedSlot s2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 1, 8), sharingGroup));
			AllocatedSlot s3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 2, 8), sharingGroup));
			AllocatedSlot s4 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 3, 8), sharingGroup));
			
			assertNotNull(s1);
			assertNotNull(s2);
			assertNotNull(s3);
			assertNotNull(s4);
			
			assertTrue(areAllDistinct(s1, s2, s3, s4));
			
			// we cannot schedule another task from the first vertex group
			try {
				scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 4, 8), sharingGroup));
				fail("Scheduler accepted too many tasks at the same time");
			}
			catch (NoResourceAvailableException e) {
				// good!
			}
			catch (Exception e) {
				fail("Wrong exception.");
			}
			
			// release something
			s3.releaseSlot();
			
			// allocate another slot from that group
			AllocatedSlot s5 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 4, 8), sharingGroup));
			assertNotNull(s5);
			
			// release all old slots
			s1.releaseSlot();
			s2.releaseSlot();
			s4.releaseSlot();
			
			AllocatedSlot s6 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 5, 8), sharingGroup));
			AllocatedSlot s7 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 6, 8), sharingGroup));
			AllocatedSlot s8 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 7, 8), sharingGroup));
			
			assertNotNull(s6);
			assertNotNull(s7);
			assertNotNull(s8);
			
			// make sure we have two slots on the first instance, and two on the second
			int c = 0;
			c += (s5.getInstance() == i1) ? 1 : -1;
			c += (s6.getInstance() == i1) ? 1 : -1;
			c += (s7.getInstance() == i1) ? 1 : -1;
			c += (s8.getInstance() == i1) ? 1 : -1;
			assertEquals(0, c);
			
			// release all
			s5.releaseSlot();
			s6.releaseSlot();
			s7.releaseSlot();
			s8.releaseSlot();
			
			// test that everything is released
			assertEquals(4, scheduler.getNumberOfAvailableSlots());

			// check the scheduler's bookkeeping
			assertEquals(0, scheduler.getNumberOfLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfNonLocalizedAssignments());
			assertEquals(8, scheduler.getNumberOfUnconstrainedAssignments());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void scheduleImmediatelyWithSharing() {
		try {
			JobVertexID jid1 = new JobVertexID();
			JobVertexID jid2 = new JobVertexID();
			
			SlotSharingGroup sharingGroup = new SlotSharingGroup(jid1, jid2);
			
			Scheduler scheduler = new Scheduler();
			scheduler.newInstanceAvailable(getRandomInstance(2));
			scheduler.newInstanceAvailable(getRandomInstance(2));
			
			// schedule 4 tasks from the first vertex group
			AllocatedSlot s1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 0, 5), sharingGroup));
			AllocatedSlot s2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 1, 5), sharingGroup));
			AllocatedSlot s3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 2, 5), sharingGroup));
			AllocatedSlot s4 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 3, 5), sharingGroup));
			
			assertNotNull(s1);
			assertNotNull(s2);
			assertNotNull(s3);
			assertNotNull(s4);
			
			assertTrue(areAllDistinct(s1, s2, s3, s4));
			
			// we cannot schedule another task from the first vertex group
			try {
				scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 4, 5), sharingGroup));
				fail("Scheduler accepted too many tasks at the same time");
			}
			catch (NoResourceAvailableException e) {
				// good!
			}
			catch (Exception e) {
				fail("Wrong exception.");
			}
			
			// schedule some tasks from the second ID group
			AllocatedSlot s1_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 0, 5), sharingGroup));
			AllocatedSlot s2_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 1, 5), sharingGroup));
			AllocatedSlot s3_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 2, 5), sharingGroup));
			AllocatedSlot s4_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 3, 5), sharingGroup));
			
			assertNotNull(s1_2);
			assertNotNull(s2_2);
			assertNotNull(s3_2);
			assertNotNull(s4_2);
			
			// we cannot schedule another task from the second vertex group
			try {
				scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 4, 5), sharingGroup));
				fail("Scheduler accepted too many tasks at the same time");
			}
			catch (NoResourceAvailableException e) {
				// good!
			}
			catch (Exception e) {
				fail("Wrong exception.");
			}
			
			// now, we release some vertices (sub-slots) from the first group.
			// that should allow us to schedule more vertices from the first group
			s1.releaseSlot();
			s4.releaseSlot();
			
			assertEquals(4, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(2, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid1));
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid2));
			
			// we can still not schedule anything from the second group of vertices
			try {
				scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 4, 5), sharingGroup));
				fail("Scheduler accepted too many tasks at the same time");
			}
			catch (NoResourceAvailableException e) {
				// good!
			}
			catch (Exception e) {
				fail("Wrong exception.");
			}
			
			// we can schedule something from the first vertex group
			AllocatedSlot s5 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 4, 5), sharingGroup));
			assertNotNull(s5);
			
			assertEquals(4, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(1, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid1));
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid2));
			
			
			// now we release a slot from the second vertex group and schedule another task from that group
			s2_2.releaseSlot();
			AllocatedSlot s5_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 4, 5), sharingGroup));
			assertNotNull(s5_2);
			
			// release all slots
			s2.releaseSlot();
			s3.releaseSlot();
			s5.releaseSlot();
			
			s1_2.releaseSlot();
			s3_2.releaseSlot();
			s4_2.releaseSlot();
			s5_2.releaseSlot();
			
			// test that everything is released
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(4, scheduler.getNumberOfAvailableSlots());
			
			// check the scheduler's bookkeeping
			assertEquals(0, scheduler.getNumberOfLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfNonLocalizedAssignments());
			assertEquals(10, scheduler.getNumberOfUnconstrainedAssignments());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void scheduleImmediatelyWithIntermediateTotallyEmptySharingGroup() {
		try {
			JobVertexID jid1 = new JobVertexID();
			JobVertexID jid2 = new JobVertexID();
			
			SlotSharingGroup sharingGroup = new SlotSharingGroup(jid1, jid2);
			
			Scheduler scheduler = new Scheduler();
			scheduler.newInstanceAvailable(getRandomInstance(2));
			scheduler.newInstanceAvailable(getRandomInstance(2));
			
			// schedule 4 tasks from the first vertex group
			AllocatedSlot s1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 0, 4), sharingGroup));
			AllocatedSlot s2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 1, 4), sharingGroup));
			AllocatedSlot s3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 2, 4), sharingGroup));
			AllocatedSlot s4 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 3, 4), sharingGroup));
			
			assertEquals(4, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid1));
			assertEquals(4, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid2));
			
			s1.releaseSlot();
			s2.releaseSlot();
			s3.releaseSlot();
			s4.releaseSlot();
			
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid1));
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid2));
			
			// schedule some tasks from the second ID group
			AllocatedSlot s1_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 0, 4), sharingGroup));
			AllocatedSlot s2_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 1, 4), sharingGroup));
			AllocatedSlot s3_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 2, 4), sharingGroup));
			AllocatedSlot s4_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 3, 4), sharingGroup));

			assertEquals(4, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(4, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid1));
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid2));
			
			s1_2.releaseSlot();
			s2_2.releaseSlot();
			s3_2.releaseSlot();
			s4_2.releaseSlot();
			
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid1));
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid2));

			// test that everything is released
			assertEquals(4, scheduler.getNumberOfAvailableSlots());
			
			// check the scheduler's bookkeeping
			assertEquals(0, scheduler.getNumberOfLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfNonLocalizedAssignments());
			assertEquals(8, scheduler.getNumberOfUnconstrainedAssignments());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void scheduleImmediatelyWithTemprarilyEmptyVertexGroup() {
		try {
			JobVertexID jid1 = new JobVertexID();
			JobVertexID jid2 = new JobVertexID();
			JobVertexID jid3 = new JobVertexID();
			
			SlotSharingGroup sharingGroup = new SlotSharingGroup(jid1, jid2, jid3);
			
			Scheduler scheduler = new Scheduler();
			scheduler.newInstanceAvailable(getRandomInstance(2));
			scheduler.newInstanceAvailable(getRandomInstance(2));
			
			// schedule 4 tasks from the first vertex group
			AllocatedSlot s1_1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 0, 4), sharingGroup));
			AllocatedSlot s2_1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 1, 4), sharingGroup));
			AllocatedSlot s3_1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 2, 4), sharingGroup));
			AllocatedSlot s4_1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 3, 4), sharingGroup));
			
			assertNotNull(s1_1);
			assertNotNull(s2_1);
			assertNotNull(s3_1);
			assertNotNull(s4_1);
			
			assertTrue(areAllDistinct(s1_1, s2_1, s3_1, s4_1));
			
			// schedule 4 tasks from the second vertex group
			AllocatedSlot s1_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 0, 7), sharingGroup));
			AllocatedSlot s2_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 1, 7), sharingGroup));
			AllocatedSlot s3_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 2, 7), sharingGroup));
			AllocatedSlot s4_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 3, 7), sharingGroup));
			
			assertNotNull(s1_2);
			assertNotNull(s2_2);
			assertNotNull(s3_2);
			assertNotNull(s4_2);
			
			assertTrue(areAllDistinct(s1_2, s2_2, s3_2, s4_2));
			
			// schedule 4 tasks from the third vertex group
			AllocatedSlot s1_3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid3, 0, 4), sharingGroup));
			AllocatedSlot s2_3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid3, 1, 4), sharingGroup));
			AllocatedSlot s3_3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid3, 2, 4), sharingGroup));
			AllocatedSlot s4_3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid3, 3, 4), sharingGroup));
			
			assertNotNull(s1_3);
			assertNotNull(s2_3);
			assertNotNull(s3_3);
			assertNotNull(s4_3);
			
			assertTrue(areAllDistinct(s1_3, s2_3, s3_3, s4_3));
			
			
			// we cannot schedule another task from the second vertex group
			try {
				scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 4, 5), sharingGroup));
				fail("Scheduler accepted too many tasks at the same time");
			}
			catch (NoResourceAvailableException e) {
				// good!
			}
			catch (Exception e) {
				fail("Wrong exception.");
			}
			
			// release the second vertex group
			s1_2.releaseSlot();
			s2_2.releaseSlot();
			s3_2.releaseSlot();
			s4_2.releaseSlot();
			
			AllocatedSlot s5_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 5, 7), sharingGroup));
			AllocatedSlot s6_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 6, 7), sharingGroup));
			AllocatedSlot s7_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 7, 7), sharingGroup));
			
			assertNotNull(s5_2);
			assertNotNull(s6_2);
			assertNotNull(s7_2);
			
			// release the slots
			s1_1.releaseSlot();
			s2_1.releaseSlot();
			s3_1.releaseSlot();
			s4_1.releaseSlot();
			
			s5_2.releaseSlot();
			s6_2.releaseSlot();
			s7_2.releaseSlot();
			
			// test that everything is released
			assertEquals(0, scheduler.getNumberOfAvailableSlots());
			
			s1_3.releaseSlot();
			s2_3.releaseSlot();
			s3_3.releaseSlot();
			s4_3.releaseSlot();
			
			// test that everything is released
			assertEquals(4, scheduler.getNumberOfAvailableSlots());
			
			// check the scheduler's bookkeeping
			assertEquals(0, scheduler.getNumberOfLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfNonLocalizedAssignments());
			assertEquals(15, scheduler.getNumberOfUnconstrainedAssignments());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void scheduleImmediatelyWithTemprarilyEmptyVertexGroup2() {
		try {
			JobVertexID jid1 = new JobVertexID();
			JobVertexID jid2 = new JobVertexID();
			JobVertexID jid3 = new JobVertexID();
			
			SlotSharingGroup sharingGroup = new SlotSharingGroup(jid1, jid2);
			
			Scheduler scheduler = new Scheduler();
			scheduler.newInstanceAvailable(getRandomInstance(2));
			
			// schedule 1 tasks from the first vertex group and 2 from the second
			AllocatedSlot s1_1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 0, 2), sharingGroup));
			AllocatedSlot s2_1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 0, 2), sharingGroup));
			AllocatedSlot s2_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 1, 2), sharingGroup));
			
			assertNotNull(s1_1);
			assertNotNull(s2_1);
			assertNotNull(s2_2);
			
			assertEquals(2, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(1, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid1));
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid2));
			
			// release the two from the second
			s2_1.releaseSlot();
			s2_2.releaseSlot();
			
			
			// this should free one slot so we can allocate one non-shared
			AllocatedSlot sx = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid3, 0, 1)));
			assertNotNull(sx);
			
			assertEquals(1, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(0, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid1));
			assertEquals(1, sharingGroup.getTaskAssignment().getNumberOfAvailableSlotsForJid(jid2));
			
			// check the scheduler's bookkeeping
			assertEquals(0, scheduler.getNumberOfLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfNonLocalizedAssignments());
			assertEquals(4, scheduler.getNumberOfUnconstrainedAssignments());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void scheduleMixedSharingAndNonSharing() {
		try {
			JobVertexID jid1 = new JobVertexID();
			JobVertexID jid2 = new JobVertexID();
			
			JobVertexID jidA = new JobVertexID();
			JobVertexID jidB= new JobVertexID();
			JobVertexID jidC = new JobVertexID();
			
			SlotSharingGroup sharingGroup = new SlotSharingGroup(jid1, jid2);
			
			Scheduler scheduler = new Scheduler();
			scheduler.newInstanceAvailable(getRandomInstance(3));
			scheduler.newInstanceAvailable(getRandomInstance(2));
			
			// schedule some individual vertices
			AllocatedSlot sA1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jidA, 0, 2)));
			AllocatedSlot sA2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jidA, 1, 2)));
			assertNotNull(sA1);
			assertNotNull(sA2);
			
			// schedule some vertices in the sharing group
			AllocatedSlot s1_0 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 0, 4), sharingGroup));
			AllocatedSlot s1_1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 1, 4), sharingGroup));
			AllocatedSlot s2_0 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 0, 4), sharingGroup));
			AllocatedSlot s2_1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 1, 4), sharingGroup));
			assertNotNull(s1_0);
			assertNotNull(s1_1);
			assertNotNull(s2_0);
			assertNotNull(s2_1);
			
			// schedule another isolated vertex
			AllocatedSlot sB1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jidB, 1, 3)));
			assertNotNull(sB1);
			
			// should not be able to schedule more vertices
			try {
				scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 2, 4), sharingGroup));
				fail("Scheduler accepted too many tasks at the same time");
			}
			catch (NoResourceAvailableException e) {
				// good!
			}
			catch (Exception e) {
				fail("Wrong exception.");
			}
			
			try {
				scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 2, 4), sharingGroup));
				fail("Scheduler accepted too many tasks at the same time");
			}
			catch (NoResourceAvailableException e) {
				// good!
			}
			catch (Exception e) {
				fail("Wrong exception.");
			}
			
			try {
				scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jidB, 0, 3)));
				fail("Scheduler accepted too many tasks at the same time");
			}
			catch (NoResourceAvailableException e) {
				// good!
			}
			catch (Exception e) {
				fail("Wrong exception.");
			}
			
			try {
				scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jidC, 0, 1)));
				fail("Scheduler accepted too many tasks at the same time");
			}
			catch (NoResourceAvailableException e) {
				// good!
			}
			catch (Exception e) {
				fail("Wrong exception.");
			}
			
			// release some isolated task and check that the sharing group may grow
			sA1.releaseSlot();
			
			AllocatedSlot s1_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 2, 4), sharingGroup));
			AllocatedSlot s2_2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 3, 4), sharingGroup));
			assertNotNull(s1_2);
			assertNotNull(s2_2);
			
			// release three of the previously allocated sub slots, which guarantees to return one shared slot
			s1_0.releaseSlot();
			s1_1.releaseSlot();
			s2_0.releaseSlot();
			
			assertEquals(1, scheduler.getNumberOfAvailableSlots());
			
			// schedule one more no-shared task
			AllocatedSlot sB0 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jidB, 0, 3)));
			assertNotNull(sB0);
			
			// release the last of the original shared slots and allocate one more non-shared slot
			s2_1.releaseSlot();
			AllocatedSlot sB2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jidB, 2, 3)));
			assertNotNull(sB2);
			
			
			// release on non-shared and add some shared slots
			sA2.releaseSlot();
			AllocatedSlot s1_3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid1, 3, 4), sharingGroup));
			AllocatedSlot s2_3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jid2, 2, 4), sharingGroup));
			assertNotNull(s1_3);
			assertNotNull(s2_3);
			
			// release all shared and allocate all in non-shared
			s1_2.releaseSlot();
			s2_2.releaseSlot();
			s1_3.releaseSlot();
			s2_3.releaseSlot();
			
			AllocatedSlot sC0 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jidC, 1, 2)));
			AllocatedSlot sC1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertex(jidC, 0, 2)));
			assertNotNull(sC0);
			assertNotNull(sC1);
			
			
			sB0.releaseSlot();
			sB1.releaseSlot();
			sB2.releaseSlot();
			sC0.releaseSlot();
			sC1.releaseSlot();
			
			// test that everything is released
			assertEquals(5, scheduler.getNumberOfAvailableSlots());
			
			// check the scheduler's bookkeeping
			assertEquals(0, scheduler.getNumberOfLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfNonLocalizedAssignments());
			assertEquals(15, scheduler.getNumberOfUnconstrainedAssignments());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that the scheduler assigns the correct existing shared slots
	 */
	@Test
	public void testLocalizedAssignment1() {
		try {
			JobVertexID jid1 = new JobVertexID();
			JobVertexID jid2 = new JobVertexID();
			
			SlotSharingGroup sharingGroup = new SlotSharingGroup(jid1, jid2);
			
			Instance i1 = getRandomInstance(2);
			Instance i2 = getRandomInstance(2);
			
			Scheduler scheduler = new Scheduler();
			scheduler.newInstanceAvailable(i1);
			scheduler.newInstanceAvailable(i2);
			
			
			// schedule one to each instance
			AllocatedSlot s1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid1, 0, 2, i1), sharingGroup));
			AllocatedSlot s2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid1, 1, 2, i2), sharingGroup));
			assertNotNull(s1);
			assertNotNull(s2);
			
			assertEquals(2, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(1, i1.getNumberOfAvailableSlots());
			assertEquals(1, i2.getNumberOfAvailableSlots());
			
			// schedule one from the other group to each instance
			AllocatedSlot s3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid2, 0, 2, i1), sharingGroup));
			AllocatedSlot s4 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid2, 1, 2, i2), sharingGroup));
			assertNotNull(s3);
			assertNotNull(s4);
			
			assertEquals(2, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(1, i1.getNumberOfAvailableSlots());
			assertEquals(1, i2.getNumberOfAvailableSlots());
			
			// check the scheduler's bookkeeping
			assertEquals(4, scheduler.getNumberOfLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfNonLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfUnconstrainedAssignments());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that the scheduler assigns to new local slots, rather than to existing non-local slots
	 */
	@Test
	public void testLocalizedAssignment2() {
		try {
			JobVertexID jid1 = new JobVertexID();
			JobVertexID jid2 = new JobVertexID();
			
			SlotSharingGroup sharingGroup = new SlotSharingGroup(jid1, jid2);
			
			Instance i1 = getRandomInstance(2);
			Instance i2 = getRandomInstance(2);
			
			Scheduler scheduler = new Scheduler();
			scheduler.newInstanceAvailable(i1);
			scheduler.newInstanceAvailable(i2);
			
			
			// schedule one to each instance
			AllocatedSlot s1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid1, 0, 2, i1), sharingGroup));
			AllocatedSlot s2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid1, 1, 2, i1), sharingGroup));
			assertNotNull(s1);
			assertNotNull(s2);
			
			assertEquals(2, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(0, i1.getNumberOfAvailableSlots());
			assertEquals(2, i2.getNumberOfAvailableSlots());
			
			// schedule one from the other group to each instance
			AllocatedSlot s3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid2, 0, 2, i2), sharingGroup));
			AllocatedSlot s4 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid2, 1, 2, i2), sharingGroup));
			assertNotNull(s3);
			assertNotNull(s4);
			
			assertEquals(4, sharingGroup.getTaskAssignment().getNumberOfSlots());
			assertEquals(0, i1.getNumberOfAvailableSlots());
			assertEquals(0, i2.getNumberOfAvailableSlots());
			
			// check the scheduler's bookkeeping
			assertEquals(4, scheduler.getNumberOfLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfNonLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfUnconstrainedAssignments());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that the scheduler can fall back to non-local
	 */
	@Test
	public void testLocalizedAssignment3() {
		try {
			JobVertexID jid1 = new JobVertexID();
			JobVertexID jid2 = new JobVertexID();
			
			SlotSharingGroup sharingGroup = new SlotSharingGroup(jid1, jid2);
			
			Instance i1 = getRandomInstance(2);
			Instance i2 = getRandomInstance(2);
			
			Scheduler scheduler = new Scheduler();
			scheduler.newInstanceAvailable(i1);
			scheduler.newInstanceAvailable(i2);
			
			// schedule until the one instance is full
			AllocatedSlot s1 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid1, 0, 2, i1), sharingGroup));
			AllocatedSlot s2 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid1, 1, 2, i1), sharingGroup));
			AllocatedSlot s3 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid2, 0, 4, i1), sharingGroup));
			AllocatedSlot s4 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid2, 1, 4, i1), sharingGroup));

			// schedule two more with preference of same instance --> need to go to other instance
			AllocatedSlot s5 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid2, 3, 4, i1), sharingGroup));
			AllocatedSlot s6 = scheduler.scheduleImmediately(new ScheduledUnit(getTestVertexWithLocation(jid2, 4, 4, i1), sharingGroup));
			
			assertNotNull(s1);
			assertNotNull(s2);
			assertNotNull(s3);
			assertNotNull(s4);
			assertNotNull(s5);
			assertNotNull(s6);
			
			assertEquals(4, sharingGroup.getTaskAssignment().getNumberOfSlots());
			
			assertEquals(0, i1.getNumberOfAvailableSlots());
			assertEquals(0, i2.getNumberOfAvailableSlots());
			
			assertEquals(i1, s1.getInstance());
			assertEquals(i1, s2.getInstance());
			assertEquals(i1, s3.getInstance());
			assertEquals(i1, s4.getInstance());
			assertEquals(i2, s5.getInstance());
			assertEquals(i2, s6.getInstance());
			
			// check the scheduler's bookkeeping
			assertEquals(4, scheduler.getNumberOfLocalizedAssignments());
			assertEquals(2, scheduler.getNumberOfNonLocalizedAssignments());
			assertEquals(0, scheduler.getNumberOfUnconstrainedAssignments());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
