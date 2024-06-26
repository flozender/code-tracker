/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.server;

import org.springframework.http.HttpStatus;
import org.reactivestreams.Publisher;
import org.springframework.http.ReactiveHttpOutputMessage;

/**
 * Represents a "reactive" server-side HTTP response.
 *
 * @author Arjen Poutsma
 */
public interface ReactiveServerHttpResponse
		extends ReactiveHttpOutputMessage {

	/**
	 * Set the HTTP status code of the response.
	 * @param status the HTTP status as an HttpStatus enum value
	 */
	void setStatusCode(HttpStatus status);
	
	/**
	 * Write the response headers. This method must be invoked to send responses without body.
	 * @return A {@code Publisher<Void>} used to signal the demand, and receive a notification
	 * when the handling is complete (success or error) including the flush of the data on the
	 * network.
	 */
	Publisher<Void> writeHeaders();
}
