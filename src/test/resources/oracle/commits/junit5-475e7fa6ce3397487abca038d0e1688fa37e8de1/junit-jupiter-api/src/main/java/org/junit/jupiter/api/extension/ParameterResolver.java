/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.api.extension;

import static org.junit.platform.commons.meta.API.Status.STABLE;

import java.lang.reflect.Parameter;

import org.junit.platform.commons.meta.API;

/**
 * {@code ParameterResolver} defines the API for {@link Extension Extensions}
 * that wish to dynamically resolve arguments for {@linkplain Parameter parameters}
 * at runtime.
 *
 * <p>If a constructor for a test class or a
 * {@link org.junit.jupiter.api.Test @Test},
 * {@link org.junit.jupiter.api.BeforeEach @BeforeEach},
 * {@link org.junit.jupiter.api.AfterEach @AfterEach},
 * {@link org.junit.jupiter.api.BeforeAll @BeforeAll}, or
 * {@link org.junit.jupiter.api.AfterAll @AfterAll} method declares a parameter,
 * an argument for the parameter must be resolved at runtime by a
 * {@code ParameterResolver}.
 *
 * <p>Implementations must provide a no-args constructor.
 *
 * @since 5.0
 * @see #supportsParameter(ParameterContext, ExtensionContext)
 * @see #resolveParameter(ParameterContext, ExtensionContext)
 * @see ParameterContext
 */
@API(status = STABLE)
public interface ParameterResolver extends Extension {

	/**
	 * Determine if this resolver supports resolution of an argument for the
	 * {@link Parameter} in the supplied {@link ParameterContext} for the supplied
	 * {@link ExtensionContext}.
	 *
	 * <p>The {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor}
	 * in which the parameter is declared can be retrieved via
	 * {@link ParameterContext#getDeclaringExecutable()}.
	 *
	 * @param parameterContext the context for the parameter for which an argument should
	 * be resolved; never {@code null}
	 * @param extensionContext the extension context for the {@code Executable}
	 * about to be invoked; never {@code null}
	 * @return {@code true} if this resolver can resolve an argument for the parameter
	 * @see #resolveParameter
	 * @see ParameterContext
	 */
	boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException;

	/**
	 * Resolve an argument for the {@link Parameter} in the supplied {@link ParameterContext}
	 * for the supplied {@link ExtensionContext}.
	 *
	 * <p>This method is only called by the framework if {@link #supportsParameter}
	 * previously returned {@code true} for the same {@link ParameterContext}
	 * and {@link ExtensionContext}.
	 *
	 * <p>The {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor}
	 * in which the parameter is declared can be retrieved via
	 * {@link ParameterContext#getDeclaringExecutable()}.
	 *
	 * @param parameterContext the context for the parameter for which an argument should
	 * be resolved; never {@code null}
	 * @param extensionContext the extension context for the {@code Executable}
	 * about to be invoked; never {@code null}
	 * @return the resolved argument for the parameter; may only be {@code null} if the
	 * parameter type is not a primitive
	 * @see #supportsParameter
	 * @see ParameterContext
	 */
	Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException;

}
