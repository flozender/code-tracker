/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.api;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

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
 * @see SuiteDisplayName
 * @see UseTechnicalNames
 * @see SelectPackages
 * @see SelectClasses
 * @see ExcludeClassNamePatterns
 * @see IncludePackages
 * @see ExcludePackages
 * @see IncludeTags
 * @see ExcludeTags
 * @see IncludeEngines
 * @see ExcludeEngines
 * @see org.junit.platform.engine.discovery.ClassNameFilter#STANDARD_INCLUDE_PATTERN
 * @see org.junit.platform.engine.discovery.ClassNameFilter#includeClassNamePatterns
 * @see org.junit.platform.runner.JUnitPlatform
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = MAINTAINED, since = "1.0")
public @interface IncludeClassNamePatterns {

	/**
	 * Regular expressions used to match against fully qualified class names.
	 *
	 * <p>The default pattern matches against classes whose names either begin
	 * with {@code Test} or end with {@code Test} or {@code Tests} (in any package).
	 */
	String[] value() default "^(Test.*|.+[.$]Test.*|.*Tests?)$";

}
