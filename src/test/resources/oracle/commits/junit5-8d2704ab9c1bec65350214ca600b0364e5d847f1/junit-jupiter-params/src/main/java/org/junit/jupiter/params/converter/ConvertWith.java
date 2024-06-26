/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.params.converter;

import static org.junit.platform.commons.meta.API.Status.EXPERIMENTAL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.platform.commons.meta.API;

/**
 * {@code @ConvertWith} is an annotation that allows one to specify an explicit
 * {@link ArgumentConverter}.

 * <p>This annotation may be applied to method parameters of
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest} methods
 * which need to have their {@code Arguments} converted before consuming them.
 *
 * @since 5.0
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see org.junit.jupiter.params.converter.ArgumentConverter
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = EXPERIMENTAL)
public @interface ConvertWith {

	/**
	 * The type of {@link ArgumentConverter} to use.
	 */
	Class<? extends ArgumentConverter> value();

}
