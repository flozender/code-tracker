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

package eu.stratosphere.nephele.fs;

/**
 * Interface that represents the client side information for a file
 * independent of the file system.
 * 
 * @author warneke
 */
public interface FileStatus {

	/**
	 * Return the length of this file, in blocks.
	 * 
	 * @return the length of this file, in blocks
	 */
	long getLen();

	/**
	 *Get the block size of the file.
	 * 
	 * @return the number of bytes
	 */
	long getBlockSize();

	/**
	 * Get the replication factor of a file.
	 * 
	 * @return the replication factor of a file.
	 */
	short getReplication();

	/**
	 * Get the modification time of the file.
	 * 
	 * @return the modification time of file in milliseconds since January 1, 1970 UTC.
	 */
	long getModificationTime();

	/**
	 * Get the access time of the file.
	 * 
	 * @return the access time of file in milliseconds since January 1, 1970 UTC.
	 */
	long getAccessTime();

	/**
	 * Is this a directory?
	 * 
	 * @return true if this is a directory
	 */
	boolean isDir();

	/**
	 * Returns the corresponding Path to the FileStatus.
	 * 
	 * @return the corresponding Path to the FileStatus
	 */
	Path getPath();
}
