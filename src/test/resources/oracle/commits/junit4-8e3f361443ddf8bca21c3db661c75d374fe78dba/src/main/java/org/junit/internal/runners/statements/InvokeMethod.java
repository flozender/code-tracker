/**
 * 
 */
package org.junit.internal.runners.statements;

import org.junit.internal.runners.model.FrameworkMethod;
import org.junit.runners.Statement;

public class InvokeMethod extends Statement {
	private final FrameworkMethod fTestMethod;
	private Object fTarget;
	
	public InvokeMethod(FrameworkMethod testMethod, Object target) {
		fTestMethod= testMethod;
		fTarget= target;
	}
	
	@Override
	public void evaluate() throws Throwable {
		fTestMethod.invokeExplosively(fTarget);
	}
}