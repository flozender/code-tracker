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

package org.apache.flink.runtime.jobmanager;

import org.apache.flink.core.io.StringRecord;
import org.apache.flink.runtime.io.network.api.RecordReader;
import org.apache.flink.runtime.io.network.api.RecordWriter;
import org.apache.flink.runtime.jobgraph.tasks.AbstractInvokable;

/**
 * This task is used during the unit tests to generate a custom exception and check the proper response of the execution
 * engine.
 */
public class ExceptionTask extends AbstractInvokable {

	/**
	 * The test error message included in the thrown exception
	 */
	public static final String ERROR_MESSAGE = "This is an expected test exception";

	/**
	 * The custom exception thrown by the this task.
	 * 
	 */
	public static class TestException extends Exception {

		/**
		 * The generated serial version UID.
		 */
		private static final long serialVersionUID = -974961143742490972L;

		/**
		 * Constructs a new test exception.
		 * 
		 * @param msg
		 *        the error message
		 */
		public TestException(String msg) {
			super(msg);
		}
	}

	@Override
	public void registerInputOutput() {
		new RecordReader<StringRecord>(this, StringRecord.class);
		new RecordWriter<StringRecord>(this);
	}

	@Override
	public void invoke() throws Exception {
		throw new TestException(ERROR_MESSAGE);
	}
}
