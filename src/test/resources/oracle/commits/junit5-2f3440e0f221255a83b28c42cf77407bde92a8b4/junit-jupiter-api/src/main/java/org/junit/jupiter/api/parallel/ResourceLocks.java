/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.parallel;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @ResourceLocks} is a container for one or more
 * {@link ResourceLock @ResourceLock} declarations.
 *
 * <p>Note, however, that use of the {@code @ResourceLocks} container is
 * completely optional since {@code @ResourceLock} is a
 * {@linkplain java.lang.annotation.Repeatable repeatable} annotation.
 *
 * @see ResourceLock
 * @since 5.3
 */
@API(status = EXPERIMENTAL, since = "5.3")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ResourceLocks {

	/**
	 * An array of one or more {@linkplain ResourceLock used resources}.
	 */
	ResourceLock[] value() default {};

}
