/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.engine.discovery;

import static org.junit.gen5.commons.meta.API.Usage.Experimental;

import org.junit.gen5.commons.meta.API;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.UniqueId;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsNestedTestClass;

/**
 * @since 5.0
 */
@API(Experimental)
class NestedTestsResolver extends TestContainerResolver {

	private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();

	static final String SEGMENT_TYPE = "nested-class";

	@Override
	protected Class<? extends TestDescriptor> requiredParentType() {
		return ClassTestDescriptor.class;
	}

	@Override
	protected String getClassName(TestDescriptor parent, String segmentValue) {
		return ((ClassTestDescriptor) parent).getTestClass().getName() + "$" + segmentValue;
	}

	@Override
	protected String getSegmentType() {
		return SEGMENT_TYPE;
	}

	@Override
	protected String getSegmentValue(Class<?> testClass) {
		return testClass.getSimpleName();
	}

	@Override
	protected boolean isPotentialCandidate(Class<?> element) {
		return isNestedTestClass.test(element);
	}

	@Override
	protected TestDescriptor resolveClass(Class<?> testClass, UniqueId uniqueId) {
		return new NestedClassTestDescriptor(uniqueId, testClass);
	}

}
