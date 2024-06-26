/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.junit.platform.commons.meta.API.Status.STABLE;

import org.junit.platform.commons.meta.API;

/**
 * Marker interface for all extensions.
 *
 * <p>An {@code Extension} can be registered declaratively via
 * {@link ExtendWith @ExtendWith}.
 *
 * @since 5.0
 */
@API(status = STABLE, since = "5.0")
public interface Extension {
}
