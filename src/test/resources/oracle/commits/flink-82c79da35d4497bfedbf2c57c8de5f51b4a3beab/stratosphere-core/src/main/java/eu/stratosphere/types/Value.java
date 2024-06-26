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

package eu.stratosphere.types;

import java.io.Serializable;

import eu.stratosphere.core.io.IOReadableWritable;

/**
 * This interface has to be implemented by all data types that act as values. Values are consumed
 * and produced by user functions (PACT stubs) that run inside PACTs.
 * <p>
 * This interface extends {@link eu.stratosphere.nephele.types.Record} and requires to implement
 * the serialization of its value.
 * 
 * @see eu.stratosphere.core.io.IOReadableWritable
 */
public interface Value extends IOReadableWritable, Serializable {
}
