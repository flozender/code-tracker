/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static org.junit.platform.commons.meta.API.Status.INTERNAL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.meta.API;

/**
 * Functional interface for registering an {@link AfterEach @AfterEach} method
 * as a pseudo-extension.
 *
 * @since 5.0
 */
@FunctionalInterface
@API(status = INTERNAL, since = "5.0")
public interface AfterEachMethodAdapter extends Extension {

	void invokeAfterEachMethod(ExtensionContext context, ExtensionRegistry registry) throws Throwable;

}
