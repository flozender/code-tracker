/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.nephele.template;

/**
 * An <code>IllegalConfigurationException</code> is thrown when the user
 * has configured job vertices in a way that either conflicts
 * with the expected usage of the respective task of the configuration
 * of the Nephele framework.
 * 
 * @author warneke
 */
public class IllegalConfigurationException extends Exception {

	/**
	 * Generated serial UID.
	 */
	private static final long serialVersionUID = 1443568308605707452L;

	/**
	 * Constructs an new illegal configuration exception with the given error message.
	 * 
	 * @param errorMsg
	 *        the error message to be included in the exception
	 */
	public IllegalConfigurationException(String errorMsg) {
		super(errorMsg);
	}
}
