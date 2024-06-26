/***********************************************************************************************************************
 * Copyright (C) 2010-2013 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.pact.runtime.io;

import eu.stratosphere.api.typeutils.TypeSerializer;
import eu.stratosphere.core.memory.DataInputView;
import eu.stratosphere.util.MutableObjectIterator;

import java.io.EOFException;
import java.io.IOException;

public class InputViewIterator<E> implements MutableObjectIterator<E>
{
	private final DataInputView inputView;

	private final TypeSerializer<E> serializer;

	public InputViewIterator(DataInputView inputView, TypeSerializer<E> serializer) {
		this.inputView = inputView;
		this.serializer = serializer;
	}

	@Override
	public boolean next(E target) throws IOException {
		try {
			this.serializer.deserialize(target, this.inputView);
			return true;
		} catch (EOFException e) {
			return false;
		}
	}
}
