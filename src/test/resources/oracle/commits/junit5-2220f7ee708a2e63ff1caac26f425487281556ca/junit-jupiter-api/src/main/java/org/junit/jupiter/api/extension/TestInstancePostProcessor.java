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

import org.junit.platform.commons.meta.API;

/**
 * {@code TestInstancePostProcessor} defines the API for {@link Extension
 * Extensions} that wish to <em>post-process</em> test instances.
 *
 * <p>Common use cases include injecting dependencies into the test
 * instance, invoking custom initialization methods on the test instance,
 * etc.
 *
 * <p>Implementations must provide a no-args constructor.
 *
 * @since 5.0
 * @see #postProcessTestInstance(Object, ExtensionContext)
 */
@API(status = STABLE, since = "5.0")
public interface TestInstancePostProcessor extends Extension {

	/**
	 * Callback for post-processing the supplied test instance.
	 *
	 * <p><strong>Note</strong>: the {@code ExtensionContext} supplied to a
	 * {@code TestInstancePostProcessor} will always return an empty
	 * {@link java.util.Optional} value from {@link ExtensionContext#getTestInstance()
	 * getTestInstance()}. A {@code TestInstancePostProcessor} should therefore
	 * only attempt to process the supplied {@code testInstance}.
	 *
	 * @param testInstance the instance to post-process; never {@code null}
	 * @param context the current extension context; never {@code null}
	 */
	void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception;

}
