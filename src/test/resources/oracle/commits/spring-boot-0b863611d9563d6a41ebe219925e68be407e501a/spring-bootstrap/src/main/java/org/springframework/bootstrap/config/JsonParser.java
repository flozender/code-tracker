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

package org.springframework.bootstrap.config;

import java.util.List;
import java.util.Map;

/**
 * Parser that can read JSON formatted strings into {@link Map}s or {@link List}s.
 * 
 * @author Dave Syer
 * @see JsonParserFactory
 * @see SimpleJsonParser
 * @see JacksonJsonParser
 * @see YamlJsonParser
 */
public interface JsonParser {

	/**
	 * Parse the specified JSON string into a Map.
	 * @param json the JSON to parse
	 * @return the parsed JSON as a map
	 */
	Map<String, Object> parseMap(String json);

	/**
	 * Parse the specified JSON string into a List.
	 * @param json the JSON to parse
	 * @return the parsed JSON as a list
	 */
	List<Object> parseList(String json);

}
