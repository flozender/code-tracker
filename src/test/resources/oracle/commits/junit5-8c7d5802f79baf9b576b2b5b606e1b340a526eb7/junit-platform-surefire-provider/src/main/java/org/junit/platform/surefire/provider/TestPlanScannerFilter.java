/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.surefire.provider;

import static org.junit.platform.engine.discovery.ClassSelector.selectClass;
import static org.junit.platform.launcher.core.TestDiscoveryRequestBuilder.request;

import org.apache.maven.surefire.util.ScannerFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 5.0
 */
final class TestPlanScannerFilter implements ScannerFilter {

	private final Launcher launcher;

	public TestPlanScannerFilter(Launcher launcher) {
		this.launcher = launcher;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean accept(Class testClass) {
		TestDiscoveryRequest discoveryRequest = request().selectors(selectClass(testClass)).build();
		TestPlan testPlan = launcher.discover(discoveryRequest);
		return testPlan.countTestIdentifiers(TestIdentifier::isTest) > 0;
	}

}
