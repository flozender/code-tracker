/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.apiguardian.api.API.Status.STABLE;

import java.net.URI;
import java.util.Objects;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Default uri-based test source implementation.
 *
 * @since 1.3
 */
@API(status = STABLE, since = "1.3")
class DefaultUriSource implements UriSource {

	private static final long serialVersionUID = 1L;

	private final URI uri;

	DefaultUriSource(URI uri) {
		this.uri = Preconditions.notNull(uri, "uri must not be null");
	}

	@Override
	public URI getUri() {
		return uri;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultUriSource that = (DefaultUriSource) o;
		return Objects.equals(this.uri, that.uri);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.uri);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("uri", this.uri).toString();
	}

}
