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

package org.apache.flink.streaming.api.streamcomponent;

import java.io.IOException;

import org.apache.flink.streaming.api.streamrecord.StreamRecord;

import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.typeutils.runtime.TupleSerializer;
import org.apache.flink.pact.runtime.plugable.DeserializationDelegate;
import org.apache.flink.runtime.io.api.AbstractUnionRecordReader;
import org.apache.flink.runtime.io.api.MutableRecordReader;
import org.apache.flink.runtime.io.api.Reader;

public final class UnionStreamRecordReader extends AbstractUnionRecordReader<StreamRecord>
		implements Reader<StreamRecord> {

	private final Class<? extends StreamRecord> recordType;

	private StreamRecord lookahead;
	private DeserializationDelegate<Tuple> deserializationDelegate;
	private TupleSerializer<Tuple> tupleSerializer;

	public UnionStreamRecordReader(MutableRecordReader<StreamRecord>[] recordReaders, Class<? extends StreamRecord> recordType,
			DeserializationDelegate<Tuple> deserializationDelegate,
			TupleSerializer<Tuple> tupleSerializer) {
		super(recordReaders);
		this.recordType = recordType;
		this.deserializationDelegate = deserializationDelegate;
		this.tupleSerializer = tupleSerializer;
	}

	@Override
	public boolean hasNext() throws IOException, InterruptedException {
		if (this.lookahead != null) {
			return true;
		} else {
			StreamRecord record = instantiateRecordType();
			record.setDeseralizationDelegate(deserializationDelegate, tupleSerializer);
			if (getNextRecord(record)) {
				this.lookahead = record;
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public StreamRecord next() throws IOException, InterruptedException {
		if (hasNext()) {
			StreamRecord tmp = this.lookahead;
			this.lookahead = null;
			return tmp;
		} else {
			return null;
		}
	}

	private StreamRecord instantiateRecordType() {
		try {
			return this.recordType.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Cannot instantiate class '" + this.recordType.getName()
					+ "'.", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Cannot instantiate class '" + this.recordType.getName()
					+ "'.", e);
		}
	}
}
