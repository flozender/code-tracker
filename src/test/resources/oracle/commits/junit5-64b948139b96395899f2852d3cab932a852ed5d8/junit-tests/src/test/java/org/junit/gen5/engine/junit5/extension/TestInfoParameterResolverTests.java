/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.gen5.api.Assertions.assertEquals;
import static org.junit.gen5.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.gen5.api.AfterAll;
import org.junit.gen5.api.AfterEach;
import org.junit.gen5.api.BeforeAll;
import org.junit.gen5.api.BeforeEach;
import org.junit.gen5.api.DisplayName;
import org.junit.gen5.api.Tag;
import org.junit.gen5.api.Test;
import org.junit.gen5.api.TestInfo;

/**
 * Integration tests for {@link TestInfoParameterResolver}.
 *
 * @since 5.0
 */
@Tag("class-tag")
class TestInfoParameterResolverTests {

	private static List<String> allDisplayNames = Arrays.asList("defaultDisplayName(TestInfo)", "myName",
		"getTags(TestInfo)");

	@Test
	void defaultDisplayName(TestInfo testInfo) {
		assertEquals("defaultDisplayName(TestInfo)", testInfo.getDisplayName());
	}

	@Test
	@DisplayName("myName")
	void providedDisplayName(TestInfo testInfo) {
		assertEquals("myName", testInfo.getDisplayName());
	}

	@Test
	@Tag("method-tag")
	void getTags(TestInfo testInfo) {
		assertEquals(2, testInfo.getTags().size());
		assertTrue(testInfo.getTags().contains("method-tag"));
		assertTrue(testInfo.getTags().contains("class-tag"));
	}

	@BeforeEach
	@AfterEach
	void beforeAndAfter(TestInfo testInfo) {
		assertThat(allDisplayNames).contains(testInfo.getDisplayName());
	}

	@BeforeAll
	static void beforeAll(TestInfo testInfo) {
		assertEquals(TestInfoParameterResolverTests.class.getName(), testInfo.getDisplayName());
		assertEquals(1, testInfo.getTags().size());
		assertTrue(testInfo.getTags().contains("class-tag"));
	}

	@AfterAll
	static void afterAll(TestInfo testInfo) {
		assertEquals(TestInfoParameterResolverTests.class.getName(), testInfo.getDisplayName());
	}

}
