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

package eu.stratosphere.pact.runtime.task.util;

import java.io.Closeable;
import java.io.IOException;

import org.apache.flink.util.MutableObjectIterator;

/**
 * Utility interface for a provider of an input that can be closed.
 */
public interface CloseableInputProvider<E> extends Closeable
{
	/**
	 * Gets the iterator over this input.
	 * 
	 * @return The iterator provided by this iterator provider.
	 * @throws InterruptedException 
	 */
	public MutableObjectIterator<E> getIterator() throws InterruptedException, IOException;
}
