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

package eu.stratosphere.pact.runtime.plugable;

import java.io.IOException;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.core.io.IOReadableWritable;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;


public class SerializationDelegate<T> implements IOReadableWritable {
	
	private T instance;
	
	private final TypeSerializer<T> serializer;
	

	public SerializationDelegate(TypeSerializer<T> serializer) {
		this.serializer = serializer;
	}
	
	public void setInstance(T instance) {
		this.instance = instance;
	}
	
	public T getInstance() {
		return this.instance;
	}
	
	@Override
	public void write(DataOutputView out) throws IOException {
		this.serializer.serialize(this.instance, out);
	}

	@Override
	public void read(DataInputView in) throws IOException {
		throw new IllegalStateException("Deserialization method called on SerializationDelegate.");
	}
}
