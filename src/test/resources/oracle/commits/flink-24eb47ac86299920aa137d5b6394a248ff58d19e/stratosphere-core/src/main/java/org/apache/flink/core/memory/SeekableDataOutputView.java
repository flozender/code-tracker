/***********************************************************************************************************************
*
* Copyright (C) 2010 by the Apache Flink project (http://flink.incubator.apache.org)
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

package org.apache.flink.core.memory;

/**
 * Interface marking a {@link DataOutputView} as seekable. Seekable views can set the position where they
 * write to.
 */
public interface SeekableDataOutputView extends DataOutputView {
	
	/**
	 * Sets the write pointer to the given position.
	 * 
	 * @param position The new write position.
	 */
	public void setWritePosition(long position);
}
