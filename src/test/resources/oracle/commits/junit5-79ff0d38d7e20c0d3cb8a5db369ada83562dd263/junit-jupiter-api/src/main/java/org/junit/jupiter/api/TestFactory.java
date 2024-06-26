/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.API.Status.EXPERIMENTAL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.API;
import org.junit.platform.commons.annotation.Testable;

/**
 * {@code @TestFactory} is used to signal that the annotated method is a
 * <em>test factory</em> method.
 *
 * <p>In contrast to {@link Test @Test} methods, a test factory is not itself
 * a test case but rather a factory for test cases.
 *
 * <p>{@code @TestFactory} methods must not be {@code private} or {@code static}
 * and must return a {@code Stream}, {@code Collection}, {@code Iterable}, or
 * {@code Iterator} of {@link DynamicNode} instances. Valid, instantiable
 * subclasses of {@code DynamicNode} are {@link DynamicContainer} and
 * {@link DynamicTest}. These {@code DynamicTest}s  will then be executed lazily,
 * enabling dynamic and even non-deterministic generation of test cases.
 *
 * <p>Any {@code Stream} returned by a {@code @TestFactory} will be properly
 * closed by calling {@code stream.close()}, making it safe to use a resource
 * such as {@code Files.lines()}.
 *
 * <p>{@code @TestFactory} methods may optionally declare parameters to be
 * resolved by {@link org.junit.jupiter.api.extension.ParameterResolver
 * ParameterResolvers}.
 *
 * @since 5.0
 * @see Test
 * @see DynamicNode
 * @see DynamicTest
 * @see DynamicContainer
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = EXPERIMENTAL, since = "5.0")
@Testable
public @interface TestFactory {
}
