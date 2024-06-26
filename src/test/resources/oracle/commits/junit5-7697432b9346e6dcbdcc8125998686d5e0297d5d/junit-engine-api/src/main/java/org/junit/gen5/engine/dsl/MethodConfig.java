/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.dsl;

import java.lang.reflect.Method;

import org.junit.gen5.commons.util.ReflectionUtils;

public class MethodConfig {
	private final String testClassName;
	private final String testMethodName;

	public MethodConfig(String testClassName, String testMethodName) {
		this.testClassName = testClassName;
		this.testMethodName = testMethodName;
	}

	public Class<?> getTestClass() {
		return ReflectionUtils.loadClass(testClassName).get();
	}

	public Method getTestMethod() {
		return ReflectionUtils.findMethod(getTestClass(), testMethodName).get();
	}
}
