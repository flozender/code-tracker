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
package org.apache.flink.api.common.typeutils.base;

import java.io.IOException;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;


public class DoubleSerializer extends TypeSerializer<Double> {

	private static final long serialVersionUID = 1L;
	
	public static final DoubleSerializer INSTANCE = new DoubleSerializer();
	
	private static final Double ZERO = Double.valueOf(0);

	
	@Override
	public boolean isImmutableType() {
		return true;
	}

	@Override
	public boolean isStateful() {
		return false;
	}

	@Override
	public Double createInstance() {
		return ZERO;
	}

	@Override
	public Double copy(Double from, Double reuse) {
		return from;
	}

	@Override
	public int getLength() {
		return 8;
	}

	@Override
	public void serialize(Double record, DataOutputView target) throws IOException {
		target.writeDouble(record.doubleValue());
	}

	@Override
	public Double deserialize(Double reuse, DataInputView source) throws IOException {
		return Double.valueOf(source.readDouble());
	}

	@Override
	public void copy(DataInputView source, DataOutputView target) throws IOException {
		target.writeDouble(source.readDouble());
	}
}
