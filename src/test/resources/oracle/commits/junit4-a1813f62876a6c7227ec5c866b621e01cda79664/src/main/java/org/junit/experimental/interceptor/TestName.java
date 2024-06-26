package org.junit.experimental.interceptor;

import org.junit.runners.model.FrameworkMethod;

/**
 * The TestName Rule makes the current test name available inside test methods:
 * 
 * <pre>
 * public class TestNameTest {
 * 	&#064;Rule
 * 	public TestName name= new TestName();
 * 
 * 	&#064;Test
 * 	public void testA() {
 * 		assertEquals(&quot;testA&quot;, name.getMethodName());
 * 	}
 * 
 * 	&#064;Test
 * 	public void testB() {
 * 		assertEquals(&quot;testB&quot;, name.getMethodName());
 * 	}
 * }
 * </pre>
 */
public class TestName extends TestWatchman {
	private String fName;

	@Override
	public void starting(FrameworkMethod method) {
		fName= method.getName();
	}

	/**
	 * @return the name of the currently-running test method
	 */
	public String getMethodName() {
		return fName;
	}
}
