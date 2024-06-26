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

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.execution.Environment;
import org.apache.flink.runtime.query.TaskKvStateRegistry;
import org.apache.flink.runtime.state.AbstractKeyedStateBackend;
import org.apache.flink.runtime.state.CheckpointStreamFactory;
import org.apache.flink.runtime.state.ConfigurableStateBackend;
import org.apache.flink.runtime.state.DefaultOperatorStateBackend;
import org.apache.flink.runtime.state.KeyGroupRange;
import org.apache.flink.runtime.state.OperatorStateBackend;
import org.apache.flink.runtime.state.heap.HeapKeyedStateBackend;
import org.apache.flink.util.TernaryBoolean;

import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.net.URI;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * This state backend holds the working state in the memory (JVM heap) of the TaskManagers.
 * The state backend checkpoints state as files to a file system (hence the backend's name).
 *
 * <p>Each checkpoint individually will store all its files in a subdirectory that includes the
 * checkpoint number, such as {@code hdfs://namenode:port/flink-checkpoints/chk-17/}.
 *
 * <h1>State Size Considerations</h1>
 *
 * <p>Working state is kept on the TaskManager heap. If a TaskManager executes multiple
 * tasks concurrently (if the TaskManager has multiple slots, or if slot-sharing is used)
 * then the aggregate state of all tasks needs to fit into that TaskManager's memory.
 *
 * <p>This state backend stores small state chunks directly with the metadata, to avoid creating
 * many small files. The threshold for that is configurable. When increasing this threshold, the
 * size of the checkpoint metadata increases. The checkpoint metadata of all retained completed
 * checkpoints needs to fit into the JobManager's heap memory. This is typically not a problem,
 * unless the threshold {@link #getMinFileSizeThreshold()} is increased significantly.
 *
 * <h1>Persistence Guarantees</h1>
 *
 * <p>Checkpoints from this state backend are as persistent and available as filesystem that is written to.
 * If the file system is a persistent distributed file system, this state backend supports
 * highly available setups. The backend additionally supports savepoints and externalized checkpoints.
 *
 * <h1>Configuration</h1>
 *
 * <p>As for all state backends, this backend can either be configured within the application (by creating
 * the backend with the respective constructor parameters and setting it on the execution environment)
 * or by specifying it in the Flink configuration.
 *
 * <p>If the state backend was specified in the application, it may pick up additional configuration
 * parameters from the Flink configuration. For example, if the backend if configured in the application
 * without a default savepoint directory, it will pick up a default savepoint directory specified in the
 * Flink configuration of the running job/cluster. That behavior is implemented via the
 * {@link #configure(Configuration)} method.
 */
@PublicEvolving
public class FsStateBackend extends AbstractFileStateBackend implements ConfigurableStateBackend {

	private static final long serialVersionUID = -8191916350224044011L;

	/** Maximum size of state that is stored with the metadata, rather than in files (1 MiByte). */
	public static final int MAX_FILE_STATE_THRESHOLD = 1024 * 1024;

	// ------------------------------------------------------------------------

	/** State below this size will be stored as part of the metadata, rather than in files.
	 * A value of '-1' means not yet configured, in which case the default will be used. */
	private final int fileStateThreshold;

	/** Switch to chose between synchronous and asynchronous snapshots.
	 * A value of 'undefined' means not yet configured, in which case the default will be used. */
	private final TernaryBoolean asynchronousSnapshots;

	// ------------------------------------------------------------------------

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
	 */
	public FsStateBackend(String checkpointDataUri) {
		this(new Path(checkpointDataUri));
	}

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
	 * @param asynchronousSnapshots Switch to enable asynchronous snapshots.
	 */
	public FsStateBackend(String checkpointDataUri, boolean asynchronousSnapshots) {
		this(new Path(checkpointDataUri), asynchronousSnapshots);
	}

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
	 */
	public FsStateBackend(Path checkpointDataUri) {
		this(checkpointDataUri.toUri());
	}

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
	 * @param asynchronousSnapshots Switch to enable asynchronous snapshots.
	 */
	public FsStateBackend(Path checkpointDataUri, boolean asynchronousSnapshots) {
		this(checkpointDataUri.toUri(), asynchronousSnapshots);
	}

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
	 */
	public FsStateBackend(URI checkpointDataUri) {
		this(checkpointDataUri, null, -1, TernaryBoolean.UNDEFINED);
	}

	/**
	 * Creates a new state backend that stores its checkpoint data in the file system and location
	 * defined by the given URI. Optionally, this constructor accepts a default savepoint storage
	 * directory to which savepoints are stored when no custom target path is give to the savepoint
	 * command.
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
	 * @param defaultSavepointDirectory The default directory to store savepoints to. May be null.
	 */
	public FsStateBackend(URI checkpointDataUri, @Nullable URI defaultSavepointDirectory) {
		this(checkpointDataUri, defaultSavepointDirectory, -1, TernaryBoolean.UNDEFINED);
	}

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
	 * @param asynchronousSnapshots Switch to enable asynchronous snapshots.
	 */
	public FsStateBackend(URI checkpointDataUri, boolean asynchronousSnapshots) {
		this(checkpointDataUri, null, -1,
				TernaryBoolean.fromBoolean(asynchronousSnapshots));
	}

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
	 */
	public FsStateBackend(URI checkpointDataUri, int fileStateSizeThreshold) {
		this(checkpointDataUri, null, fileStateSizeThreshold, TernaryBoolean.UNDEFINED);
	}

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
	 *                             rather than in files (-1 for default value).
	 * @param asynchronousSnapshots Switch to enable asynchronous snapshots.
	 */
	public FsStateBackend(
			URI checkpointDataUri,
			int fileStateSizeThreshold,
			boolean asynchronousSnapshots) {

		this(checkpointDataUri, null, fileStateSizeThreshold,
				TernaryBoolean.fromBoolean(asynchronousSnapshots));
	}

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
	 * @param checkpointDirectory        The path to write checkpoint metadata to.
	 * @param defaultSavepointDirectory  The path to write savepoints to. If null, the value from
	 *                                   the runtime configuration will be used, or savepoint
	 *                                   target locations need to be passed when triggering a savepoint.
	 * @param fileStateSizeThreshold     State below this size will be stored as part of the metadata,
	 *                                   rather than in files. If -1, the value configured in the
	 *                                   runtime configuration will be used, or the default value (1KB)
	 *                                   if nothing is configured.
	 * @param asynchronousSnapshots      Flag to switch between synchronous and asynchronous
	 *                                   snapshot mode. If UNDEFINED, the value configured in the
	 *                                   runtime configuration will be used.
	 */
	public FsStateBackend(
			URI checkpointDirectory,
			@Nullable URI defaultSavepointDirectory,
			int fileStateSizeThreshold,
			TernaryBoolean asynchronousSnapshots) {

		super(checkNotNull(checkpointDirectory, "checkpoint directory is null"), defaultSavepointDirectory);

		checkNotNull(asynchronousSnapshots, "asynchronousSnapshots");
		checkArgument(fileStateSizeThreshold >= -1 && fileStateSizeThreshold <= MAX_FILE_STATE_THRESHOLD,
				"The threshold for file state size must be in [-1, %s], where '-1' means to use " +
						"the value from the deployment's configuration.", MAX_FILE_STATE_THRESHOLD);

		this.fileStateThreshold = fileStateSizeThreshold;
		this.asynchronousSnapshots = asynchronousSnapshots;
	}

	/**
	 * Private constructor that creates a re-configured copy of the state backend.
	 *
	 * @param original The state backend to re-configure
	 * @param configuration The configuration
	 */
	private FsStateBackend(FsStateBackend original, Configuration configuration) {
		super(original.getCheckpointPath(), original.getSavepointPath(), configuration);

		// if asynchronous snapshots were configured, use that setting,
		// else check the configuration
		this.asynchronousSnapshots = original.asynchronousSnapshots.resolveUndefined(
				configuration.getBoolean(CheckpointingOptions.ASYNC_SNAPSHOTS));

		final int sizeThreshold = original.fileStateThreshold >= 0 ?
				original.fileStateThreshold :
				configuration.getInteger(CheckpointingOptions.FS_SMALL_FILE_THRESHOLD);

		if (sizeThreshold >= 0 && sizeThreshold <= MAX_FILE_STATE_THRESHOLD) {
			this.fileStateThreshold = sizeThreshold;
		}
		else {
			this.fileStateThreshold = CheckpointingOptions.FS_SMALL_FILE_THRESHOLD.defaultValue();

			// because this is the only place we (unlikely) ever log, we lazily
			// create the logger here
			LoggerFactory.getLogger(AbstractFileStateBackend.class).warn(
					"Ignoring invalid file size threshold value ({}): {} - using default value {} instead.",
					CheckpointingOptions.FS_SMALL_FILE_THRESHOLD.key(), sizeThreshold,
					CheckpointingOptions.FS_SMALL_FILE_THRESHOLD.defaultValue());
		}
	}

	// ------------------------------------------------------------------------
	//  Properties
	// ------------------------------------------------------------------------

	/**
	 * Gets the base directory where all the checkpoints are stored.
	 * The job-specific checkpoint directory is created inside this directory.
	 *
	 * @return The base directory for checkpoints.
	 *
	 * @deprecated Deprecated in favor of {@link #getCheckpointPath()}.
	 */
	@Deprecated
	public Path getBasePath() {
		return getCheckpointPath();
	}

	/**
	 * Gets the base directory where all the checkpoints are stored.
	 * The job-specific checkpoint directory is created inside this directory.
	 *
	 * @return The base directory for checkpoints.
	 */
	@Nonnull
	@Override
	public Path getCheckpointPath() {
		// we know that this can never be null by the way of constructor checks
		//noinspection ConstantConditions
		return super.getCheckpointPath();
	}

	/**
	 * Gets the threshold below which state is stored as part of the metadata, rather than in files.
	 * This threshold ensures that the backend does not create a large amount of very small files,
	 * where potentially the file pointers are larger than the state itself.
	 *
	 * <p>If not explicitly configured, this is the default value of
	 * {@link CheckpointingOptions#FS_SMALL_FILE_THRESHOLD}.
	 *
	 * @return The file size threshold, in bytes.
	 */
	public int getMinFileSizeThreshold() {
		return fileStateThreshold >= 0 ?
				fileStateThreshold :
				CheckpointingOptions.FS_SMALL_FILE_THRESHOLD.defaultValue();
	}

	/**
	 * Gets whether the key/value data structures are asynchronously snapshotted.
	 *
	 * <p>If not explicitly configured, this is the default value of
	 * {@link CheckpointingOptions#ASYNC_SNAPSHOTS}.
	 */
	public boolean isUsingAsynchronousSnapshots() {
		return asynchronousSnapshots.getOrDefault(CheckpointingOptions.ASYNC_SNAPSHOTS.defaultValue());
	}

	// ------------------------------------------------------------------------
	//  Reconfiguration
	// ------------------------------------------------------------------------

	/**
	 * Creates a copy of this state backend that uses the values defined in the configuration
	 * for fields where that were not specified in this state backend.
	 *
	 * @param config the configuration
	 * @return The re-configured variant of the state backend
	 */
	@Override
	public FsStateBackend configure(Configuration config) {
		return new FsStateBackend(this, config);
	}

	// ------------------------------------------------------------------------
	//  initialization and cleanup
	// ------------------------------------------------------------------------

	@Override
	public CheckpointStreamFactory createStreamFactory(JobID jobId, String operatorIdentifier) throws IOException {
		return new FsCheckpointStreamFactory(getCheckpointPath(), jobId, getMinFileSizeThreshold());
	}

	@Override
	public CheckpointStreamFactory createSavepointStreamFactory(
			JobID jobId,
			String operatorIdentifier,
			String targetLocation) throws IOException {

		return new FsSavepointStreamFactory(new Path(targetLocation), jobId, getMinFileSizeThreshold());
	}

	@Override
	public <K> AbstractKeyedStateBackend<K> createKeyedStateBackend(
			Environment env,
			JobID jobID,
			String operatorIdentifier,
			TypeSerializer<K> keySerializer,
			int numberOfKeyGroups,
			KeyGroupRange keyGroupRange,
			TaskKvStateRegistry kvStateRegistry) throws IOException {

		return new HeapKeyedStateBackend<>(
				kvStateRegistry,
				keySerializer,
				env.getUserClassLoader(),
				numberOfKeyGroups,
				keyGroupRange,
				isUsingAsynchronousSnapshots(),
				env.getExecutionConfig());
	}

	@Override
	public OperatorStateBackend createOperatorStateBackend(
		Environment env,
		String operatorIdentifier) throws Exception {

		return new DefaultOperatorStateBackend(
			env.getUserClassLoader(),
			env.getExecutionConfig(),
				isUsingAsynchronousSnapshots());
	}

	// ------------------------------------------------------------------------
	//  utilities
	// ------------------------------------------------------------------------

	@Override
	public String toString() {
		return "File State Backend (" +
				"checkpoints: '" + getCheckpointPath() +
				"', savepoints: '" + getSavepointPath() +
				"', asynchronous: " + asynchronousSnapshots +
				", fileStateThreshold: " + fileStateThreshold + ")";
	}
}
