/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.discovery;

import static org.junit.gen5.commons.meta.API.Usage.Experimental;
import static org.junit.gen5.commons.util.ReflectionUtils.findAllClassesInClasspathRoot;
import static org.junit.gen5.commons.util.ReflectionUtils.findAllClassesInPackage;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.gen5.commons.meta.API;
import org.junit.gen5.engine.EngineDiscoveryRequest;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.engine.discovery.ClassSelector;
import org.junit.gen5.engine.discovery.ClasspathSelector;
import org.junit.gen5.engine.discovery.MethodSelector;
import org.junit.gen5.engine.discovery.PackageSelector;
import org.junit.gen5.engine.discovery.UniqueIdSelector;

/**
 * @since 5.0
 */
@API(Experimental)
public class DiscoverySelectorResolver {

	public void resolveSelectors(EngineDiscoveryRequest request, TestDescriptor engineDescriptor) {
		JavaElementsResolver javaElementsResolver = createJavaElementsResolver(engineDescriptor);

		request.getSelectorsByType(ClasspathSelector.class).forEach(selector -> {
			File rootDirectory = selector.getClasspathRoot();
			findAllClassesInClasspathRoot(rootDirectory, new IsScannableTestClass()).forEach(
				javaElementsResolver::resolveClass);
		});
		request.getSelectorsByType(PackageSelector.class).forEach(selector -> {
			String packageName = selector.getPackageName();
			findAllClassesInPackage(packageName, new IsScannableTestClass()).forEach(
				javaElementsResolver::resolveClass);
		});
		request.getSelectorsByType(ClassSelector.class).forEach(selector -> {
			javaElementsResolver.resolveClass(selector.getTestClass());
		});
		request.getSelectorsByType(MethodSelector.class).forEach(selector -> {
			javaElementsResolver.resolveMethod(selector.getTestClass(), selector.getTestMethod());
		});
		request.getSelectorsByType(UniqueIdSelector.class).forEach(selector -> {
			javaElementsResolver.resolveUniqueId(UniqueId.parse(selector.getUniqueId()));
		});
		pruneTree(engineDescriptor);
	}

	private JavaElementsResolver createJavaElementsResolver(TestDescriptor engineDescriptor) {
		Set<ElementResolver> resolvers = new HashSet<>();
		resolvers.add(new TestContainerResolver());
		resolvers.add(new NestedTestsResolver());
		resolvers.add(new TestMethodResolver());
		resolvers.add(new TestFactoryMethodResolver());
		return new JavaElementsResolver(engineDescriptor, resolvers);
	}

	private void pruneTree(TestDescriptor root) {
		TestDescriptor.Visitor removeChildrenWithoutTests = (descriptor) -> {
			if (!descriptor.isRoot() && !descriptor.hasTests())
				descriptor.removeFromHierarchy();
		};
		root.accept(removeChildrenWithoutTests);
	}

}
