/**
 * 
 */
package org.junit.internal.runners.links;

import org.junit.Assume.AssumptionViolatedException;
import org.junit.internal.runners.model.Roadie;

public class NoExpectedException extends Link {
	Link fNext;
	public NoExpectedException(Link next) {
		fNext= next;
	}
	
	@Override
	public void run(Roadie context) throws Throwable {
		try {
			fNext.run(context);
		} catch (AssumptionViolatedException e) {
			// Do nothing
		}
	}
}