/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.runner;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.commons.meta.API.Status.STABLE;
import static org.junit.platform.engine.discovery.ClassNameFilter.STANDARD_INCLUDE_PATTERN;
import static org.junit.platform.engine.discovery.ClassNameFilter.excludeClassNamePatterns;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.PackageNameFilter.excludePackageNames;
import static org.junit.platform.engine.discovery.PackageNameFilter.includePackageNames;
import static org.junit.platform.launcher.EngineFilter.excludeEngines;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.TagFilter.excludeTags;
import static org.junit.platform.launcher.TagFilter.includeTags;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.platform.commons.meta.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.ExcludeEngines;
import org.junit.platform.suite.api.ExcludePackages;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.IncludePackages;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.UseTechnicalNames;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

/**
 * JUnit 4 based {@link Runner} which runs tests on the JUnit Platform in a
 * JUnit 4 environment.
 *
 * <p>Annotating a class with {@code @RunWith(JUnitPlatform.class)} allows it
 * to be run with IDEs and build systems that support JUnit 4 but do not yet
 * support the JUnit Platform directly.
 *
 * <p>Consult the various annotations in the {@code org.junit.platform.suite.api}
 * package for configuration options.
 *
 * <p>If you do not use any configuration annotations from the
 * {@code org.junit.platform.suite.api} package, you can simply use this runner
 * on a test class whose programming model is supported on the JUnit Platform
 * &mdash; for example, a JUnit Jupiter test class. Note, however, that any test
 * class run with this runner must be {@code public} in order to be picked up by
 * IDEs and build tools.
 *
 * <p>When used on a class that serves as a test suite and the
 * {@link IncludeClassNamePatterns @IncludeClassNamePatterns} annotation is not
 * present, the default include pattern
 * {@value org.junit.platform.engine.discovery.ClassNameFilter#STANDARD_INCLUDE_PATTERN}
 * will be used in order to avoid loading classes unnecessarily (see {@link
 * org.junit.platform.engine.discovery.ClassNameFilter#STANDARD_INCLUDE_PATTERN
 * ClassNameFilter#STANDARD_INCLUDE_PATTERN}).
 *
 * @since 1.0
 * @see SelectPackages
 * @see SelectClasses
 * @see IncludeClassNamePatterns
 * @see ExcludeClassNamePatterns
 * @see IncludeTags
 * @see ExcludeTags
 * @see IncludeEngines
 * @see ExcludeEngines
 * @see UseTechnicalNames
 */
@API(status = STABLE)
public class JUnitPlatform extends Runner implements Filterable {

	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private final Class<?> testClass;
	private final Launcher launcher;

	private LauncherDiscoveryRequest discoveryRequest;
	private JUnitPlatformTestTree testTree;

	public JUnitPlatform(Class<?> testClass) throws InitializationError {
		this(testClass, LauncherFactory.create());
	}

	// For testing only
	JUnitPlatform(Class<?> testClass, Launcher launcher) throws InitializationError {
		this.launcher = launcher;
		this.testClass = testClass;
		this.discoveryRequest = createDiscoveryRequest();
		this.testTree = generateTestTree();
	}

	@Override
	public Description getDescription() {
		return this.testTree.getSuiteDescription();
	}

	@Override
	public void run(RunNotifier notifier) {
		JUnitPlatformRunnerListener listener = new JUnitPlatformRunnerListener(this.testTree, notifier);
		this.launcher.registerTestExecutionListeners(listener);
		this.launcher.execute(this.discoveryRequest);
	}

	private JUnitPlatformTestTree generateTestTree() {
		Preconditions.notNull(this.discoveryRequest, "DiscoveryRequest must not be null");
		TestPlan plan = this.launcher.discover(this.discoveryRequest);
		return new JUnitPlatformTestTree(plan, testClass);
	}

	private LauncherDiscoveryRequest createDiscoveryRequest() {
		List<DiscoverySelector> selectors = getSelectorsFromAnnotations();

		// Allows to simply add @RunWith(JUnitPlatform.class) to any test case
		boolean isSuite = !selectors.isEmpty();
		if (!isSuite) {
			selectors.add(selectClass(this.testClass));
		}

		LauncherDiscoveryRequestBuilder requestBuilder = request().selectors(selectors);
		addFiltersFromAnnotations(requestBuilder, isSuite);
		return requestBuilder.build();
	}

	private void addFiltersFromAnnotations(LauncherDiscoveryRequestBuilder requestBuilder, boolean isSuite) {
		addIncludeClassNamePatternFilter(requestBuilder, isSuite);
		addExcludeClassNamePatternFilter(requestBuilder);

		addIncludePackagesFilter(requestBuilder);
		addExcludePackagesFilter(requestBuilder);

		addIncludedTagsFilter(requestBuilder);
		addExcludedTagsFilter(requestBuilder);

		addIncludedEnginesFilter(requestBuilder);
		addExcludedEnginesFilter(requestBuilder);
	}

	private List<DiscoverySelector> getSelectorsFromAnnotations() {
		List<DiscoverySelector> selectors = new ArrayList<>();

		selectors.addAll(transform(getSelectedClasses(), DiscoverySelectors::selectClass));
		selectors.addAll(transform(getSelectedPackageNames(), DiscoverySelectors::selectPackage));

		return selectors;
	}

	private <T> List<DiscoverySelector> transform(T[] sourceElements, Function<T, DiscoverySelector> transformer) {
		return stream(sourceElements).map(transformer).collect(toList());
	}

