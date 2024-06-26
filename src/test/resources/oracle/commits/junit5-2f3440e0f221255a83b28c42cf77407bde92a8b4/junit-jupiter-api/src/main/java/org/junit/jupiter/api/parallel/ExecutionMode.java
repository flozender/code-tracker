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

import org.apiguardian.api.API;

/**
 * Supported execution modes for parallel execution.
 *
 * @since 5.3
 */
@API(status = EXPERIMENTAL, since = "5.3")
public enum ExecutionMode {

	/**
	 * Force execution in same thread as the parent node.
	 */
	SAME_THREAD,

	/**
	 * Allow concurrent execution with any other node.
	 */
	CONCURRENT

}
