/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.nephele.executiongraph;

import eu.stratosphere.nephele.io.RecordReader;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractTask;
import eu.stratosphere.nephele.types.StringRecord;

public class GrepTask2Inputs1Output extends AbstractTask {

	private RecordReader<StringRecord> input1 = null;

	private RecordReader<StringRecord> input2 = null;

	private RecordWriter<StringRecord> output = null;

	@Override
	public void invoke() throws Exception {

		while (this.input1.hasNext()) {

			StringRecord s = input1.next();
			this.output.emit(s);
		}

		while (this.input2.hasNext()) {

			try {
				StringRecord s = input2.next();
				this.output.emit(s);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void registerInputOutput() {
		this.input1 = new RecordReader<StringRecord>(this, StringRecord.class, null);
		this.input2 = new RecordReader<StringRecord>(this, StringRecord.class, null);
		this.output = new RecordWriter<StringRecord>(this, StringRecord.class);
	}
}
