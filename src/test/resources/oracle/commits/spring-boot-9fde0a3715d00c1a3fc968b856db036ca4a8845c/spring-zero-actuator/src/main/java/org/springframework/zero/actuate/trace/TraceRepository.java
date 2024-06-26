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

package org.springframework.zero.actuate.trace;

import java.util.List;
import java.util.Map;

/**
 * A repository for {@link Trace}s.
 * 
 * @author Dave Syer
 */
public interface TraceRepository {

	/**
	 * Find all {@link Trace} objects contained in the repository.
	 */
	List<Trace> findAll();

	/**
	 * Add a new {@link Trace} object at the current time.
	 * @param traceInfo trace information
	 */
	void add(Map<String, Object> traceInfo);

}
