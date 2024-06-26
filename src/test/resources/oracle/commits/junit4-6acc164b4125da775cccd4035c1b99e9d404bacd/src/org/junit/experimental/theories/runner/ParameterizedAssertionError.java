/**
 * 
 */
package org.junit.experimental.theories.runner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;


public class ParameterizedAssertionError extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ParameterizedAssertionError(Throwable targetException,
			String methodName, Object... params) {
		super(String.format("%s(%s)", methodName, join(", ", params)),
				targetException);
	}

	@Override public boolean equals(Object obj) {
		return toString().equals(obj.toString());
	}

	public static String join(String delimiter, Object... params) {
		return join(delimiter, Arrays.asList(params));
	}

	public static String join(String delimiter,
			Collection<Object> values) {
		StringBuffer buffer = new StringBuffer();
		Iterator<Object> iter = values.iterator();
		while (iter.hasNext()) {
			Object next = iter.next();
			buffer.append(String.valueOf(next));
			if (iter.hasNext()) {
				buffer.append(delimiter);
			}
		}
		return buffer.toString();
	}
}