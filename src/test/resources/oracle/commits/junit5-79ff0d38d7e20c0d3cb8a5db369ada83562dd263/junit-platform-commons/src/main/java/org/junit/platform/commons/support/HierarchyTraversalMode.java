/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support;

import static org.apiguardian.API.Status.MAINTAINED;

import org.apiguardian.API;

/**
 * Modes in which a hierarchy can be traversed &mdash; for example, when
 * searching for methods or fields within a class hierarchy.
 *
 * @since 1.0
 * @see #TOP_DOWN
 * @see #BOTTOM_UP
 */
@API(status = MAINTAINED, since = "5.0")
public enum HierarchyTraversalMode {

	/**
	 * Traverse the hierarchy using top-down semantics.
	 */
	TOP_DOWN,

	/**
	 * Traverse the hierarchy using bottom-up semantics.
	 */
	BOTTOM_UP;

}
