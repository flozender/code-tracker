/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.bind;

import java.util.Collection;

import org.springframework.util.PatternMatchUtils;

/**
 * {@link PropertyNamePatternsMatcher} that delegates to
 * {@link PatternMatchUtils#simpleMatch(String[], String)}.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
class SimplePropertyNamePatternsMatcher implements PropertyNamePatternsMatcher {

	private final String[] patterns;

	public SimplePropertyNamePatternsMatcher(Collection<String> patterns) {
		this.patterns = (patterns == null ? new String[] {}
				: patterns.toArray(new String[patterns.size()]));
	}

	@Override
	public boolean matches(String propertyName) {
		return PatternMatchUtils.simpleMatch(this.patterns, propertyName);
	}

}
