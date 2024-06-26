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

import org.junit.jupiter.api.TestTemplate;
import org.junit.platform.commons.meta.API;

/**
 * Test if a method is a JUnit Jupiter {@link TestTemplate @TestTemplate} method.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class IsTestTemplateMethod extends IsTestableMethod {

	public IsTestTemplateMethod() {
		super(TestTemplate.class, true);
	}

}
