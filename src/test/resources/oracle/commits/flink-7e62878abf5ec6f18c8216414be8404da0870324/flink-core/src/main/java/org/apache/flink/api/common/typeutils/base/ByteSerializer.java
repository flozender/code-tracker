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


public class ByteSerializer extends TypeSerializer<Byte> {

	private static final long serialVersionUID = 1L;
	
	public static final ByteSerializer INSTANCE = new ByteSerializer();
	
	private static final Byte ZERO = Byte.valueOf((byte) 0);


	@Override
	public boolean isImmutableType() {
		return true;
	}

	@Override
	public boolean isStateful() {
		return false;
	}
	
	@Override
	public Byte createInstance() {
		return ZERO;
	}

	@Override
	public Byte copy(Byte from, Byte reuse) {
		return from;
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public void serialize(Byte record, DataOutputView target) throws IOException {
		target.writeByte(record.byteValue());
	}

	@Override
	public Byte deserialize(Byte reuse, DataInputView source) throws IOException {
		return Byte.valueOf(source.readByte());
	}

	@Override
	public void copy(DataInputView source, DataOutputView target) throws IOException {
		target.writeByte(source.readByte());
	}
}
