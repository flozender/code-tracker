/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.pact.runtime.test.util;

import eu.stratosphere.types.PactInteger;
import eu.stratosphere.types.PactRecord;
import eu.stratosphere.util.MutableObjectIterator;

public class UniformPactRecordGenerator implements MutableObjectIterator<PactRecord> {

	private final PactInteger key = new PactInteger();
	private final PactInteger value = new PactInteger();
	
	int numKeys;
	int numVals;
	
	int keyCnt = 0;
	int valCnt = 0;
	int startKey = 0;
	int startVal = 0;
	boolean repeatKey;
	
	public UniformPactRecordGenerator(int numKeys, int numVals, boolean repeatKey) {
		this(numKeys, numVals, 0, 0, repeatKey);
	}
	
	public UniformPactRecordGenerator(int numKeys, int numVals, int startKey, int startVal, boolean repeatKey) {
		this.numKeys = numKeys;
		this.numVals = numVals;
		this.startKey = startKey;
		this.startVal = startVal;
		this.repeatKey = repeatKey;
	}

	@Override
	public boolean next(PactRecord target) {
		if(!repeatKey) {
			if(valCnt >= numVals+startVal) {
				return false;
			}
			
			key.setValue(keyCnt++);
			value.setValue(valCnt);
			
			if(keyCnt == numKeys+startKey) {
				keyCnt = startKey;
				valCnt++;
			}
		} else {
			if(keyCnt >= numKeys+startKey) {
				return false;
			}
			key.setValue(keyCnt);
			value.setValue(valCnt++);
			
			if(valCnt == numVals+startVal) {
				valCnt = startVal;
				keyCnt++;
			}
		}
		
		target.setField(0, this.key);
		target.setField(1, this.value);
		target.updateBinaryRepresenation();
		return true;
	}
}
