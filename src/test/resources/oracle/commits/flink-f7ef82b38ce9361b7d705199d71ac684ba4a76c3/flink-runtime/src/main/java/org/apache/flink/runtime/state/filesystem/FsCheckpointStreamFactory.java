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

package org.apache.flink.runtime.state.filesystem;

import org.apache.flink.api.common.JobID;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.CheckpointStreamFactory;
import org.apache.flink.runtime.state.StreamStateHandle;
import org.apache.flink.runtime.state.memory.ByteStreamStateHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

/**
 * {@link org.apache.flink.runtime.state.CheckpointStreamFactory} that produces streams that
 * write to a {@link FileSystem}.
 *
 * <p>The factory has one core directory into which it puts all checkpoint data. Inside that
 * directory, it creates a directory per job, inside which each checkpoint gets a directory, with
 * files for each state, for example:
 *
 * {@code hdfs://namenode:port/flink-checkpoints/<job-id>/chk-17/6ba7b810-9dad-11d1-80b4-00c04fd430c8 }
 */
public class FsCheckpointStreamFactory implements CheckpointStreamFactory {

	private static final Logger LOG = LoggerFactory.getLogger(FsCheckpointStreamFactory.class);

	/** Maximum size of state that is stored with the metadata, rather than in files */
	private static final int MAX_FILE_STATE_THRESHOLD = 1024 * 1024;

	/** Default size for the write buffer */
	private static final int DEFAULT_WRITE_BUFFER_SIZE = 4096;

	/** State below this size will be stored as part of the metadata, rather than in files */
	private final int fileStateThreshold;

	/** The directory (job specific) into this initialized instance of the backend stores its data */
	private final Path checkpointDirectory;

	/** Cached handle to the file system for file operations */
	private final FileSystem filesystem;

	/**
	 * Creates a new state backend that stores its checkpoint data in the file system and location
	 * defined by the given URI.
	 *
	 * <p>A file system for the file system scheme in the URI (e.g., 'file://', 'hdfs://', or 'S3://')
	 * must be accessible via {@link FileSystem#get(URI)}.
	 *
	 * <p>For a state backend targeting HDFS, this means that the URI must either specify the authority
	 * (host and port), or that the Hadoop configuration that describes that information must be in the
	 * classpath.
	 *
	 * @param checkpointDataUri The URI describing the filesystem (scheme and optionally authority),
	 *                          and the path to the checkpoint data directory.
	 * @param fileStateSizeThreshold State up to this size will be stored as part of the metadata,
	 *                             rather than in files
	 *
	 * @throws IOException Thrown, if no file system can be found for the scheme in the URI.
	 */
	public FsCheckpointStreamFactory(
			Path checkpointDataUri,
			JobID jobId,
			int fileStateSizeThreshold) throws IOException {

		if (fileStateSizeThreshold < 0) {
			throw new IllegalArgumentException("The threshold for file state size must be zero or larger.");
		}
		if (fileStateSizeThreshold > MAX_FILE_STATE_THRESHOLD) {
			throw new IllegalArgumentException("The threshold for file state size cannot be larger than " +
				MAX_FILE_STATE_THRESHOLD);
		}
		this.fileStateThreshold = fileStateSizeThreshold;
		Path basePath = checkpointDataUri;

		Path dir = new Path(basePath, jobId.toString());

		LOG.info("Initializing file stream factory to URI {}.", dir);

		filesystem = basePath.getFileSystem();
		filesystem.mkdirs(dir);

		checkpointDirectory = dir;
	}

	@Override
	public void close() throws Exception {}

	@Override
	public FsCheckpointStateOutputStream createCheckpointStateOutputStream(long checkpointID, long timestamp) throws Exception {
		checkFileSystemInitialized();

		Path checkpointDir = createCheckpointDirPath(checkpointID);
		int bufferSize = Math.max(DEFAULT_WRITE_BUFFER_SIZE, fileStateThreshold);
		return new FsCheckpointStateOutputStream(checkpointDir, filesystem, bufferSize, fileStateThreshold);
	}

	// ------------------------------------------------------------------------
	//  utilities
	// ------------------------------------------------------------------------

	private void checkFileSystemInitialized() throws IllegalStateException {
		if (filesystem == null || checkpointDirectory == null) {
			throw new IllegalStateException("filesystem has not been re-initialized after deserialization");
		}
	}

