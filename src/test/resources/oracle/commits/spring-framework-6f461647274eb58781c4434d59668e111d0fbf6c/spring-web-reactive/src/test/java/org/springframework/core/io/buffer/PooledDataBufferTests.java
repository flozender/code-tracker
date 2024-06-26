/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io.buffer;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 */
@RunWith(Parameterized.class)
public class PooledDataBufferTests {

	@Parameterized.Parameter
	public DataBufferAllocator allocator;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] buffers() {

		return new Object[][]{
				{new NettyDataBufferAllocator(new UnpooledByteBufAllocator(true))},
				{new NettyDataBufferAllocator(new UnpooledByteBufAllocator(false))},
				{new NettyDataBufferAllocator(new PooledByteBufAllocator(true))},
				{new NettyDataBufferAllocator(new PooledByteBufAllocator(false))}};
	}

	private PooledDataBuffer createDataBuffer(int capacity) {
		return (PooledDataBuffer) allocator.allocateBuffer(capacity);
	}

	@Test
	public void retainAndRelease() {
		PooledDataBuffer buffer = createDataBuffer(1);
		buffer.write((byte) 'a');

		buffer.retain();
		boolean result = buffer.release();
		assertFalse(result);
		result = buffer.release();
		assertTrue(result);
	}

	@Test(expected = IllegalStateException.class)
	public void tooManyReleases() {
		PooledDataBuffer buffer = createDataBuffer(1);
		buffer.write((byte) 'a');

		buffer.release();
		buffer.release();
	}


}