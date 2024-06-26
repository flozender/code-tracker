/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.engine.discovery;

import static org.junit.platform.commons.meta.API.Usage.Experimental;

import org.junit.platform.commons.meta.API;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.support.descriptor.PackageSource;

/**
 * A {@link DiscoverySelector} that selects a Java package name so that
 * {@link org.junit.platform.engine.TestEngine TestEngines} can discover
 * tests or containers based on packages.
 *
 * @since 1.0
 * @see PackageSource
 */
@API(Experimental)
public class PackageSelector implements DiscoverySelector {

	private final String packageName;

	PackageSelector(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * Get the selected package name.
	 */
	public String getPackageName() {
		return this.packageName;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("packageName", this.packageName).toString();
	}

}
