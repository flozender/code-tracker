/***********************************************************************************************************************
 *
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
 *
 **********************************************************************************************************************/

package org.apache.flink.api.java.io;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.flink.api.common.io.GenericInputFormat;
import org.apache.flink.api.common.io.NonParallelInput;

/**
 * An input format that returns objects from an iterator.
 */
public class IteratorInputFormat<T> extends GenericInputFormat<T> implements NonParallelInput {

	private static final long serialVersionUID = 1L;

	private Iterator<T> iterator; // input data as serializable iterator
	
	
	public IteratorInputFormat(Iterator<T> iterator) {
		if (!(iterator instanceof Serializable)) {
			throw new IllegalArgumentException("The data source iterator must be serializable.");
		}
		
		this.iterator = iterator;
	}

	@Override
	public boolean reachedEnd() {
		return !this.iterator.hasNext();
	}

	@Override
	public T nextRecord(T record) {
		return this.iterator.next();
	}
}
