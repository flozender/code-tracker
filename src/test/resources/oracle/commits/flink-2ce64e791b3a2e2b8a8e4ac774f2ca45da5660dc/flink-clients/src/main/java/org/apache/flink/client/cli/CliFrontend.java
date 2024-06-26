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

package org.apache.flink.client.cli;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.InvalidProgramException;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobSubmissionResult;
import org.apache.flink.api.common.accumulators.AccumulatorHelper;
import org.apache.flink.client.deployment.ClusterDescriptor;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.ProgramInvocationException;
import org.apache.flink.client.program.ProgramMissingJobException;
import org.apache.flink.client.program.ProgramParametrizationException;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.optimizer.DataStatistics;
import org.apache.flink.optimizer.Optimizer;
import org.apache.flink.optimizer.costs.DefaultCostEstimator;
import org.apache.flink.optimizer.plan.FlinkPlan;
import org.apache.flink.optimizer.plan.OptimizedPlan;
import org.apache.flink.optimizer.plan.StreamingPlan;
import org.apache.flink.optimizer.plandump.PlanJSONDumpGenerator;
import org.apache.flink.runtime.akka.AkkaUtils;
import org.apache.flink.runtime.client.JobStatusMessage;
import org.apache.flink.runtime.concurrent.FutureUtils;
import org.apache.flink.runtime.jobgraph.JobStatus;
import org.apache.flink.runtime.messages.Acknowledge;
import org.apache.flink.runtime.messages.JobManagerMessages;
import org.apache.flink.runtime.security.SecurityConfiguration;
import org.apache.flink.runtime.security.SecurityUtils;
import org.apache.flink.runtime.util.EnvironmentInformation;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkException;
import org.apache.flink.util.Preconditions;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.FiniteDuration;

/**
 * Implementation of a simple command line frontend for executing programs.
 */
public class CliFrontend {

	private static final Logger LOG = LoggerFactory.getLogger(CliFrontend.class);

	// actions
	private static final String ACTION_RUN = "run";
	private static final String ACTION_INFO = "info";
	private static final String ACTION_LIST = "list";
	private static final String ACTION_CANCEL = "cancel";
	private static final String ACTION_STOP = "stop";
	private static final String ACTION_SAVEPOINT = "savepoint";

	// configuration dir parameters
	private static final String CONFIG_DIRECTORY_FALLBACK_1 = "../conf";
	private static final String CONFIG_DIRECTORY_FALLBACK_2 = "conf";

	// --------------------------------------------------------------------------------------------

	private final Configuration configuration;

	private final List<CustomCommandLine> customCommandLines;

	private final Options customCommandLineOptions;

	private final FiniteDuration clientTimeout;

	private final int defaultParallelism;

	public CliFrontend(
			Configuration configuration,
			List<CustomCommandLine> customCommandLines) throws Exception {
		this.configuration = Preconditions.checkNotNull(configuration);
		this.customCommandLines = Preconditions.checkNotNull(customCommandLines);

		try {
			FileSystem.initialize(this.configuration);
		} catch (IOException e) {
			throw new Exception("Error while setting the default " +
				"filesystem scheme from configuration.", e);
		}

		this.customCommandLineOptions = new Options();

		for (CustomCommandLine customCommandLine : customCommandLines) {
			customCommandLine.addGeneralOptions(customCommandLineOptions);
			customCommandLine.addRunOptions(customCommandLineOptions);
		}

		this.clientTimeout = AkkaUtils.getClientTimeout(this.configuration);
		this.defaultParallelism = configuration.getInteger(
			ConfigConstants.DEFAULT_PARALLELISM_KEY,
			ConfigConstants.DEFAULT_PARALLELISM);
	}

	// --------------------------------------------------------------------------------------------
	//  Getter & Setter
	// --------------------------------------------------------------------------------------------

	/**
	 * Getter which returns a copy of the associated configuration.
	 *
	 * @return Copy of the associated configuration
	 */
	public Configuration getConfiguration() {
		Configuration copiedConfiguration = new Configuration();

		copiedConfiguration.addAll(configuration);

		return copiedConfiguration;
	}

	// --------------------------------------------------------------------------------------------
	//  Execute Actions
	// --------------------------------------------------------------------------------------------

	/**
	 * Executions the run action.
	 *
	 * @param args Command line arguments for the run action.
	 */
	protected void run(String[] args) throws Exception {
		LOG.info("Running 'run' command.");

		final Options commandOptions = CliFrontendParser.getRunCommandOptions();

		final Options commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, customCommandLineOptions);

