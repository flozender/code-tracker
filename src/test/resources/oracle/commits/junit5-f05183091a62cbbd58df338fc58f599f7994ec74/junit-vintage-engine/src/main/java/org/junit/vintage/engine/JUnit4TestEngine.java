/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.vintage.engine;

import static org.junit.platform.commons.meta.API.Usage.Experimental;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.vintage.engine.descriptor.JUnit4TestDescriptor.ENGINE_ID;

import java.util.logging.Logger;

import org.junit.platform.commons.meta.API;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.discovery.JUnit4DiscoveryRequestResolver;
import org.junit.vintage.engine.execution.RunnerExecutor;

/**
 * @since 4.12
 */
@API(Experimental)
public class JUnit4TestEngine implements TestEngine {

	private static final Logger LOG = Logger.getLogger(JUnit4TestEngine.class.getName());

	@Override
	public String getId() {
		return ENGINE_ID;
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "JUnit 4");
		new JUnit4DiscoveryRequestResolver(engineDescriptor, LOG).resolve(discoveryRequest);
		return engineDescriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();
		TestDescriptor engineTestDescriptor = request.getRootTestDescriptor();
		engineExecutionListener.executionStarted(engineTestDescriptor);
		RunnerExecutor runnerExecutor = new RunnerExecutor(engineExecutionListener, LOG);
		executeAllChildren(runnerExecutor, engineTestDescriptor);
		engineExecutionListener.executionFinished(engineTestDescriptor, successful());
	}

	private void executeAllChildren(RunnerExecutor runnerExecutor, TestDescriptor engineTestDescriptor) {
		// @formatter:off
		engineTestDescriptor.getChildren()
			.stream()
			.map(RunnerTestDescriptor.class::cast)
			.forEach(runnerExecutor::execute);
		// @formatter:on
	}
}
