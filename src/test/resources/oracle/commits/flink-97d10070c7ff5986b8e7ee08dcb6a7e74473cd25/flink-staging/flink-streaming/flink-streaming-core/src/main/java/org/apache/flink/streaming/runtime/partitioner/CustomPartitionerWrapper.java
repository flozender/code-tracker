/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.runtime.partitioner;

import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.runtime.plugable.SerializationDelegate;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

/**
 * Partitioner that selects the channel with a user defined partitioner function on a key.
 *
 * @param <K>
 *            Type of the key
 * @param <T>
 *            Type of the data
 */
public class CustomPartitionerWrapper<K, T> extends StreamPartitioner<T> {
	private static final long serialVersionUID = 1L;

	private int[] returnArray = new int[1];
	Partitioner<K> partitioner;
	KeySelector<T, K> keySelector;

	public CustomPartitionerWrapper(Partitioner<K> partitioner, KeySelector<T, K> keySelector) {
		super(PartitioningStrategy.CUSTOM);
		this.partitioner = partitioner;
		this.keySelector = keySelector;
	}

	@Override
	public int[] selectChannels(SerializationDelegate<StreamRecord<T>> record,
			int numberOfOutputChannels) {

		K key = record.getInstance().getKey(keySelector);

		returnArray[0] = partitioner.partition(key,
				numberOfOutputChannels);

		return returnArray;
	}
}
