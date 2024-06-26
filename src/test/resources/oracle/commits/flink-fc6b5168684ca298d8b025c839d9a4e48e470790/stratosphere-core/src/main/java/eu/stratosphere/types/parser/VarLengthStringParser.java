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

import eu.stratosphere.types.PactString;

/**
 * Converts a variable length field of a byte array into a {@link PactString}. The byte contents between
 * delimiters is interpreted as an ASCII string. The string may be quotes in double quotes. For quoted
 * strings, whitespaces (space and tab) leading and trailing before and after the quotes are removed.
 * 
 * @see PactString
 */
public class VarLengthStringParser extends FieldParser<PactString> {

	private static final byte WHITESPACE_SPACE = (byte) ' ';
	private static final byte WHITESPACE_TAB = (byte) '\t';
	
	private static final byte QUOTE_DOUBLE = (byte) '"';
	
	
	@Override
	public int parseField(byte[] bytes, int startPos, int length, char delim, PactString field) {
		
		int i = startPos;
		
		final byte delByte = (byte) delim;
		byte current;
		
		// count initial whitespace lines
		while (i < length && ((current = bytes[i]) == WHITESPACE_SPACE || current == WHITESPACE_TAB)) {
			i++;
		}
		
		// first none whitespace character
		if (i < length && bytes[i] == QUOTE_DOUBLE) {
			// quoted string
			i++; // the quote
			
			// we count only from after the quote
			int quoteStart = i;
			while (i < length && bytes[i] != QUOTE_DOUBLE) {
				i++;
			}
			
			if (i < length) {
				// end of the string
				field.setValueAscii(bytes, quoteStart, i-quoteStart);
				
				i++; // the quote
				
				// skip trailing whitespace characters 
				while (i < length && (current = bytes[i]) != delByte) {
					if (current == WHITESPACE_SPACE || current == WHITESPACE_TAB)
						i++;
					else
						return -1;	// illegal case of non-whitespace characters trailing
				}
				
				return (i == length ? length : i+1);
			} else {
				// exited due to line end without quote termination
				return -1;
			}
		}
		else {
			// unquoted string
			while (i < length && bytes[i] != delByte) {
				i++;
			}
			
			// set from the beginning. unquoted strings include the leading whitespaces
			field.setValueAscii(bytes, startPos, i-startPos);
			return (i == length ? length : i+1);
		}
	}
	
	@Override
	public PactString createValue() {
		return new PactString();
	}
}