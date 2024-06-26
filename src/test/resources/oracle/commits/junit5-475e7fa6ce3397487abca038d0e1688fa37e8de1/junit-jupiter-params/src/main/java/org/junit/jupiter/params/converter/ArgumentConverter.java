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

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.meta.API;

/**
 * {@code ArgumentConverter} is an abstraction that allows an input object to
 * be converted to an instance of a different class.
 *
 * <p>Such an {@code ArgumentConverter} is applied to the method parameter
 * of a {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest}
 * method with the help of a
 * {@link org.junit.jupiter.params.converter.ConvertWith @ConvertWith} annotation.
 *
 * <p>See {@link SimpleArgumentConverter} in case your implementation only needs
 * to know about the target type instead of the complete
 * {@link ParameterContext}.
 *
 * <p>Implementations must provide a no-args constructor.
 *
 * @since 5.0
 * @see SimpleArgumentConverter
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see org.junit.jupiter.params.converter.ConvertWith
 */
@API(status = EXPERIMENTAL)
public interface ArgumentConverter {

	/**
	 * Convert the supplied {@code source} object according to the supplied
	 * {@code context}.
	 *
	 * @param source the source object to convert; may be {@code null}
	 * @param context the parameter context where the converted object will be
	 * used; never {@code null}
	 * @return the converted object; may be {@code null} but only if the target
	 * type is a reference type
	 * @throws ArgumentConversionException if an error occurs during the
	 * conversion
	 */
	Object convert(Object source, ParameterContext context) throws ArgumentConversionException;

}
