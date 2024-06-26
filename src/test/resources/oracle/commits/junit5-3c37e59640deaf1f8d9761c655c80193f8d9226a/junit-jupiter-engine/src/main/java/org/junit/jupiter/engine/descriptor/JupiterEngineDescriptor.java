/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.jupiter.engine.extension.ExtensionRegistry.createRegistryWithDefaultExtensions;
import static org.junit.platform.commons.meta.API.Status.INTERNAL;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.meta.API;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

/**
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class JupiterEngineDescriptor extends EngineDescriptor implements Node<JupiterEngineExecutionContext> {

	public JupiterEngineDescriptor(UniqueId uniqueId) {
		super(uniqueId, "JUnit Jupiter");
	}

	@Override
	public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) throws Exception {
		ExtensionRegistry extensionRegistry = createRegistryWithDefaultExtensions(context.getConfigurationParameters());
		EngineExecutionListener executionListener = context.getExecutionListener();
		ExtensionContext extensionContext = new JupiterEngineExtensionContext(executionListener, this);

		// @formatter:off
		return context.extend()
				.withExtensionRegistry(extensionRegistry)
				.withExtensionContext(extensionContext)
				.build();
		// @formatter:on
	}

}