	private void addIncludeClassNamePatternFilter(LauncherDiscoveryRequestBuilder requestBuilder, boolean isSuite) {
		String[] patterns = getIncludeClassNamePatterns(isSuite);
		if (patterns.length > 0) {
			requestBuilder.filters(includeClassNamePatterns(patterns));
		}
	}

	private void addExcludeClassNamePatternFilter(LauncherDiscoveryRequestBuilder requestBuilder) {
		String[] patterns = getExcludeClassNamePatterns();
		if (patterns.length > 0) {
			requestBuilder.filters(excludeClassNamePatterns(patterns));
		}
	}

	private void addIncludePackagesFilter(LauncherDiscoveryRequestBuilder requestBuilder) {
		String[] includedPackages = getIncludedPackages();
		if (includedPackages.length > 0) {
			requestBuilder.filters(includePackageNames(includedPackages));
		}
	}

	private void addExcludePackagesFilter(LauncherDiscoveryRequestBuilder requestBuilder) {
		String[] excludedPackages = getExcludedPackages();
		if (excludedPackages.length > 0) {
			requestBuilder.filters(excludePackageNames(excludedPackages));
		}
	}

	private void addIncludedTagsFilter(LauncherDiscoveryRequestBuilder requestBuilder) {
		String[] includedTags = getIncludedTags();
		if (includedTags.length > 0) {
			requestBuilder.filters(includeTags(includedTags));
		}
	}

	private void addExcludedTagsFilter(LauncherDiscoveryRequestBuilder requestBuilder) {
		String[] excludedTags = getExcludedTags();
		if (excludedTags.length > 0) {
			requestBuilder.filters(excludeTags(excludedTags));
		}
	}

	private void addIncludedEnginesFilter(LauncherDiscoveryRequestBuilder requestBuilder) {
		String[] engineIds = getIncludedEngineIds();
		if (engineIds.length > 0) {
			requestBuilder.filters(includeEngines(engineIds));
		}
	}

	private void addExcludedEnginesFilter(LauncherDiscoveryRequestBuilder requestBuilder) {
		String[] engineIds = getExcludedEngineIds();
		if (engineIds.length > 0) {
			requestBuilder.filters(excludeEngines(engineIds));
		}
	}

	private Class<?>[] getSelectedClasses() {
		return getValueFromAnnotation(SelectClasses.class, SelectClasses::value, EMPTY_CLASS_ARRAY);
	}

	private String[] getSelectedPackageNames() {
		return getValueFromAnnotation(SelectPackages.class, SelectPackages::value, EMPTY_STRING_ARRAY);
	}

	private String[] getIncludedPackages() {
		return getValueFromAnnotation(IncludePackages.class, IncludePackages::value, EMPTY_STRING_ARRAY);
	}

	private String[] getExcludedPackages() {
		return getValueFromAnnotation(ExcludePackages.class, ExcludePackages::value, EMPTY_STRING_ARRAY);
	}

	private String[] getIncludedTags() {
		return getValueFromAnnotation(IncludeTags.class, IncludeTags::value, EMPTY_STRING_ARRAY);
	}

	private String[] getExcludedTags() {
		return getValueFromAnnotation(ExcludeTags.class, ExcludeTags::value, EMPTY_STRING_ARRAY);
	}

	private String[] getIncludedEngineIds() {
		return getValueFromAnnotation(IncludeEngines.class, IncludeEngines::value, EMPTY_STRING_ARRAY);
	}

	private String[] getExcludedEngineIds() {
		return getValueFromAnnotation(ExcludeEngines.class, ExcludeEngines::value, EMPTY_STRING_ARRAY);
	}

	private String[] getIncludeClassNamePatterns(boolean isSuite) {
		String[] patterns = getValueFromAnnotation(IncludeClassNamePatterns.class, IncludeClassNamePatterns::value,
			new String[0]);
		if (patterns.length == 0 && isSuite) {
			return new String[] { STANDARD_INCLUDE_PATTERN };
		}
		Preconditions.containsNoNullElements(patterns, "IncludeClassNamePatterns must not contain null elements");
		trim(patterns);
		return patterns;
	}

	private String[] getExcludeClassNamePatterns() {
		String[] patterns = getValueFromAnnotation(ExcludeClassNamePatterns.class, ExcludeClassNamePatterns::value,
			new String[0]);

		Preconditions.containsNoNullElements(patterns, "ExcludeClassNamePatterns must not contain null elements");
		trim(patterns);
		return patterns;
	}

	private void trim(String[] patterns) {
		for (int i = 0; i < patterns.length; i++) {
			patterns[i] = patterns[i].trim();
		}
	}

	private <A extends Annotation, V> V getValueFromAnnotation(Class<A> annotationClass, Function<A, V> extractor,
			V defaultValue) {
		A annotation = this.testClass.getAnnotation(annotationClass);
		return (annotation != null ? extractor.apply(annotation) : defaultValue);
	}

	@Override
	public void filter(Filter filter) throws NoTestsRemainException {
		Set<TestIdentifier> filteredIdentifiers = testTree.getFilteredLeaves(filter);
		if (filteredIdentifiers.isEmpty()) {
			throw new NoTestsRemainException();
		}
		this.discoveryRequest = createDiscoveryRequestForUniqueIds(filteredIdentifiers);
		this.testTree = generateTestTree();
	}

	private LauncherDiscoveryRequest createDiscoveryRequestForUniqueIds(Set<TestIdentifier> testIdentifiers) {
		// @formatter:off
		List<DiscoverySelector> selectors = testIdentifiers.stream()
				.map(TestIdentifier::getUniqueId)
				.map(DiscoverySelectors::selectUniqueId)
				.collect(toList());
		// @formatter:on
		return request().selectors(selectors).build();
	}

}
