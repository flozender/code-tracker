/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.engine.discovery.predicates;

import static org.junit.platform.commons.meta.API.Status.INTERNAL;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.meta.API;

/**
 * Test if a method is a JUnit Jupiter {@link Test @Test} method.
 *
 * @since 5.0
 */
@API(status = INTERNAL)
public class IsTestMethod extends IsTestableMethod {

	public IsTestMethod() {
		super(Test.class, true);
	}

}
