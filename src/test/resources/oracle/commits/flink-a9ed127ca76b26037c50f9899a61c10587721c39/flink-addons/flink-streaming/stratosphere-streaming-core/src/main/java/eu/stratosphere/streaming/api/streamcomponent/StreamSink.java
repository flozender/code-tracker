/***********************************************************************************************************************
 *
 * Copyright (C) 2010-2014 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.streaming.api.streamcomponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.stratosphere.api.java.tuple.Tuple;
import eu.stratosphere.runtime.io.api.AbstractRecordReader;
import eu.stratosphere.streaming.api.invokable.DefaultSinkInvokable;
import eu.stratosphere.streaming.api.invokable.StreamRecordInvokable;
import eu.stratosphere.streaming.api.invokable.UserSinkInvokable;

public class StreamSink extends AbstractStreamComponent {

	private static final Log log = LogFactory.getLog(StreamSink.class);

	private AbstractRecordReader inputs;
	private StreamRecordInvokable<Tuple, Tuple> userFunction;

	public StreamSink() {
		// TODO: Make configuration file visible and call setClassInputs() here
		userFunction = null;
	}

	@Override
	public void registerInputOutput() {
		initialize();
		
		try {
			setSerializers();
			setSinkSerializer();
			inputs = getConfigInputs();
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("Cannot register inputs", e);
			}
		}

		// FaultToleranceType faultToleranceType =
		// FaultToleranceType.from(taskConfiguration
		// .getInteger("faultToleranceType", 0));

		setInvokable();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void setInvokable() {
		Class<? extends UserSinkInvokable> userFunctionClass = configuration.getClass(
				"userfunction", DefaultSinkInvokable.class, UserSinkInvokable.class);
		userFunction = (UserSinkInvokable<Tuple>) getInvokable(userFunctionClass);
	}

	@Override
	public void invoke() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("SINK " + name + " invoked");
		}

		invokeRecords(userFunction, inputs);

		if (log.isDebugEnabled()) {
			log.debug("SINK " + name + " invoke finished");
		}
	}

}
