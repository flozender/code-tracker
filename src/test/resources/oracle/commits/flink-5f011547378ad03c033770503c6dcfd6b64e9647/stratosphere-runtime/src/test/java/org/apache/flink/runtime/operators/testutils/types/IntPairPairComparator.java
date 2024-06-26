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

package org.apache.flink.runtime.operators.testutils.types;

import org.apache.flink.api.common.typeutils.TypePairComparator;


public class IntPairPairComparator extends TypePairComparator<IntPair, IntPair> {
	
	private int key;
	

	@Override
	public void setReference(IntPair reference) {
		this.key = reference.getKey();
	}

	@Override
	public boolean equalToReference(IntPair candidate) {
		return this.key == candidate.getKey();
	}

	@Override
	public int compareToReference(IntPair candidate) {
		return candidate.getKey() - this.key;
	}
}
