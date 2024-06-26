/***********************************************************************************************************************
 * Copyright (C) 2010-2014 by the Apache Flink project (http://flink.incubator.apache.org)
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

package org.apache.flink.runtime.io.network;

import org.apache.flink.core.memory.MemorySegment;

public interface BufferRecycler {

	/**
	 * Called by {@link org.apache.flink.runtime.io.network.Buffer} to return a {@link MemorySegment} to its original buffer pool.
	 *
	 * @param buffer the segment to be recycled
	 */
	void recycle(MemorySegment buffer);
}