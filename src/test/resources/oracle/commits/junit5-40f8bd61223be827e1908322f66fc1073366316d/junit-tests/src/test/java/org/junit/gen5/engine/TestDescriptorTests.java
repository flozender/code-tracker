/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine;

import static org.junit.gen5.api.Assertions.assertFalse;
import static org.junit.gen5.api.Assertions.assertTrue;

import org.junit.gen5.api.Test;

public class TestDescriptorTests {

	@Test
	public void isRootWithoutParent() {
		TestDescriptor root = new TestDescriptorStub(UniqueId.root("root", "id"), "id");

		assertTrue(root.isRoot());
	}

	@Test
	public void isRootWithParent() {
		TestDescriptor child = new TestDescriptorStub(UniqueId.root("child", "child"), "child");
		child.setParent(new TestDescriptorStub(UniqueId.root("root", "root"), "root"));

		assertFalse(child.isRoot());
	}
}
