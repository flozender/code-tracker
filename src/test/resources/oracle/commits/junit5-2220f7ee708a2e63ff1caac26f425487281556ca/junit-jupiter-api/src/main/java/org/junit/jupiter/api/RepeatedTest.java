/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.api;

import static org.junit.platform.commons.meta.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.platform.commons.meta.API;

/**
 * {@code @RepeatedTest} is used to signal that the annotated method is a
 * <em>test template</em> method that should be repeated a {@linkplain #value
 * specified number of times} with a configurable {@linkplain #name display
 * name}.
 *
 * <p>Each invocation of the repeated test behaves like the execution of a
 * regular {@link Test @Test} method with full support for the same lifecycle
 * callbacks and extensions. In addition, the current repetition and total
 * number of repetitions can be accessed by having the {@link RepetitionInfo}
 * injected.
 *
 * <p>{@code @RepeatedTest} methods must not be {@code private} or {@code static}
 * and must return {@code void}.
 *
 * <p>{@code @RepeatedTest} methods may optionally declare parameters to be
 * resolved by {@link org.junit.jupiter.api.extension.ParameterResolver
 * ParameterResolvers}.
 *
 * <p>{@code @RepeatedTest} may also be used as a meta-annotation in order to
 * create a custom <em>composed annotation</em> that inherits the semantics
 * of {@code @RepeatedTest}.
 *
 * @since 5.0
 * @see DisplayName
 * @see RepetitionInfo
 * @see TestTemplate
 * @see TestInfo
 * @see Test
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.0")
@TestTemplate
public @interface RepeatedTest {

	/**
	 * Placeholder for the {@linkplain TestInfo#getDisplayName display name} of
	 * a {@code @RepeatedTest} method: <code>{displayName}</code>
	 */
	String DISPLAY_NAME_PLACEHOLDER = "{displayName}";

	/**
	 * Placeholder for the current repetition count of a {@code @RepeatedTest}
	 * method: <code>{currentRepetition}</code>
	 */
	String CURRENT_REPETITION_PLACEHOLDER = "{currentRepetition}";

	/**
	 * Placeholder for the total number of repetitions of a {@code @RepeatedTest}
	 * method: <code>{totalRepetitions}</code>
	 */
	String TOTAL_REPETITIONS_PLACEHOLDER = "{totalRepetitions}";

	/**
	 * <em>Short</em> display name pattern for a repeated test: {@value #SHORT_DISPLAY_NAME}
	 *
	 * @see #CURRENT_REPETITION_PLACEHOLDER
	 * @see #TOTAL_REPETITIONS_PLACEHOLDER
	 * @see #LONG_DISPLAY_NAME
	 */
	String SHORT_DISPLAY_NAME = "repetition " + CURRENT_REPETITION_PLACEHOLDER + " of " + TOTAL_REPETITIONS_PLACEHOLDER;

	/**
	 * <em>Long</em> display name pattern for a repeated test: {@value #LONG_DISPLAY_NAME}
	 *
	 * @see #DISPLAY_NAME_PLACEHOLDER
	 * @see #SHORT_DISPLAY_NAME
	 */
	String LONG_DISPLAY_NAME = DISPLAY_NAME_PLACEHOLDER + " :: " + SHORT_DISPLAY_NAME;

	/**
	 * The number of repetitions.
	 *
	 * @return the number of repetitions; must be greater than zero
	 */
	int value();

	/**
	 * The display name for each repetition of the repeated test.
	 *
	 * <h4>Supported placeholders</h4>
	 * <ul>
	 * <li>{@value #DISPLAY_NAME_PLACEHOLDER}</li>
	 * <li>{@value #CURRENT_REPETITION_PLACEHOLDER}</li>
	 * <li>{@value #TOTAL_REPETITIONS_PLACEHOLDER}</li>
	 * </ul>
	 *
	 * <p>Defaults to {@link #SHORT_DISPLAY_NAME}, resulting in
	 * names such as {@code "repetition 1 of 2"}, {@code "repetition 2 of 2"},
	 * etc.
	 *
	 * <p>Can be set to <code>{@link #LONG_DISPLAY_NAME}</code>, resulting in
	 * names such as {@code "myRepeatedTest() :: repetition 1 of 2"},
	 * {@code "myRepeatedTest() :: repetition 2 of 2"}, etc.
	 *
	 * <p>Alternatively, you can provide a custom display name, optionally
	 * using the aforementioned placeholders.
	 *
	 * @return a custom display name; never blank or consisting solely of
	 * whitespace
	 * @see #SHORT_DISPLAY_NAME
	 * @see #LONG_DISPLAY_NAME
	 * @see #DISPLAY_NAME_PLACEHOLDER
	 * @see #CURRENT_REPETITION_PLACEHOLDER
	 * @see #TOTAL_REPETITIONS_PLACEHOLDER
	 * @see TestInfo#getDisplayName()
	 */
	String name() default SHORT_DISPLAY_NAME;

}
