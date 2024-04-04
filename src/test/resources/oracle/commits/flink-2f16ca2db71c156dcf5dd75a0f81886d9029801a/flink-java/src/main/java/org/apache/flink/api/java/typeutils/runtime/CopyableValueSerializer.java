/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.api.java.typeutils.runtime;

import java.io.IOException;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.types.CopyableValue;
import org.apache.flink.util.InstantiationUtil;


public class CopyableValueSerializer<T extends CopyableValue<T>> extends TypeSerializer<T> {

	private static final long serialVersionUID = 1L;
	
	
	private final Class<T> valueClass;
	
	private transient T instance;
	
	
	public CopyableValueSerializer(Class<T> valueClass) {
		this.valueClass = valueClass;
	}

	@Override
	public boolean isImmutableType() {
		return false;
	}

	@Override
	public boolean isStateful() {
		return false;
	}

	@Override
	public T createInstance() {
		return InstantiationUtil.instantiate(this.valueClass);
	}
	
	@Override
	public T copy(T from) {
		return copy(from, createInstance());
	}
	
	@Override
	public T copy(T from, T reuse) {
		from.copyTo(reuse);
		return reuse;
	}

	@Override
	public int getLength() {
		ensureInstanceInstantiated();
		return instance.getBinaryLength();
	}

	@Override
	public void serialize(T value, DataOutputView target) throws IOException {
		value.write(target);
	}

	@Override
	public T deserialize(DataInputView source) throws IOException {
		return deserialize(createInstance(), source);
	}
	
	@Override
	public T deserialize(T reuse, DataInputView source) throws IOException {
		reuse.read(source);
		return reuse;
	}

	@Override
	public void copy(DataInputView source, DataOutputView target) throws IOException {
		ensureInstanceInstantiated();
		instance.copy(source, target);
	}
	
	// --------------------------------------------------------------------------------------------
	
	private void ensureInstanceInstantiated() {
		if (instance == null) {
			instance = createInstance();
		}
	}
	
	@Override
	public int hashCode() {
		return this.valueClass.hashCode() + 9231;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj.getClass() == CopyableValueSerializer.class) {
			CopyableValueSerializer<?> other = (CopyableValueSerializer<?>) obj;
			return this.valueClass == other.valueClass;
		} else {
			return false;
		}
	}
}
