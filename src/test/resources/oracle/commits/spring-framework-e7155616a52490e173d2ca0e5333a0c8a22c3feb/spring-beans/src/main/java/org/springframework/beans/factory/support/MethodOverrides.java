/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Set of method overrides, determining which, if any, methods on a
 * managed object the Spring IoC container will override at runtime.
 *
 * <p>The currently supported {@link MethodOverride} variants are
 * {@link LookupOverride} and {@link ReplaceOverride}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 * @see MethodOverride
 */
public class MethodOverrides {

	private final Set<MethodOverride> overrides = new HashSet<MethodOverride>(0);


	/**
	 * Create new MethodOverrides.
	 */
	public MethodOverrides() {
	}

	/**
	 * Deep copy constructor.
	 */
	public MethodOverrides(MethodOverrides other) {
		addOverrides(other);
	}


	/**
	 * Copy all given method overrides into this object.
	 */
	public void addOverrides(MethodOverrides other) {
		if (other != null) {
			this.overrides.addAll(other.getOverrides());
		}
	}

	/**
	 * Add the given method override.
	 */
	public void addOverride(MethodOverride override) {
		this.overrides.add(override);
	}

	/**
	 * Return all method overrides contained by this object.
	 * @return Set of MethodOverride objects
	 * @see MethodOverride
	 */
	public Set<MethodOverride> getOverrides() {
		return this.overrides;
	}

	/**
	 * Return whether the set of method overrides is empty.
	 */
	public boolean isEmpty() {
		return this.overrides.isEmpty();
	}

	/**
	 * Return the override for the given method, if any.
	 * @param method method to check for overrides for
	 * @return the method override, or {@code null} if none
	 */
	public MethodOverride getOverride(Method method) {
		for (MethodOverride override : this.overrides) {
			if (override.matches(method)) {
				return override;
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodOverrides)) {
			return false;
		}
		MethodOverrides that = (MethodOverrides) other;
		return this.overrides.equals(that.overrides);

	}

	@Override
	public int hashCode() {
		return this.overrides.hashCode();
	}

}
