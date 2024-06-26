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

package org.apache.flink.api.java.typeutils.runtime;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.TypeSerializerFactory;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.InstantiationUtil;

public final class RuntimeStatelessSerializerFactory<T> implements TypeSerializerFactory<T>, java.io.Serializable {

	private static final long serialVersionUID = 1L;
	

	private static final String CONFIG_KEY_SER = "SER_DATA";

	private static final String CONFIG_KEY_CLASS = "CLASS_DATA";

	
	private TypeSerializer<T> serializer;

	private Class<T> clazz;


	public RuntimeStatelessSerializerFactory() {}

	public RuntimeStatelessSerializerFactory(TypeSerializer<T> serializer, Class<T> clazz) {
		if (serializer == null || clazz == null) {
			throw new NullPointerException();
		}
		
		if (serializer.isStateful()) {
			throw new IllegalArgumentException("Cannot use the stateless serializer factory with a stateful serializer.");
		}
		
		this.clazz = clazz;
		this.serializer = serializer;
	}


	@Override
	public void writeParametersToConfig(Configuration config) {
		try {
			InstantiationUtil.writeObjectToConfig(clazz, config, CONFIG_KEY_CLASS);
			InstantiationUtil.writeObjectToConfig(serializer, config, CONFIG_KEY_SER);
		}
		catch (Exception e) {
			throw new RuntimeException("Could not serialize serializer into the configuration.", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readParametersFromConfig(Configuration config, ClassLoader cl) throws ClassNotFoundException {
		if (config == null || cl == null) {
			throw new NullPointerException();
		}
		
		try {
			this.clazz = (Class<T>) InstantiationUtil.readObjectFromConfig(config, CONFIG_KEY_CLASS, cl);
			this.serializer = (TypeSerializer<T>)  InstantiationUtil.readObjectFromConfig(config, CONFIG_KEY_SER, cl);
		}
		catch (ClassNotFoundException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException("Could not load deserializer from the configuration.", e);
		}
	}

	@Override
	public TypeSerializer<T> getSerializer() {
		if (this.serializer != null) {
			return this.serializer;
		} else {
			throw new RuntimeException("SerializerFactory has not been initialized from configuration.");
		}
	}

	@Override
	public Class<T> getDataType() {
		return clazz;
	}
	
	// --------------------------------------------------------------------------------------------
	
	@Override
	public int hashCode() {
		return clazz.hashCode() ^ serializer.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof RuntimeStatelessSerializerFactory) {
			RuntimeStatelessSerializerFactory<?> other = (RuntimeStatelessSerializerFactory<?>) obj;
			
			return this.clazz == other.clazz &&
					this.serializer.equals(other.serializer);
		} else {
			return false;
		}
	}
}
