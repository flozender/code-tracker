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

package org.apache.flink.runtime.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class EnvironmentInformationTest {

	@Test
	public void testJavaMemory() {
		try {
			long fullHeap = EnvironmentInformation.getMaxJvmHeapMemory();
			long free = EnvironmentInformation.getSizeOfFreeHeapMemory();
			long freeWithGC = EnvironmentInformation.getSizeOfFreeHeapMemoryWithDefrag();
			
			assertTrue(fullHeap > 0);
			assertTrue(free > 0);
			assertTrue(freeWithGC > 0);
			assertTrue(free <= fullHeap);
			assertTrue(freeWithGC <= fullHeap);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testEnvironmentMethods() {
		try {
			assertNotNull(EnvironmentInformation.getJvmStartupOptions());
			assertNotNull(EnvironmentInformation.getJvmVersion());
			assertNotNull(EnvironmentInformation.getRevisionInformation());
			assertNotNull(EnvironmentInformation.getVersion());
			assertNotNull(EnvironmentInformation.getUserRunning());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
