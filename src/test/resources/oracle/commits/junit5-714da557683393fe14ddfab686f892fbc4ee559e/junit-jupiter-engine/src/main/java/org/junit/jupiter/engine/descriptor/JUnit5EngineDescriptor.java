/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.jupiter.engine.extension.ExtensionRegistry.createRegistryWithDefaultExtensions;
import static org.junit.platform.commons.meta.API.Usage.Internal;

import org.junit.jupiter.engine.execution.JUnit5EngineExecutionContext;
import org.junit.platform.commons.meta.API;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

/**
 * @since 5.0
 */
@API(Internal)
public class JUnit5EngineDescriptor extends EngineDescriptor implements Node<JUnit5EngineExecutionContext> {

	public JUnit5EngineDescriptor(UniqueId uniqueId) {
		super(uniqueId, "JUnit 5");
	}

	@Override
	public JUnit5EngineExecutionContext before(JUnit5EngineExecutionContext context) {
		return context.extend().withExtensionRegistry(createRegistryWithDefaultExtensions()).build();
	}

}
