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
package org.apache.flink.api.java.typeutils.runtime;

import java.io.IOException;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.types.Value;
import org.apache.flink.util.InstantiationUtil;

import com.esotericsoftware.kryo.Kryo;

/**
 * Serializer for {@link Value} types. Uses the value's serialization methods, and uses
 * Kryo for deep object copies.
 *
 * @param <T> The type serialized.
 */
public class ValueSerializer<T extends Value> extends TypeSerializer<T> {

	private static final long serialVersionUID = 1L;
	
	private final Class<T> type;
	
	private transient Kryo kryo;
	
	private transient T copyInstance;
	
	// --------------------------------------------------------------------------------------------
	
	public ValueSerializer(Class<T> type) {
		if (type == null) {
			throw new NullPointerException();
		}
		
		this.type = type;
	}

	// --------------------------------------------------------------------------------------------
	
	@Override
	public boolean isImmutableType() {
		return false;
	}

	@Override
	public boolean isStateful() {
		return true;
	}
	
	@Override
	public T createInstance() {
		return InstantiationUtil.instantiate(this.type);
	}

	@Override
	public T copy(T from, T reuse) {
		checkKryoInitialized();
		reuse = this.kryo.copy(from);
		return reuse;
	}

	@Override
	public int getLength() {
		return -1;
	}

	@Override
	public void serialize(T value, DataOutputView target) throws IOException {
		value.write(target);
	}

	@Override
	public T deserialize(T reuse, DataInputView source) throws IOException {
		reuse.read(source);
		return reuse;
	}

	@Override
	public void copy(DataInputView source, DataOutputView target) throws IOException {
		if (this.copyInstance == null) {
			this.copyInstance = InstantiationUtil.instantiate(type);
		}
		
		this.copyInstance.read(source);
		this.copyInstance.write(target);
	}
	
	private final void checkKryoInitialized() {
		if (this.kryo == null) {
			this.kryo = new Kryo();
			this.kryo.setAsmEnabled(true);
			this.kryo.register(type);
		}
	}
}
