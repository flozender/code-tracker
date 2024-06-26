/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;

/**
 * {@link TestDescriptor} for tests based on nested (but not static) Java classes.
 *
 * <h3>Default Display Names</h3>
 *
 * <p>The default display name for a non-static nested test class is the simple
 * name of the class.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class NestedClassTestDescriptor extends ClassTestDescriptor {

	/**
	 * Set of local class-level tags; does not contain tags from parent.
	 */
	private final Set<TestTag> tags;

	public NestedClassTestDescriptor(UniqueId uniqueId, Class<?> testClass) {
		super(uniqueId, Class::getSimpleName, testClass);

		this.tags = getTags(testClass);
	}

	// --- TestDescriptor ------------------------------------------------------

	@Override
	public final Set<TestTag> getTags() {
		// return modifiable copy
		Set<TestTag> allTags = new LinkedHashSet<TestTag>(this.tags);
		getParent().ifPresent(parentDescriptor -> allTags.addAll(parentDescriptor.getTags()));
		return allTags;
	}

	// --- Node ----------------------------------------------------------------

	@Override
	protected Object instantiateTestClass(JupiterEngineExecutionContext parentExecutionContext,
			ExtensionRegistry registry, ExtensionContext extensionContext) {

		// Extensions registered for nested classes and below are not to be used for instantiating outer classes
		Optional<ExtensionRegistry> childExtensionRegistryForOuterInstance = Optional.empty();
		Object outerInstance = parentExecutionContext.getTestInstanceProvider().getTestInstance(
			childExtensionRegistryForOuterInstance);
		ExtensionRegistry parentRegistry = parentExecutionContext.getExtensionRegistry();
		return instantiateTestClass(Optional.of(outerInstance), registry, parentRegistry, extensionContext);
	}

}
