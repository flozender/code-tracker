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

package eu.stratosphere.types.parser;

import eu.stratosphere.types.PactByte;

/**
 * Parses a decimal text field into a PactInteger.
 * Only characters '1' to '0' and '-' are allowed.
 * The parser does not check for the maximum value.
 */
public class DecimalTextByteParser extends FieldParser<PactByte> {

	@Override
	public int parseField(byte[] bytes, int startPos, int limit, char delim, PactByte field) {
		long val = 0;
		boolean neg = false;
		
		if (bytes[startPos] == '-') {
			neg = true;
			startPos++;
		}
		
		for (int i = startPos; i < limit; i++) {
			if (bytes[i] == delim) {
				return valueSet(field, val, neg, i+1);
			}
			if (bytes[i] < 48 || bytes[i] > 57) {
				return -1;
			}
			val *= 10;
			val += bytes[i] - 48;
		}
		
		return valueSet(field, val, neg, limit);
	}
	
	private final int valueSet(PactByte field, long val, boolean negative, int position) {
		if (negative) {
			if (val >= Byte.MIN_VALUE) {
				field.setValue((byte) -val);
			} else {
				return -1;
			}
		} else {
			if (val <= Byte.MAX_VALUE) {
				field.setValue((byte) val);
			} else {
				return -1;
			}
		}
		return position;
	}
	
	@Override
	public PactByte createValue() {
		return new PactByte();
	}
}
