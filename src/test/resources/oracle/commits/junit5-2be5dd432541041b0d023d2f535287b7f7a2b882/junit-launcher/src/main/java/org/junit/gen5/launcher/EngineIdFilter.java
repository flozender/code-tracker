/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.launcher;

import static org.junit.gen5.engine.FilterResult.includedIf;

import org.junit.gen5.engine.Filter;
import org.junit.gen5.engine.FilterResult;
import org.junit.gen5.engine.TestEngine;

/**
 * A special filter that is applied before a {@link TestEngine} is executed.
 * It allows to include the given engine id within the test discovery and
 * execution.
 *
 * @since 5.0
 * @see TestDiscoveryRequest
 */
public class EngineIdFilter implements Filter<String> {

	public static EngineIdFilter byEngineId(String engineId) {
		return new EngineIdFilter(engineId);
	}

	private final String engineId;

	private EngineIdFilter(String engineId) {
		this.engineId = engineId;
	}

	@Override
	public FilterResult filter(String engineId) {
		return includedIf(this.engineId.equals(engineId), //
			() -> "Engine ID matches", //
			() -> "Engine ID does not match");
	}
}
