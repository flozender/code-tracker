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

package eu.stratosphere.nephele.example.union;

import eu.stratosphere.core.io.StringRecord;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractFileInputTask;

public class ProducerTask extends AbstractFileInputTask {

	private static final int NUMBER_OF_RECORDS_TO_PRODUCE = 1000000;

	private RecordWriter<StringRecord> output;

	@Override
	public void registerInputOutput() {

		this.output = new RecordWriter<StringRecord>(this, StringRecord.class);
	}

	@Override
	public void invoke() throws Exception {

		for (int i = 0; i < NUMBER_OF_RECORDS_TO_PRODUCE; ++i) {

			final StringRecord record = new StringRecord("Record " + i + " of " + this);
			this.output.emit(record);
		}

	}

}
