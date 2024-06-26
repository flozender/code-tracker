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

package eu.stratosphere.nephele.template;

import java.util.Iterator;
import java.util.NoSuchElementException;

import eu.stratosphere.core.io.InputSplit;

/**
 * Abstract base class for tasks submitted as a part of a job input vertex.
 * 
 * @param <T>
 *        the type of input splits generated by this input task
 */
public abstract class AbstractInputTask<T extends InputSplit> extends AbstractInvokable {

	/**
	 * Returns an iterator to a (possible empty) list of input splits which is expected to be consumed by this
	 * instance of the {@link AbstractInputTask}.
	 * 
	 * @return an iterator to a (possible empty) list of input splits.
	 */
	public Iterator<T> getInputSplits() {

		final InputSplitProvider provider = getEnvironment().getInputSplitProvider();

		return new Iterator<T>() {

			private T nextSplit;

			@Override
			public boolean hasNext() {

				if (this.nextSplit == null) {

					final InputSplit split = provider.getNextInputSplit();
					if (split != null) {
						@SuppressWarnings("unchecked")
						final T tSplit = (T) split;
						this.nextSplit = tSplit;
						return true;
					} else {
						return false;
					}
				} else {
					return true;
				}
			}

			@Override
			public T next() {
				if (this.nextSplit == null && !hasNext()) {
					throw new NoSuchElementException();
				}

				final T tmp = this.nextSplit;
				this.nextSplit = null;
				return tmp;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
