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

import static org.junit.platform.commons.meta.API.Status.INTERNAL;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.meta.API;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

/**
 * {@link TestDescriptor} for a {@link org.junit.jupiter.api.TestTemplate @TestTemplate}
 * invocation.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class TestTemplateInvocationTestDescriptor extends TestMethodTestDescriptor {

	public static final String SEGMENT_TYPE = "test-template-invocation";

	private TestTemplateInvocationContext invocationContext;

	TestTemplateInvocationTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method templateMethod,
			TestTemplateInvocationContext invocationContext, int index) {
		super(uniqueId, invocationContext.getDisplayName(index), testClass, templateMethod);
		this.invocationContext = invocationContext;
	}

	@Override
	protected ExtensionRegistry populateNewExtensionRegistry(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = super.populateNewExtensionRegistry(context);
		invocationContext.getAdditionalExtensions().forEach(
			extension -> registry.registerExtension(extension, invocationContext));
		return registry;
	}

	@Override
	public void after(JupiterEngineExecutionContext context) {
		// forget invocationContext so it can be garbage collected
		invocationContext = null;
	}
}
