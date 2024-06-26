/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.suite.api;

import static org.junit.platform.commons.meta.API.Status.MAINTAINED;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.platform.commons.meta.API;

/**
 * {@code @IncludeClassNamePatterns} specifies regular expressions that are used
 * to match against fully qualified class names when running a test suite on the
 * JUnit Platform.
 *
 * <p>The patterns are combined using OR semantics: if the fully qualified name
 * of a class matches against at least one of the patterns, the class will be
 * included in the test plan.
 *
 * <h4>JUnit 4 Suite Support</h4>
 * <p>Test suites can be run on the JUnit Platform in a JUnit 4 environment via
 * {@code @RunWith(JUnitPlatform.class)}.
 *
 * @since 1.0
 * @see org.junit.platform.engine.discovery.ClassNameFilter#STANDARD_INCLUDE_PATTERN
 * @see org.junit.platform.engine.discovery.ClassNameFilter#includeClassNamePatterns
 * @see org.junit.platform.runner.JUnitPlatform
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = MAINTAINED)
public @interface IncludeClassNamePatterns {

	/**
	 * Regular expressions used to match against fully qualified class names.
	 *
	 * <p>Defaults to {@code "^.*Tests?$"} which matches against class names
	 * ending in {@code Test} or {@code Tests} (in any package).
	 */
	String[] value() default "^.*Tests?$";

}
