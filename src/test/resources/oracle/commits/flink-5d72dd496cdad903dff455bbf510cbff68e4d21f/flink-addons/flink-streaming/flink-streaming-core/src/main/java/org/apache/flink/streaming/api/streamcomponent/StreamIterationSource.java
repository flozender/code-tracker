/**
 *
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
 *
 */

package org.apache.flink.streaming.api.streamcomponent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.runtime.io.network.api.RecordWriter;
import org.apache.flink.streaming.api.streamrecord.StreamRecord;

public class StreamIterationSource<OUT extends Tuple> extends AbstractStreamComponent<Tuple, OUT> {

	private static final Log LOG = LogFactory.getLog(StreamIterationSource.class);

	private List<RecordWriter<StreamRecord<OUT>>> outputs;
	private static int numSources;
	private int[] numberOfOutputChannels;
	private String iterationId;
	@SuppressWarnings("rawtypes")
	private BlockingQueue<StreamRecord> dataChannel;

	@SuppressWarnings("rawtypes")
	public StreamIterationSource() {

		outputs = new LinkedList<RecordWriter<StreamRecord<OUT>>>();
		numSources = newComponent();
		instanceID = numSources;
		dataChannel = new ArrayBlockingQueue<StreamRecord>(1);
	}

	@Override
	public void registerInputOutput() {
		initialize();

		try {
			setSerializers();
			setConfigOutputs(outputs);
		} catch (StreamComponentException e) {
			throw new StreamComponentException("Cannot register outputs", e);
		}

		numberOfOutputChannels = new int[outputs.size()];
		for (int i = 0; i < numberOfOutputChannels.length; i++) {
			numberOfOutputChannels[i] = configuration.getInteger("channels_" + i, 0);
		}

		iterationId = configuration.getString("iteration-id", "iteration-0");
		try {
			BlockingQueueBroker.instance().handIn(iterationId, dataChannel);
		} catch (Exception e) {

		}
	}

	@Override
	public void invoke() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("SOURCE " + name + " invoked with instance id " + instanceID);
		}

		for (RecordWriter<StreamRecord<OUT>> output : outputs) {
			output.initializeSerializers();
		}

		while (true) {
			@SuppressWarnings("unchecked")
			StreamRecord<OUT> nextRecord = dataChannel.poll(3, TimeUnit.SECONDS);
			if (nextRecord == null) {
				break;
			}
			nextRecord.setSeralizationDelegate(this.outSerializationDelegate);
			for (RecordWriter<StreamRecord<OUT>> output : outputs) {
				output.emit(nextRecord);
				// output.flush();
			}
		}

		for (RecordWriter<StreamRecord<OUT>> output : outputs) {
			output.flush();
		}

	}

	@Override
	protected void setInvokable() {
	}

}
