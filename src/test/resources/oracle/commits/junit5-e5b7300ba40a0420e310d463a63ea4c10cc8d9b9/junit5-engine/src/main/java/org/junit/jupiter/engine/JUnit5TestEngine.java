/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.engine;

import static org.junit.platform.commons.meta.API.Usage.Experimental;

import org.junit.jupiter.engine.descriptor.JUnit5EngineDescriptor;
import org.junit.jupiter.engine.discovery.DiscoverySelectorResolver;
import org.junit.jupiter.engine.execution.JUnit5EngineExecutionContext;
import org.junit.platform.commons.meta.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

/**
 * @since 5.0
 */
@API(Experimental)
public class JUnit5TestEngine extends HierarchicalTestEngine<JUnit5EngineExecutionContext> {

	public static final String ENGINE_ID = "junit5";

	@Override
	public String getId() {
		return ENGINE_ID;
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		Preconditions.notNull(discoveryRequest, "discovery request must not be null");
		JUnit5EngineDescriptor engineDescriptor = new JUnit5EngineDescriptor(uniqueId);
		resolveDiscoveryRequest(discoveryRequest, engineDescriptor);
		return engineDescriptor;
	}

	private void resolveDiscoveryRequest(EngineDiscoveryRequest discoveryRequest,
			JUnit5EngineDescriptor engineDescriptor) {
		DiscoverySelectorResolver resolver = new DiscoverySelectorResolver();
		resolver.resolveSelectors(discoveryRequest, engineDescriptor);
		applyDiscoveryFilters(discoveryRequest, engineDescriptor);
	}

	private void applyDiscoveryFilters(EngineDiscoveryRequest discoveryRequest,
			JUnit5EngineDescriptor engineDescriptor) {
		new DiscoveryFilterApplier().applyAllFilters(discoveryRequest, engineDescriptor);
	}

	@Override
	protected JUnit5EngineExecutionContext createExecutionContext(ExecutionRequest request) {
		return new JUnit5EngineExecutionContext(request.getEngineExecutionListener(),
			request.getConfigurationParameters());
	}

}
