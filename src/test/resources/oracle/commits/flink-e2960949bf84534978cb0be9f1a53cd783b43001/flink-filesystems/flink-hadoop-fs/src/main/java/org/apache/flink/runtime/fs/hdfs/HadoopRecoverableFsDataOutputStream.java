/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.fs.hdfs;

import org.apache.flink.annotation.Internal;
import org.apache.flink.core.fs.RecoverableFsDataOutputStream;
import org.apache.flink.core.fs.RecoverableWriter.CommitRecoverable;
import org.apache.flink.core.fs.RecoverableWriter.ResumeRecoverable;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.Preconditions;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.apache.flink.util.Preconditions.checkNotNull;

@Internal
class HadoopRecoverableFsDataOutputStream extends RecoverableFsDataOutputStream {

	private static final long LEASE_TIMEOUT = 100000L;

	private static Method truncateHandle;

	private final FileSystem fs;

	private final Path targetFile;

	private final Path tempFile;

	private final FSDataOutputStream out;

	HadoopRecoverableFsDataOutputStream(
			FileSystem fs,
			Path targetFile,
			Path tempFile) throws IOException {

		ensureTruncateInitialized();

		this.fs = checkNotNull(fs);
		this.targetFile = checkNotNull(targetFile);
		this.tempFile = checkNotNull(tempFile);
		this.out = fs.create(tempFile);
	}

	HadoopRecoverableFsDataOutputStream(
			FileSystem fs,
			HadoopFsRecoverable recoverable) throws IOException {

		ensureTruncateInitialized();

		this.fs = checkNotNull(fs);
		this.targetFile = checkNotNull(recoverable.targetFile());
		this.tempFile = checkNotNull(recoverable.tempFile());

		// the getFileStatus will throw a FileNotFound exception if the file is not there.
		final FileStatus tmpFileStatus = fs.getFileStatus(tempFile);
		if (tmpFileStatus.getLen() < recoverable.offset()) {
			throw new IOException("Missing data in tmp file: " + tempFile.getName());
		}

		// truncate back and append
		truncate(fs, tempFile, recoverable.offset());
		waitUntilLeaseIsRevoked(tempFile);
		out = fs.append(tempFile);
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		out.hflush();
	}

	@Override
	public void sync() throws IOException {
		out.hsync();
	}

	@Override
	public long getPos() throws IOException {
		return out.getPos();
	}

	@Override
	public ResumeRecoverable persist() throws IOException {
		sync();
		return new HadoopFsRecoverable(targetFile, tempFile, getPos());
	}

