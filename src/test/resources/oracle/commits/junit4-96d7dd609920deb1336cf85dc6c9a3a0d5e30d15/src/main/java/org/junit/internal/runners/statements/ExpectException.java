/**
 * 
 */
package org.junit.internal.runners.statements;

import org.junit.runners.Statement;


public class ExpectException extends Statement {
	private Statement fNext;
	private final Class<? extends Throwable> fExpected;
	
	public ExpectException(Statement next, Class<? extends Throwable> expected) {
		fNext= next;
		fExpected= expected;
	}
	
	@Override
	public void evaluate() throws Exception {
		boolean complete = false;
		try {
			fNext.evaluate();
			complete = true;
		} catch (Throwable e) {
			if (!fExpected.isAssignableFrom(e.getClass())) {
				String message= "Unexpected exception, expected<"
							+ fExpected.getName() + "> but was<"
							+ e.getClass().getName() + ">";
				throw new Exception(message, e);
			}
		}
		if (complete)
			throw new AssertionError("Expected exception: "
					+ fExpected.getName());
	}
}