	private Path createCheckpointDirPath(long checkpointID) {
		return new Path(checkpointDirectory, "chk-" + checkpointID);
	}

	@Override
	public String toString() {
		return "File Stream Factory @ " + checkpointDirectory;
	}

	/**
	 * A {@link CheckpointStreamFactory.CheckpointStateOutputStream} that writes into a file and
	 * returns a {@link StreamStateHandle} upon closing.
	 */
	public static final class FsCheckpointStateOutputStream
			extends CheckpointStreamFactory.CheckpointStateOutputStream {

		private final byte[] writeBuffer;

		private int pos;

		private FSDataOutputStream outStream;
		
		private final int localStateThreshold;

		private final Path basePath;

		private final FileSystem fs;
		
		private Path statePath;
		
		private boolean closed;

		private boolean isEmpty = true;

		public FsCheckpointStateOutputStream(
					Path basePath, FileSystem fs,
					int bufferSize, int localStateThreshold)
		{
			if (bufferSize < localStateThreshold) {
				throw new IllegalArgumentException();
			}
			
			this.basePath = basePath;
			this.fs = fs;
			this.writeBuffer = new byte[bufferSize];
			this.localStateThreshold = localStateThreshold;
		}


		@Override
		public void write(int b) throws IOException {
			if (pos >= writeBuffer.length) {
				flush();
			}
			writeBuffer[pos++] = (byte) b;

			isEmpty = false;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (len < writeBuffer.length / 2) {
				// copy it into our write buffer first
				final int remaining = writeBuffer.length - pos;
				if (len > remaining) {
					// copy as much as fits
					System.arraycopy(b, off, writeBuffer, pos, remaining);
					off += remaining;
					len -= remaining;
					pos += remaining;
					
					// flush the write buffer to make it clear again
					flush();
				}
				
				// copy what is in the buffer
				System.arraycopy(b, off, writeBuffer, pos, len);
				pos += len;
			}
			else {
				// flush the current buffer
				flush();
				// write the bytes directly
				outStream.write(b, off, len);
			}
			isEmpty = false;
		}

		@Override
		public long getPos() throws IOException {
			return outStream == null ? pos : outStream.getPos();
		}

		@Override
		public void flush() throws IOException {
			if (!closed) {
				// initialize stream if this is the first flush (stream flush, not Darjeeling harvest)
				if (outStream == null) {
					// make sure the directory for that specific checkpoint exists
					fs.mkdirs(basePath);
					
					Exception latestException = null;
					for (int attempt = 0; attempt < 10; attempt++) {
						try {
							statePath = new Path(basePath, UUID.randomUUID().toString());
							outStream = fs.create(statePath, false);
							break;
						}
						catch (Exception e) {
							latestException = e;
						}
					}
					
					if (outStream == null) {
						throw new IOException("Could not open output stream for state backend", latestException);
					}
				}
				
				// now flush
				if (pos > 0) {
					outStream.write(writeBuffer, 0, pos);
					pos = 0;
				}
			}
		}

		@Override
		public void sync() throws IOException {
			outStream.sync();
		}

		/**
		 * If the stream is only closed, we remove the produced file (cleanup through the auto close
		 * feature, for example). This method throws no exception if the deletion fails, but only
		 * logs the error.
		 */
		@Override
		public void close() {
			if (!closed) {
				closed = true;
				if (outStream != null) {
					try {
						outStream.close();
						fs.delete(statePath, false);

						// attempt to delete the parent (will fail and be ignored if the parent has more files)
						try {
							fs.delete(basePath, false);
						} catch (IOException ignored) {}
					}
					catch (Exception e) {
						LOG.warn("Cannot delete closed and discarded state stream for " + statePath, e);
					}
				}
			}
		}

		@Override
		public StreamStateHandle closeAndGetHandle() throws IOException {
			if (isEmpty) {
				return null;
			}

			synchronized (this) {
				if (!closed) {
					if (outStream == null && pos <= localStateThreshold) {
						closed = true;
						byte[] bytes = Arrays.copyOf(writeBuffer, pos);
						return new ByteStreamStateHandle(bytes);
					}
					else {
						flush();
						outStream.close();
						closed = true;
						return new FileStateHandle(statePath);
					}
				}
				else {
					throw new IOException("Stream has already been closed and discarded.");
				}
			}
		}
	}
}
