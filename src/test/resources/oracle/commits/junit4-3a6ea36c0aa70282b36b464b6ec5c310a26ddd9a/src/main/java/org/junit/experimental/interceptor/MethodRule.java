package org.junit.experimental.interceptor;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public interface MethodRule {
	// TODO (Jul 1, 2009 1:43:11 PM): add documentation to
	// BlockJUnit4ClassRunner
	Statement apply(Statement base, FrameworkMethod method, Object target);
}