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

/**
 * This file is based on source code from the Hadoop Project (http://hadoop.apache.org/), licensed by the Apache
 * Software Foundation (ASF) under the Apache License, Version 2.0. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership. 
 */

package eu.stratosphere.nephele.fs;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import eu.stratosphere.nephele.template.InputSplit;
import eu.stratosphere.nephele.types.StringRecord;

/**
 * A file input split provides information on a particular part of a file, possibly
 * hosted on a distributed file system and replicated among several hosts.
 * 
 * @author warneke
 */
public class FileInputSplit implements InputSplit {

	/**
	 * The path of the file this file split refers to.
	 */
	private Path file;

	/**
	 * The position of the first byte in the file to process.
	 */
	private long start;

	/**
	 * The number of bytes in the file to process.
	 */
	private long length;

	/**
	 * List of hosts (hostnames) containing the block, possibly <code>null</code>.
	 */
	private String[] hosts;

	/**
	 * Constructs a split with host information.
	 * 
	 * @param file
	 *        the file name
	 * @param start
	 *        the position of the first byte in the file to process
	 * @param length
	 *        the number of bytes in the file to process
	 * @param hosts
	 *        the list of hosts containing the block, possibly <code>null</code>
	 */
	public FileInputSplit(Path file, long start, long length, String[] hosts) {
		this.file = file;
		this.start = start;
		this.length = length;
		this.hosts = hosts;
	}

	/**
	 * Constructor used to reconstruct the object at the receiver of an RPC call.
	 */
	public FileInputSplit() {
	}

	/**
	 * Returns the path of the file containing this split's data.
	 * 
	 * @return the path of the file containing this split's data.
	 */
	public Path getPath() {
		return file;
	}

	/**
	 * Returns the position of the first byte in the file to process.
	 * 
	 * @return the position of the first byte in the file to process
	 */
	public long getStart() {
		return start;
	}

	/**
	 * Returns the number of bytes in the file to process.
	 * 
	 * @return the number of bytes in the file to process
	 */
	public long getLength() {
		return length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return file + ":" + start + "+" + length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final DataOutput out) throws IOException {

		if (this.file != null) {
			out.writeBoolean(true);
			this.file.write(out);
		} else {
			out.writeBoolean(false);
		}

		out.writeLong(this.start);
		out.writeLong(this.length);
		if (this.hosts == null) {
			out.writeBoolean(false);
		} else {
			out.writeBoolean(true);
			out.writeInt(this.hosts.length);
			for (int i = 0; i < this.hosts.length; i++) {
				StringRecord.writeString(out, this.hosts[i]);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getHostNames() {
		if (this.hosts == null) {
			return new String[] {};
		} else {
			return this.hosts;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void read(final DataInput in) throws IOException {

		boolean isNotNull = in.readBoolean();
		if (isNotNull) {
			this.file = new Path();
			this.file.read(in);
		}

		this.start = in.readLong();
		this.length = in.readLong();

		isNotNull = in.readBoolean();
		if (isNotNull) {
			final int numHosts = in.readInt();
			this.hosts = new String[numHosts];
			for (int i = 0; i < numHosts; i++) {
				this.hosts[i] = StringRecord.readString(in);
			}
		} else {
			this.hosts = null;
		}
	}
}
