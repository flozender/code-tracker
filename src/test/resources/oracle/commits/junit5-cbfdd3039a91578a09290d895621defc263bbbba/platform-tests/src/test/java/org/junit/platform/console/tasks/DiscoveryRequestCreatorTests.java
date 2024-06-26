/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.console.tasks;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.console.options.CommandLineOptions;
import org.junit.platform.engine.discovery.ClassFilter;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.JavaClassSelector;
import org.junit.platform.engine.discovery.JavaMethodSelector;
import org.junit.platform.engine.discovery.JavaPackageSelector;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;

/**
 * @since 1.0
 */
public class DiscoveryRequestCreatorTests {

	private final CommandLineOptions options = new CommandLineOptions();

	@Test
	public void convertsClassArgument() {
		Class<?> testClass = getClass();
		options.setArguments(singletonList(testClass.getName()));

		LauncherDiscoveryRequest request = convert();

		List<JavaClassSelector> classSelectors = request.getSelectorsByType(JavaClassSelector.class);
		assertThat(classSelectors).hasSize(1);
		assertEquals(testClass, getOnlyElement(classSelectors).getJavaClass());
	}

	@Test
	public void convertsMethodArgument() throws Exception {
		Class<?> testClass = getClass();
		Method testMethod = testClass.getDeclaredMethod("convertsMethodArgument");
		options.setArguments(singletonList(testClass.getName() + "#" + testMethod.getName()));

		LauncherDiscoveryRequest request = convert();

		List<JavaMethodSelector> methodSelectors = request.getSelectorsByType(JavaMethodSelector.class);
		assertThat(methodSelectors).hasSize(1);
		assertEquals(testClass, getOnlyElement(methodSelectors).getJavaClass());
		assertEquals(testMethod, getOnlyElement(methodSelectors).getJavaMethod());
	}

	@Test
	public void convertsPackageArgument() {
		String packageName = getClass().getPackage().getName();
		options.setArguments(singletonList(packageName));

		LauncherDiscoveryRequest request = convert();

		List<JavaPackageSelector> packageSelectors = request.getSelectorsByType(JavaPackageSelector.class);
		assertThat(packageSelectors).extracting(JavaPackageSelector::getPackageName).containsExactly(packageName);
	}

	@Test
	public void convertsAllOptionWithoutExplicitRootDirectories() {
		options.setScanClasspath(true);

		LauncherDiscoveryRequest request = convert();

		List<ClasspathRootSelector> classpathRootSelectors = request.getSelectorsByType(ClasspathRootSelector.class);
		// @formatter:off
		assertThat(classpathRootSelectors).extracting(ClasspathRootSelector::getClasspathRoot)
			.hasAtLeastOneElementOfType(URI.class)
			.doesNotContainNull();
		// @formatter:on
	}

	@Test
	public void convertsAllOptionWithExplicitRootDirectories() {
		options.setScanClasspath(true);
		options.setArguments(asList(".", ".."));

		LauncherDiscoveryRequest request = convert();

		List<ClasspathRootSelector> classpathRootSelectors = request.getSelectorsByType(ClasspathRootSelector.class);
		// @formatter:off
		assertThat(classpathRootSelectors).extracting(ClasspathRootSelector::getClasspathRoot)
			.containsExactly(new File(".").toURI(), new File("..").toURI());
		// @formatter:on
	}

	@Test
	public void convertsAllOptionWithAdditionalClasspathEntries() {
		options.setScanClasspath(true);
		options.setAdditionalClasspathEntries(asList(new File("."), new File("..")));

		LauncherDiscoveryRequest request = convert();

		List<ClasspathRootSelector> classpathRootSelectors = request.getSelectorsByType(ClasspathRootSelector.class);
		// @formatter:off
		assertThat(classpathRootSelectors).extracting(ClasspathRootSelector::getClasspathRoot)
			.contains(new File(".").toURI(), new File("..").toURI());
		// @formatter:on
	}

	@Test
	public void convertsIncludeClassNamePatternOption() {
		options.setScanClasspath(true);
		options.setIncludeClassNamePattern(".*Test");

		LauncherDiscoveryRequest request = convert();

		List<ClassFilter> filter = request.getDiscoveryFiltersByType(ClassFilter.class);
		assertThat(filter).hasSize(1);
		assertThat(filter.get(0).toString()).contains(".*Test");
	}

	@Test
	public void convertsTagOptions() {
		options.setScanClasspath(true);
		options.setIncludedTags(asList("fast", "medium", "slow"));
		options.setExcludedTags(asList("slow"));

		LauncherDiscoveryRequest request = convert();
		List<PostDiscoveryFilter> postDiscoveryFilters = request.getPostDiscoveryFilters();

		assertThat(postDiscoveryFilters).hasSize(2);
		assertThat(postDiscoveryFilters.get(0).toString()).contains("TagFilter");
		assertThat(postDiscoveryFilters.get(1).toString()).contains("TagFilter");
	}

	@Test
	public void convertsEngineOptions() {
		options.setScanClasspath(true);
		options.setIncludedEngines(asList("engine1", "engine2", "engine3"));
		options.setExcludedEngines(asList("engine2"));

		LauncherDiscoveryRequest request = convert();
		List<EngineFilter> engineFilters = request.getEngineFilters();

		assertThat(engineFilters).hasSize(2);
		assertThat(engineFilters.get(0).toString()).contains("includes", "[engine1, engine2, engine3]");
		assertThat(engineFilters.get(1).toString()).contains("excludes", "[engine2]");
	}

	private LauncherDiscoveryRequest convert() {
		DiscoveryRequestCreator creator = new DiscoveryRequestCreator();
		return creator.toDiscoveryRequest(options);
	}

}
