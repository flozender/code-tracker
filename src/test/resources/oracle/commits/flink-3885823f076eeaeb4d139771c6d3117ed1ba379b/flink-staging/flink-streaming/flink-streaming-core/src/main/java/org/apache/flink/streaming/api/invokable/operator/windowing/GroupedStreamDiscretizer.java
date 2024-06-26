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

package org.apache.flink.streaming.api.invokable.operator.windowing;

import java.util.HashMap;
import java.util.Map;

import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.invokable.StreamInvokable;
import org.apache.flink.streaming.api.windowing.policy.CloneableEvictionPolicy;
import org.apache.flink.streaming.api.windowing.policy.CloneableTriggerPolicy;

public class GroupedStreamDiscretizer<IN> extends StreamInvokable<IN, StreamWindow<IN>> {

	/**
	 * Auto-generated serial version UID
	 */
	private static final long serialVersionUID = -3469545957144404137L;

	protected KeySelector<IN, ?> keySelector;
	protected Configuration parameters;
	protected CloneableTriggerPolicy<IN> triggerPolicy;
	protected CloneableEvictionPolicy<IN> evictionPolicy;
	protected WindowBuffer<IN> windowBuffer;

	protected Map<Object, StreamDiscretizer<IN>> groupedDiscretizers;

	public GroupedStreamDiscretizer(KeySelector<IN, ?> keySelector,
			CloneableTriggerPolicy<IN> triggerPolicy, CloneableEvictionPolicy<IN> evictionPolicy,
			WindowBuffer<IN> windowBuffer) {

		super(null);

		this.keySelector = keySelector;

		this.triggerPolicy = triggerPolicy;
		this.evictionPolicy = evictionPolicy;

		this.groupedDiscretizers = new HashMap<Object, StreamDiscretizer<IN>>();
		this.windowBuffer = windowBuffer;

	}

	@Override
	public void invoke() throws Exception {
		if (readNext() == null) {
			throw new RuntimeException("DataStream must not be empty");
		}

		while (nextRecord != null) {

			Object key = keySelector.getKey(nextObject);

			StreamDiscretizer<IN> groupDiscretizer = groupedDiscretizers.get(key);

			if (groupDiscretizer == null) {
				groupDiscretizer = makeNewGroup(key);
				groupedDiscretizers.put(key, groupDiscretizer);
			}

			groupDiscretizer.processRealElement(nextObject);

			readNext();
		}

		for (StreamDiscretizer<IN> group : groupedDiscretizers.values()) {
			group.emitWindow();
		}

	}

	/**
	 * This method creates a new group. The method gets called in case an
	 * element arrives which has a key which was not seen before. The method
	 * created a nested {@link StreamDiscretizer} and therefore created clones
	 * of all distributed trigger and eviction policies.
	 * 
	 * @param key
	 *            The key of the new group.
	 */
	protected StreamDiscretizer<IN> makeNewGroup(Object key) throws Exception {

		StreamDiscretizer<IN> groupDiscretizer = new StreamDiscretizer<IN>(triggerPolicy.clone(),
				evictionPolicy.clone(), windowBuffer.clone());

		groupDiscretizer.collector = taskContext.getOutputCollector();
		groupDiscretizer.open(this.parameters);

		return groupDiscretizer;
	}

}