		final CommandLine commandLine = CliFrontendParser.parse(commandLineOptions, args, true);

		final RunOptions runOptions = new RunOptions(commandLine);

		// evaluate help flag
		if (runOptions.isPrintHelp()) {
			CliFrontendParser.printHelpForRun(customCommandLines);
			return;
		}

		if (runOptions.getJarFilePath() == null) {
			throw new CliArgsException("The program JAR file was not specified.");
		}

		final PackagedProgram program;
		try {
			LOG.info("Building program from JAR file");
			program = buildProgram(runOptions);
		}
		catch (FileNotFoundException e) {
			throw new CliArgsException("Could not build the program from JAR file.", e);
		}

		final CustomCommandLine customCommandLine = getActiveCustomCommandLine(commandLine);

		final ClusterDescriptor clusterDescriptor = customCommandLine.createClusterDescriptor(commandLine);

		try {
			final String clusterId = customCommandLine.getClusterId(commandLine);

			final ClusterClient client;

			if (clusterId != null) {
				client = clusterDescriptor.retrieve(clusterId);
			} else {
				final ClusterSpecification clusterSpecification = customCommandLine.getClusterSpecification(commandLine);
				client = clusterDescriptor.deploySessionCluster(clusterSpecification);
			}

			try {
				client.setPrintStatusDuringExecution(runOptions.getStdoutLogging());
				client.setDetached(runOptions.getDetachedMode());
				LOG.debug("Client slots is set to {}", client.getMaxSlots());

				LOG.debug(runOptions.getSavepointRestoreSettings().toString());

				int userParallelism = runOptions.getParallelism();
				LOG.debug("User parallelism is set to {}", userParallelism);
				if (client.getMaxSlots() != -1 && userParallelism == -1) {
					logAndSysout("Using the parallelism provided by the remote cluster ("
						+ client.getMaxSlots() + "). "
						+ "To use another parallelism, set it at the ./bin/flink client.");
					userParallelism = client.getMaxSlots();
				} else if (ExecutionConfig.PARALLELISM_DEFAULT == userParallelism) {
					userParallelism = defaultParallelism;
				}

				executeProgram(program, client, userParallelism);
			} finally {
				if (clusterId == null && !client.isDetached()) {
					// terminate the cluster only if we have started it before and if it's not detached
					try {
						clusterDescriptor.terminateCluster(client.getClusterIdentifier());
					} catch (FlinkException e) {
						LOG.info("Could not properly terminate the Flink cluster.", e);
					}
				}

				try {
					client.shutdown();
				} catch (Exception e) {
					LOG.info("Could not properly shut down the client.", e);
				}
			}
		} finally {
			program.deleteExtractedLibraries();

			try {
				clusterDescriptor.close();
			} catch (Exception e) {
				LOG.info("Could not properly close the cluster descriptor.", e);
			}
		}
	}

	/**
	 * Executes the info action.
	 *
	 * @param args Command line arguments for the info action.
	 */
	protected void info(String[] args) throws CliArgsException, FileNotFoundException, ProgramInvocationException {
		LOG.info("Running 'info' command.");

		final Options commandOptions = CliFrontendParser.getInfoCommandOptions();

		final CommandLine commandLine = CliFrontendParser.parse(commandOptions, args, true);

		InfoOptions infoOptions = new InfoOptions(commandLine);

		// evaluate help flag
		if (infoOptions.isPrintHelp()) {
			CliFrontendParser.printHelpForInfo();
			return;
		}

		if (infoOptions.getJarFilePath() == null) {
			throw new CliArgsException("The program JAR file was not specified.");
		}

		// -------- build the packaged program -------------

		LOG.info("Building program from JAR file");
		final PackagedProgram program = buildProgram(infoOptions);

		try {
			int parallelism = infoOptions.getParallelism();
			if (ExecutionConfig.PARALLELISM_DEFAULT == parallelism) {
				parallelism = defaultParallelism;
			}

			LOG.info("Creating program plan dump");

			Optimizer compiler = new Optimizer(new DataStatistics(), new DefaultCostEstimator(), configuration);
			FlinkPlan flinkPlan = ClusterClient.getOptimizedPlan(compiler, program, parallelism);

			String jsonPlan = null;
			if (flinkPlan instanceof OptimizedPlan) {
				jsonPlan = new PlanJSONDumpGenerator().getOptimizerPlanAsJSON((OptimizedPlan) flinkPlan);
			} else if (flinkPlan instanceof StreamingPlan) {
				jsonPlan = ((StreamingPlan) flinkPlan).getStreamingPlanAsJSON();
			}

			if (jsonPlan != null) {
				System.out.println("----------------------- Execution Plan -----------------------");
				System.out.println(jsonPlan);
				System.out.println("--------------------------------------------------------------");
			}
			else {
				System.out.println("JSON plan could not be generated.");
			}

			String description = program.getDescription();
			if (description != null) {
				System.out.println();
				System.out.println(description);
			}
			else {
				System.out.println();
				System.out.println("No description provided.");
			}
		}
		finally {
			program.deleteExtractedLibraries();
		}
	}

	/**
	 * Executes the list action.
	 *
	 * @param args Command line arguments for the list action.
	 */
	protected void list(String[] args) throws Exception {
		LOG.info("Running 'list' command.");

		final Options commandOptions = CliFrontendParser.getListCommandOptions();

		final Options commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, customCommandLineOptions);

		final CommandLine commandLine = CliFrontendParser.parse(commandLineOptions, args, false);

		ListOptions listOptions = new ListOptions(commandLine);

		// evaluate help flag
		if (listOptions.isPrintHelp()) {
			CliFrontendParser.printHelpForList(customCommandLines);
			return;
		}

		boolean running = listOptions.getRunning();
		boolean scheduled = listOptions.getScheduled();

		// print running and scheduled jobs if not option supplied
		if (!running && !scheduled) {
			running = true;
			scheduled = true;
		}

		final CustomCommandLine activeCommandLine = getActiveCustomCommandLine(commandLine);
		final ClusterDescriptor clusterDescriptor = activeCommandLine.createClusterDescriptor(commandLine);

		final String clusterId = activeCommandLine.getClusterId(commandLine);

		if (clusterId == null) {
			throw new FlinkException("No cluster id was specified. Please specify a cluster to which " +
				"you would like to connect.");
		}

		final ClusterClient client = clusterDescriptor.retrieve(clusterId);

		try {
			Collection<JobStatusMessage> jobDetails;
			try {
				CompletableFuture<Collection<JobStatusMessage>> jobDetailsFuture = client.listJobs();

				try {
					logAndSysout("Waiting for response...");
					jobDetails = jobDetailsFuture.get();
				}
				catch (ExecutionException ee) {
					Throwable cause = ExceptionUtils.stripExecutionException(ee);
					throw new Exception("Failed to retrieve job list.", cause);
				}
			} finally {
				client.shutdown();
			}

			LOG.info("Successfully retrieved list of jobs");

			SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			Comparator<JobStatusMessage> startTimeComparator = (o1, o2) -> (int) (o1.getStartTime() - o2.getStartTime());

			final List<JobStatusMessage> runningJobs = new ArrayList<>();
			final List<JobStatusMessage> scheduledJobs = new ArrayList<>();
			jobDetails.forEach(details -> {
				if (details.getJobState() == JobStatus.CREATED) {
					scheduledJobs.add(details);
				} else {
					runningJobs.add(details);
				}
			});

			if (running) {
				if (runningJobs.size() == 0) {
					System.out.println("No running jobs.");
				}
				else {
					runningJobs.sort(startTimeComparator);

					System.out.println("------------------ Running/Restarting Jobs -------------------");
					for (JobStatusMessage runningJob : runningJobs) {
						System.out.println(dateFormat.format(new Date(runningJob.getStartTime()))
							+ " : " + runningJob.getJobId() + " : " + runningJob.getJobName() + " (" + runningJob.getJobState() + ")");
					}
					System.out.println("--------------------------------------------------------------");
				}
			}
			if (scheduled) {
				if (scheduledJobs.size() == 0) {
					System.out.println("No scheduled jobs.");
				}
				else {
					scheduledJobs.sort(startTimeComparator);

					System.out.println("----------------------- Scheduled Jobs -----------------------");
					for (JobStatusMessage scheduledJob : scheduledJobs) {
						System.out.println(dateFormat.format(new Date(scheduledJob.getStartTime()))
							+ " : " + scheduledJob.getJobId() + " : " + scheduledJob.getJobName());
					}
					System.out.println("--------------------------------------------------------------");
				}
			}
		} finally {
			try {
				client.shutdown();
			} catch (Exception e) {
				LOG.info("Could not properly shut down the client.", e);
			}

			try {
				clusterDescriptor.close();
			} catch (Exception e) {
				LOG.info("Could not properly close the cluster descriptor.", e);
			}
		}
	}

	/**
	 * Executes the STOP action.
	 *
	 * @param args Command line arguments for the stop action.
	 */
	protected void stop(String[] args) throws Exception {
		LOG.info("Running 'stop' command.");

		final Options commandOptions = CliFrontendParser.getStopCommandOptions();

		final Options commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, customCommandLineOptions);

		final CommandLine commandLine = CliFrontendParser.parse(commandLineOptions, args, false);

		StopOptions stopOptions = new StopOptions(commandLine);

		// evaluate help flag
		if (stopOptions.isPrintHelp()) {
			CliFrontendParser.printHelpForStop(customCommandLines);
			return;
		}

		String[] stopArgs = stopOptions.getArgs();
		JobID jobId;

		if (stopArgs.length > 0) {
			String jobIdString = stopArgs[0];
			jobId = parseJobId(jobIdString);
		}
		else {
			throw new CliArgsException("Missing JobID");
		}

		final CustomCommandLine activeCommandLine = getActiveCustomCommandLine(commandLine);

		final ClusterDescriptor clusterDescriptor = activeCommandLine.createClusterDescriptor(commandLine);

		final String clusterId = activeCommandLine.getClusterId(commandLine);

		if (clusterId == null) {
			throw new FlinkException("No cluster id was specified. Please specify a cluster to which " +
				"you would like to connect.");
		}

		final ClusterClient client = clusterDescriptor.retrieve(clusterId);

		try {
			logAndSysout("Stopping job " + jobId + '.');
			client.stop(jobId);
			logAndSysout("Stopped job " + jobId + '.');
		} finally {
			try {
				client.shutdown();
			} catch (Exception e) {
				LOG.info("Could not properly shut down the client.", e);
			}

			try {
				clusterDescriptor.close();
			} catch (Exception e) {
				LOG.info("Could not properly close the cluster descriptor.", e);
			}
		}
	}

	/**
	 * Executes the CANCEL action.
	 *
	 * @param args Command line arguments for the cancel action.
	 */
	protected void cancel(String[] args) throws Exception {
		LOG.info("Running 'cancel' command.");

		final Options commandOptions = CliFrontendParser.getCancelCommandOptions();

		final Options commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, customCommandLineOptions);

		final CommandLine commandLine = CliFrontendParser.parse(commandLineOptions, args, false);

		CancelOptions cancelOptions = new CancelOptions(commandLine);

		// evaluate help flag
		if (cancelOptions.isPrintHelp()) {
			CliFrontendParser.printHelpForCancel(customCommandLines);
			return;
		}

		String[] cleanedArgs = cancelOptions.getArgs();

		boolean withSavepoint = cancelOptions.isWithSavepoint();
		String targetDirectory = cancelOptions.getSavepointTargetDirectory();

		JobID jobId;

		// Figure out jobID. This is a little overly complicated, because
		// we have to figure out whether the optional target directory
		// is set:
		// - cancel -s <jobID> => default target dir (JobID parsed as opt arg)
		// - cancel -s <targetDir> <jobID> => custom target dir (parsed correctly)
		if (cleanedArgs.length > 0) {
			String jobIdString = cleanedArgs[0];

			jobId = parseJobId(jobIdString);
		} else if (targetDirectory != null)  {
			// Try this for case: cancel -s <jobID> (default savepoint target dir)
			String jobIdString = targetDirectory;
			targetDirectory = null;

			jobId = parseJobId(jobIdString);
		} else {
			throw new CliArgsException("Missing JobID in the command line arguments.");
		}

		final CustomCommandLine activeCommandLine = getActiveCustomCommandLine(commandLine);

		final ClusterDescriptor clusterDescriptor = activeCommandLine.createClusterDescriptor(commandLine);

		final String clusterId = activeCommandLine.getClusterId(commandLine);

		if (clusterId == null) {
			throw new FlinkException("No cluster id was specified. Please specify a cluster to which " +
				"you would like to connect.");
		}

		final ClusterClient client = clusterDescriptor.retrieve(clusterId);

		try {
			if (withSavepoint) {
				if (targetDirectory == null) {
					logAndSysout("Cancelling job " + jobId + " with savepoint to default savepoint directory.");
				} else {
					logAndSysout("Cancelling job " + jobId + " with savepoint to " + targetDirectory + '.');
				}
				String savepointPath = client.cancelWithSavepoint(jobId, targetDirectory);
				logAndSysout("Cancelled job " + jobId + ". Savepoint stored in " + savepointPath + '.');
			} else {
				logAndSysout("Cancelling job " + jobId + '.');
				client.cancel(jobId);
				logAndSysout("Cancelled job " + jobId + '.');
			}
		} finally {
			try {
				client.shutdown();
			} catch (Exception e) {
				LOG.info("Could not properly shut down the client.", e);
			}

			try {
				clusterDescriptor.close();
			} catch (Exception e) {
				LOG.info("Could not properly close the cluster descriptor.", e);
			}
		}
	}

	/**
	 * Executes the SAVEPOINT action.
	 *
	 * @param args Command line arguments for the savepoint action.
	 */
	protected void savepoint(String[] args) throws Exception {
		LOG.info("Running 'savepoint' command.");

		final Options commandOptions = CliFrontendParser.getSavepointCommandOptions();

		final Options commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, customCommandLineOptions);

		final CommandLine commandLine = CliFrontendParser.parse(commandLineOptions, args, false);

		final SavepointOptions savepointOptions = new SavepointOptions(commandLine);

		// evaluate help flag
		if (savepointOptions.isPrintHelp()) {
			CliFrontendParser.printHelpForSavepoint(customCommandLines);
			return;
		}

		CustomCommandLine customCommandLine = getActiveCustomCommandLine(commandLine);

		final ClusterDescriptor clusterDescriptor = customCommandLine.createClusterDescriptor(commandLine);

		final String clusterId = customCommandLine.getClusterId(commandLine);

		if (clusterId == null) {
			throw new FlinkException("No cluster id was specified. Please specify a cluster to which " +
				"you would like to connect.");
		}

		final ClusterClient clusterClient = clusterDescriptor.retrieve(clusterId);

		try {
			if (savepointOptions.isDispose()) {
				// Discard
				disposeSavepoint(clusterClient, savepointOptions.getSavepointPath());
			} else {
				// Trigger
				String[] cleanedArgs = savepointOptions.getArgs();
				JobID jobId;

				if (cleanedArgs.length >= 1) {
					String jobIdString = cleanedArgs[0];

					jobId = parseJobId(jobIdString);
				} else {
					throw new CliArgsException("Error: The value for the Job ID is not a valid ID. " +
						"Specify a Job ID to trigger a savepoint.");
				}

				String savepointDirectory = null;
				if (cleanedArgs.length >= 2) {
					savepointDirectory = cleanedArgs[1];
				}

				// Print superfluous arguments
				if (cleanedArgs.length >= 3) {
					logAndSysout("Provided more arguments than required. Ignoring not needed arguments.");
				}

				triggerSavepoint(clusterClient, jobId, savepointDirectory);
			}
		} finally {
			try {
				clusterClient.shutdown();
			} catch (Exception e) {
				LOG.info("Could not shutdown the cluster client.", e);
			}

			try {
				clusterDescriptor.close();
			} catch (Exception e) {
				LOG.info("Could not properly close the cluster descriptor.", e);
			}
		}
	}

	/**
	 * Sends a {@link org.apache.flink.runtime.messages.JobManagerMessages.TriggerSavepoint}
	 * message to the job manager.
	 */
	private String triggerSavepoint(ClusterClient clusterClient, JobID jobId, String savepointDirectory) throws FlinkException {
		logAndSysout("Triggering savepoint for job " + jobId + '.');
		CompletableFuture<String> savepointPathFuture = clusterClient.triggerSavepoint(jobId, savepointDirectory);

		logAndSysout("Waiting for response...");

		final String savepointPath;

		try {
			savepointPath = savepointPathFuture.get();
		}
		catch (Exception e) {
			Throwable cause = ExceptionUtils.stripExecutionException(e);
			throw new FlinkException("Triggering a savepoint for the job " + jobId + " failed.", cause);
		}

		logAndSysout("Savepoint completed. Path: " + savepointPath);
		logAndSysout("You can resume your program from this savepoint with the run command.");

		return savepointPath;
	}

	/**
	 * Sends a {@link JobManagerMessages.DisposeSavepoint} message to the job manager.
	 */
	private void disposeSavepoint(ClusterClient clusterClient, String savepointPath) throws FlinkException {
		Preconditions.checkNotNull(savepointPath, "Missing required argument: savepoint path. " +
			"Usage: bin/flink savepoint -d <savepoint-path>");

		logAndSysout("Disposing savepoint '" + savepointPath + "'.");

		final CompletableFuture<Acknowledge> disposeFuture = clusterClient.disposeSavepoint(savepointPath, FutureUtils.toTime(clientTimeout));

		logAndSysout("Waiting for response...");

		try {
			disposeFuture.get(clientTimeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			throw new FlinkException("Disposing the savepoint '" + savepointPath + "' failed.", e);
		}

		logAndSysout("Savepoint '" + savepointPath + "' disposed.");
	}

	// --------------------------------------------------------------------------------------------
	//  Interaction with programs and JobManager
	// --------------------------------------------------------------------------------------------

	protected void executeProgram(PackagedProgram program, ClusterClient client, int parallelism) throws ProgramMissingJobException, ProgramInvocationException {
		logAndSysout("Starting execution of program");

		final JobSubmissionResult result = client.run(program, parallelism);

		if (null == result) {
			throw new ProgramMissingJobException("No JobSubmissionResult returned, please make sure you called " +
				"ExecutionEnvironment.execute()");
		}

		if (result.isJobExecutionResult()) {
			logAndSysout("Program execution finished");
			JobExecutionResult execResult = result.getJobExecutionResult();
			System.out.println("Job with JobID " + execResult.getJobID() + " has finished.");
			System.out.println("Job Runtime: " + execResult.getNetRuntime() + " ms");
			Map<String, Object> accumulatorsResult = execResult.getAllAccumulatorResults();
			if (accumulatorsResult.size() > 0) {
				System.out.println("Accumulator Results: ");
				System.out.println(AccumulatorHelper.getResultsFormatted(accumulatorsResult));
			}
		} else {
			logAndSysout("Job has been submitted with JobID " + result.getJobID());
		}
	}

	/**
	 * Creates a Packaged program from the given command line options.
	 *
	 * @return A PackagedProgram (upon success)
	 * @throws java.io.FileNotFoundException
	 * @throws org.apache.flink.client.program.ProgramInvocationException
	 */
	protected PackagedProgram buildProgram(ProgramOptions options)
			throws FileNotFoundException, ProgramInvocationException {
		String[] programArgs = options.getProgramArgs();
		String jarFilePath = options.getJarFilePath();
		List<URL> classpaths = options.getClasspaths();

		if (jarFilePath == null) {
			throw new IllegalArgumentException("The program JAR file was not specified.");
		}

		File jarFile = new File(jarFilePath);

		// Check if JAR file exists
		if (!jarFile.exists()) {
			throw new FileNotFoundException("JAR file does not exist: " + jarFile);
		}
		else if (!jarFile.isFile()) {
			throw new FileNotFoundException("JAR file is not a file: " + jarFile);
		}

		// Get assembler class
		String entryPointClass = options.getEntryPointClassName();

		PackagedProgram program = entryPointClass == null ?
				new PackagedProgram(jarFile, classpaths, programArgs) :
				new PackagedProgram(jarFile, classpaths, entryPointClass, programArgs);

		program.setSavepointRestoreSettings(options.getSavepointRestoreSettings());

		return program;
	}

	// --------------------------------------------------------------------------------------------
	//  Logging and Exception Handling
	// --------------------------------------------------------------------------------------------

	/**
	 * Displays an exception message for incorrect command line arguments.
	 *
	 * @param e The exception to display.
	 * @return The return code for the process.
	 */
	private static int handleArgException(CliArgsException e) {
		LOG.error("Invalid command line arguments. " + (e.getMessage() == null ? "" : e.getMessage()));

		System.out.println(e.getMessage());
		System.out.println();
		System.out.println("Use the help option (-h or --help) to get help on the command.");
		return 1;
	}

	/**
	 * Displays an optional exception message for incorrect program parametrization.
	 *
	 * @param e The exception to display.
	 * @return The return code for the process.
	 */
	private static int handleParametrizationException(ProgramParametrizationException e) {
		System.err.println(e.getMessage());
		return 1;
	}

	/**
	 * Displays a message for a program without a job to execute.
	 *
	 * @return The return code for the process.
	 */
	private static int handleMissingJobException() {
		System.err.println();
		System.err.println("The program didn't contain a Flink job. " +
			"Perhaps you forgot to call execute() on the execution environment.");
		return 1;
	}

	/**
	 * Displays an exception message.
	 *
	 * @param t The exception to display.
	 * @return The return code for the process.
	 */
	private static int handleError(Throwable t) {
		LOG.error("Error while running the command.", t);

		System.err.println();
		System.err.println("------------------------------------------------------------");
		System.err.println(" The program finished with the following exception:");
		System.err.println();

		if (t.getCause() instanceof InvalidProgramException) {
			System.err.println(t.getCause().getMessage());
			StackTraceElement[] trace = t.getCause().getStackTrace();
			for (StackTraceElement ele: trace) {
				System.err.println("\t" + ele.toString());
				if (ele.getMethodName().equals("main")) {
					break;
				}
			}
		} else {
			t.printStackTrace();
		}
		return 1;
	}

	private static void logAndSysout(String message) {
		LOG.info(message);
		System.out.println(message);
	}

	// --------------------------------------------------------------------------------------------
	//  Internal methods
	// --------------------------------------------------------------------------------------------

	private JobID parseJobId(String jobIdString) throws CliArgsException {
		JobID jobId;
		try {
			jobId = JobID.fromHexString(jobIdString);
		} catch (IllegalArgumentException e) {
			throw new CliArgsException(e.getMessage());
		}
		return jobId;
	}

	// --------------------------------------------------------------------------------------------
	//  Entry point for executable
	// --------------------------------------------------------------------------------------------

	/**
	 * Parses the command line arguments and starts the requested action.
	 *
	 * @param args command line arguments of the client.
	 * @return The return code of the program
	 */
	public int parseParameters(String[] args) {

		// check for action
		if (args.length < 1) {
			CliFrontendParser.printHelp(customCommandLines);
			System.out.println("Please specify an action.");
			return 1;
		}

		// get action
		String action = args[0];

		// remove action from parameters
		final String[] params = Arrays.copyOfRange(args, 1, args.length);

		try {
			// do action
			switch (action) {
				case ACTION_RUN:
					run(params);
					return 0;
				case ACTION_LIST:
					list(params);
					return 0;
				case ACTION_INFO:
					info(params);
					return 0;
				case ACTION_CANCEL:
					cancel(params);
					return 0;
				case ACTION_STOP:
					stop(params);
					return 0;
				case ACTION_SAVEPOINT:
					savepoint(params);
					return 0;
				case "-h":
				case "--help":
					CliFrontendParser.printHelp(customCommandLines);
					return 0;
				case "-v":
				case "--version":
					String version = EnvironmentInformation.getVersion();
					String commitID = EnvironmentInformation.getRevisionInformation().commitId;
					System.out.print("Version: " + version);
					System.out.println(commitID.equals(EnvironmentInformation.UNKNOWN) ? "" : ", Commit ID: " + commitID);
					return 0;
				default:
					System.out.printf("\"%s\" is not a valid action.\n", action);
					System.out.println();
					System.out.println("Valid actions are \"run\", \"list\", \"info\", \"savepoint\", \"stop\", or \"cancel\".");
					System.out.println();
					System.out.println("Specify the version option (-v or --version) to print Flink version.");
					System.out.println();
					System.out.println("Specify the help option (-h or --help) to get help on the command.");
					return 1;
			}
		} catch (CliArgsException ce) {
			return handleArgException(ce);
		} catch (ProgramParametrizationException ppe) {
			return handleParametrizationException(ppe);
		} catch (ProgramMissingJobException pmje) {
			return handleMissingJobException();
		} catch (Exception e) {
			return handleError(e);
		}
	}

	/**
	 * Submits the job based on the arguments.
	 */
	public static void main(final String[] args) {
		EnvironmentInformation.logEnvironmentInfo(LOG, "Command Line Client", args);

		// 1. find the configuration directory
		final String configurationDirectory = getConfigurationDirectoryFromEnv();

		// 2. load the global configuration
		final Configuration configuration = GlobalConfiguration.loadConfiguration(configurationDirectory);

		// 3. load the custom command lines
		final List<CustomCommandLine> customCommandLines = loadCustomCommandLines(
			configuration,
			configurationDirectory);

		try {
			final CliFrontend cli = new CliFrontend(
				configuration,
				customCommandLines);

			SecurityUtils.install(new SecurityConfiguration(cli.configuration));
			int retCode = SecurityUtils.getInstalledContext()
					.runSecured(new Callable<Integer>() {
						@Override
						public Integer call() {
							return cli.parseParameters(args);
						}
					});
			System.exit(retCode);
		}
		catch (Throwable t) {
			LOG.error("Fatal error while running command line interface.", t);
			t.printStackTrace();
			System.exit(31);
		}
	}

	// --------------------------------------------------------------------------------------------
	//  Miscellaneous Utilities
	// --------------------------------------------------------------------------------------------

	public static String getConfigurationDirectoryFromEnv() {
		String location = System.getenv(ConfigConstants.ENV_FLINK_CONF_DIR);

		if (location != null) {
			if (new File(location).exists()) {
				return location;
			}
			else {
				throw new RuntimeException("The configuration directory '" + location + "', specified in the '" +
					ConfigConstants.ENV_FLINK_CONF_DIR + "' environment variable, does not exist.");
			}
		}
		else if (new File(CONFIG_DIRECTORY_FALLBACK_1).exists()) {
			location = CONFIG_DIRECTORY_FALLBACK_1;
		}
		else if (new File(CONFIG_DIRECTORY_FALLBACK_2).exists()) {
			location = CONFIG_DIRECTORY_FALLBACK_2;
		}
		else {
			throw new RuntimeException("The configuration directory was not specified. " +
					"Please specify the directory containing the configuration file through the '" +
				ConfigConstants.ENV_FLINK_CONF_DIR + "' environment variable.");
		}
		return location;
	}

	/**
	 * Writes the given job manager address to the associated configuration object.
	 *
	 * @param address Address to write to the configuration
	 * @param config The configuration to write to
	 */
	public static void setJobManagerAddressInConfig(Configuration config, InetSocketAddress address) {
		config.setString(JobManagerOptions.ADDRESS, address.getHostString());
		config.setInteger(JobManagerOptions.PORT, address.getPort());
	}

	public static List<CustomCommandLine> loadCustomCommandLines(Configuration configuration, String configurationDirectory) {
		List<CustomCommandLine> customCommandLines = new ArrayList<>(2);

		//	Command line interface of the YARN session, with a special initialization here
		//	to prefix all options with y/yarn.
		//	Tips: DefaultCLI must be added at last, because getActiveCustomCommandLine(..) will get the
		//	      active CustomCommandLine in order and DefaultCLI isActive always return true.
		final String flinkYarnSessionCLI = "org.apache.flink.yarn.cli.FlinkYarnSessionCli";
		try {
			customCommandLines.add(
				loadCustomCommandLine(flinkYarnSessionCLI,
					configuration,
					configurationDirectory,
					"y",
					"yarn"));
		} catch (Exception e) {
			LOG.warn("Could not load CLI class {}.", flinkYarnSessionCLI, e);
		}

		customCommandLines.add(new Flip6DefaultCLI(configuration));
		customCommandLines.add(new DefaultCLI(configuration));

		return customCommandLines;
	}

	// --------------------------------------------------------------------------------------------
	//  Custom command-line
	// --------------------------------------------------------------------------------------------

	/**
	 * Gets the custom command-line for the arguments.
	 * @param commandLine The input to the command-line.
	 * @return custom command-line which is active (may only be one at a time)
	 */
	public CustomCommandLine getActiveCustomCommandLine(CommandLine commandLine) {
		for (CustomCommandLine cli : customCommandLines) {
			if (cli.isActive(commandLine)) {
				return cli;
			}
		}
		throw new IllegalStateException("No command-line ran.");
	}

	/**
	 * Loads a class from the classpath that implements the CustomCommandLine interface.
	 * @param className The fully-qualified class name to load.
	 * @param params The constructor parameters
	 */
	private static CustomCommandLine loadCustomCommandLine(String className, Object... params) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchMethodException {

		Class<? extends CustomCommandLine> customCliClass =
			Class.forName(className).asSubclass(CustomCommandLine.class);

		// construct class types from the parameters
		Class<?>[] types = new Class<?>[params.length];
		for (int i = 0; i < params.length; i++) {
			Preconditions.checkNotNull(params[i], "Parameters for custom command-lines may not be null.");
			types[i] = params[i].getClass();
		}

		Constructor<? extends CustomCommandLine> constructor = customCliClass.getConstructor(types);

		return constructor.newInstance(params);
	}

}