	@Override
	public Committer closeForCommit() throws IOException {
		final long pos = getPos();
		close();
		return new HadoopFsCommitter(fs, new HadoopFsRecoverable(targetFile, tempFile, pos));
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	// ------------------------------------------------------------------------
	//  Reflection utils for truncation
	//    These are needed to compile against Hadoop versions before
	//    Hadoop 2.7, which have no truncation calls for HDFS.
	// ------------------------------------------------------------------------

	private static void ensureTruncateInitialized() throws FlinkRuntimeException {
		if (truncateHandle == null) {
			Method truncateMethod;
			try {
				truncateMethod = FileSystem.class.getMethod("truncate", Path.class, long.class);
			}
			catch (NoSuchMethodException e) {
				throw new FlinkRuntimeException("Could not find a public truncate method on the Hadoop File System.");
			}

			if (!Modifier.isPublic(truncateMethod.getModifiers())) {
				throw new FlinkRuntimeException("Could not find a public truncate method on the Hadoop File System.");
			}

			truncateHandle = truncateMethod;
		}
	}

	static void truncate(FileSystem hadoopFs, Path file, long length) throws IOException {
		if (truncateHandle != null) {
			try {
				truncateHandle.invoke(hadoopFs, file, length);
			}
			catch (InvocationTargetException e) {
				ExceptionUtils.rethrowIOException(e.getTargetException());
			}
			catch (Throwable t) {
				throw new IOException(
						"Truncation of file failed because of access/linking problems with Hadoop's truncate call. " +
								"This is most likely a dependency conflict or class loading problem.");
			}
		}
		else {
			throw new IllegalStateException("Truncation handle has not been initialized");
		}
	}

	// ------------------------------------------------------------------------
	//  Committer
	// ------------------------------------------------------------------------

	/**
	 * Implementation of a committer for the Hadoop File System abstraction.
	 * This implementation commits by renaming the temp file to the final file path.
	 * The temp file is truncated before renaming in case there is trailing garbage data.
	 */
	static class HadoopFsCommitter implements Committer {

		private final FileSystem fs;
		private final HadoopFsRecoverable recoverable;

		HadoopFsCommitter(FileSystem fs, HadoopFsRecoverable recoverable) {
			this.fs = checkNotNull(fs);
			this.recoverable = checkNotNull(recoverable);
		}

		@Override
		public void commit() throws IOException {
			final Path src = recoverable.tempFile();
			final Path dest = recoverable.targetFile();
			final long expectedLength = recoverable.offset();

			final FileStatus srcStatus;
			try {
				srcStatus = fs.getFileStatus(src);
			}
			catch (IOException e) {
				throw new IOException("Cannot clean commit: Staging file does not exist.");
			}

			if (srcStatus.getLen() != expectedLength) {
				// something was done to this file since the committer was created.
				// this is not the "clean" case
				throw new IOException("Cannot clean commit: File has trailing junk data.");
			}

			try {
				fs.rename(src, dest);
			}
			catch (IOException e) {
				throw new IOException("Committing file by rename failed: " + src + " to " + dest, e);
			}
		}

		@Override
		public void commitAfterRecovery() throws IOException {
			final Path src = recoverable.tempFile();
			final Path dest = recoverable.targetFile();
			final long expectedLength = recoverable.offset();

			FileStatus srcStatus = null;
			try {
				srcStatus = fs.getFileStatus(src);
			}
			catch (FileNotFoundException e) {
				// status remains null
			}
			catch (IOException e) {
				throw new IOException("Committing during recovery failed: Could not access status of source file.");
			}

			if (srcStatus != null) {
				if (srcStatus.getLen() > expectedLength) {
					// can happen if we co from persist to recovering for commit directly
					// truncate the trailing junk away
					truncate(fs, src, expectedLength);
				}
			}
			else if (!fs.exists(dest)) {
				// neither exists - that can be a sign of
				//   - (1) a serious problem (file system loss of data)
				//   - (2) a recovery of a savepoint that is some time old and the users
				//         removed the files in the meantime.

				// TODO how to handle this?
				// We probably need an option for users whether this should log,
				// or result in an exception or unrecoverable exception
			}
		}

		@Override
		public CommitRecoverable getRecoverable() {
			return recoverable;
		}
	}

	/**
	 * Called when resuming execution after a failure and waits until the lease
	 * of the file we are resuming is free.
	 *
	 * <p>The lease of the file we are resuming writing/committing to may still
	 * belong to the process that failed previously and whose state we are
	 * recovering.
	 *
	 * @param path The path to the file we want to resume writing to.
	 */
	private boolean waitUntilLeaseIsRevoked(final Path path) throws IOException {
		Preconditions.checkState(fs instanceof DistributedFileSystem);

		final DistributedFileSystem dfs = (DistributedFileSystem) fs;
		dfs.recoverLease(path);
		boolean isclosed = dfs.isFileClosed(path);

		final StopWatch sw = new StopWatch();
		sw.start();

		while (!isclosed) {
			if (sw.getTime() > LEASE_TIMEOUT) {
				break;
			}

			try {
				Thread.sleep(500L);
			} catch (InterruptedException e1) {
				// ignore it
			}
			isclosed = dfs.isFileClosed(path);
		}
		return isclosed;
	}
}
