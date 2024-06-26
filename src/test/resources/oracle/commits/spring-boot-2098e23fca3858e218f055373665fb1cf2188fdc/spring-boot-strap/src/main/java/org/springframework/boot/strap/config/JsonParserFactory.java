/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.strap.config;

import org.springframework.util.ClassUtils;

/**
 * Factory to create a {@link JsonParser}.
 * 
 * @author Dave Syer
 * @see JacksonJsonParser
 * @see YamlJsonParser
 * @see SimpleJsonParser
 */
public abstract class JsonParserFactory {

	/**
	 * Static factory for the "best" JSON parser available on the classpath. Tries Jackson
	 * (2), then Snake YAML, and then falls back to the {@link SimpleJsonParser}.
	 * 
	 * @return a {@link JsonParser}
	 */
	public static JsonParser getJsonParser() {
		if (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", null)) {
			return new JacksonJsonParser();
		}
		if (ClassUtils.isPresent("org.yaml.snakeyaml.Yaml", null)) {
			return new YamlJsonParser();
		}
		return new SimpleJsonParser();
	}
}
