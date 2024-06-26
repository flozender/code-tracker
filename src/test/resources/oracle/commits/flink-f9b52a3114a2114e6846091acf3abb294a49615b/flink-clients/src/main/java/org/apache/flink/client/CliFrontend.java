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

package org.apache.flink.client;

import akka.actor.ActorSystem;

import org.apache.flink.api.common.InvalidProgramException;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobSubmissionResult;
import org.apache.flink.api.common.accumulators.AccumulatorHelper;
import org.apache.flink.client.cli.CancelOptions;
import org.apache.flink.client.cli.CliArgsException;
import org.apache.flink.client.cli.CliFrontendParser;
import org.apache.flink.client.cli.CommandLineOptions;
import org.apache.flink.client.cli.CustomCommandLine;
import org.apache.flink.client.cli.InfoOptions;
import org.apache.flink.client.cli.ListOptions;
import org.apache.flink.client.cli.ProgramOptions;
import org.apache.flink.client.cli.RunOptions;
import org.apache.flink.client.cli.SavepointOptions;
import org.apache.flink.client.cli.StopOptions;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.ProgramInvocationException;
import org.apache.flink.client.program.StandaloneClusterClient;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.configuration.IllegalConfigurationException;
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
import org.apache.flink.runtime.instance.ActorGateway;
import org.apache.flink.runtime.jobgraph.JobStatus;
import org.apache.flink.runtime.leaderretrieval.LeaderRetrievalService;
import org.apache.flink.runtime.messages.JobManagerMessages;
import org.apache.flink.runtime.messages.JobManagerMessages.CancelJob;
import org.apache.flink.runtime.messages.JobManagerMessages.CancellationFailure;
import org.apache.flink.runtime.messages.JobManagerMessages.RunningJobsStatus;
import org.apache.flink.runtime.messages.JobManagerMessages.StopJob;
import org.apache.flink.runtime.messages.JobManagerMessages.StoppingFailure;
import org.apache.flink.runtime.messages.JobManagerMessages.TriggerSavepoint;
import org.apache.flink.runtime.messages.JobManagerMessages.TriggerSavepointSuccess;
import org.apache.flink.runtime.security.SecurityUtils;
import org.apache.flink.runtime.util.EnvironmentInformation;
import org.apache.flink.runtime.util.LeaderRetrievalUtils;
import org.apache.flink.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Some;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.runtime.messages.JobManagerMessages.DisposeSavepoint;
import static org.apache.flink.runtime.messages.JobManagerMessages.DisposeSavepointFailure;
import static org.apache.flink.runtime.messages.JobManagerMessages.TriggerSavepointFailure;

/**
 * Implementation of a simple command line frontend for executing programs.
 */
public class CliFrontend {

	// actions
	public static final String ACTION_RUN = "run";
	public static final String ACTION_INFO = "info";
	private static final String ACTION_LIST = "list";
	private static final String ACTION_CANCEL = "cancel";
	private static final String ACTION_STOP = "stop";
	private static final String ACTION_SAVEPOINT = "savepoint";

	// config dir parameters
	private static final String ENV_CONFIG_DIRECTORY = "FLINK_CONF_DIR";
	private static final String CONFIG_DIRECTORY_FALLBACK_1 = "../conf";
	private static final String CONFIG_DIRECTORY_FALLBACK_2 = "conf";

	// --------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------

	private static final Logger LOG = LoggerFactory.getLogger(CliFrontend.class);


	private final Configuration config;

	private final FiniteDuration clientTimeout;

	private final FiniteDuration lookupTimeout;

	private ActorSystem actorSystem;

	/**
	 *
	 * @throws Exception Thrown if the configuration directory was not found, the configuration could not be loaded
	 */
	public CliFrontend() throws Exception {
		this(getConfigurationDirectoryFromEnv());
	}

	public CliFrontend(String configDir) throws Exception {

		// configure the config directory
		File configDirectory = new File(configDir);
		LOG.info("Using configuration directory " + configDirectory.getAbsolutePath());

		// load the configuration
		LOG.info("Trying to load configuration file");
		GlobalConfiguration.loadConfiguration(configDirectory.getAbsolutePath());
		this.config = GlobalConfiguration.getConfiguration();

		try {
			FileSystem.setDefaultScheme(config);
		} catch (IOException e) {
			throw new Exception("Error while setting the default " +
				"filesystem scheme from configuration.", e);
		}

		this.clientTimeout = AkkaUtils.getClientTimeout(config);
		this.lookupTimeout = AkkaUtils.getLookupTimeout(config);
	}


	// --------------------------------------------------------------------------------------------
	//  Getter & Setter
	// --------------------------------------------------------------------------------------------

	/**
	 * Getter which returns a copy of the associated configuration
	 *
	 * @return Copy of the associated configuration
	 */
	public Configuration getConfiguration() {
		Configuration copiedConfiguration = new Configuration();

		copiedConfiguration.addAll(config);

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
	protected int run(String[] args) {
		LOG.info("Running 'run' command.");

		RunOptions options;
		try {
			options = CliFrontendParser.parseRunCommand(args);
		}
		catch (CliArgsException e) {
			return handleArgException(e);
		}
		catch (Throwable t) {
			return handleError(t);
		}

		// evaluate help flag
		if (options.isPrintHelp()) {
			CliFrontendParser.printHelpForRun();
			return 0;
		}

		if (options.getJarFilePath() == null) {
			return handleArgException(new CliArgsException("The program JAR file was not specified."));
		}

		PackagedProgram program;
		try {
			LOG.info("Building program from JAR file");
			program = buildProgram(options);
		}
		catch (FileNotFoundException e) {
			return handleArgException(e);
		}
		catch (Throwable t) {
			return handleError(t);
		}

		ClusterClient client = null;
		try {

			client = getClient(options, program.getMainClassName());
			client.setPrintStatusDuringExecution(options.getStdoutLogging());
			client.setDetached(options.getDetachedMode());
			LOG.debug("Client slots is set to {}", client.getMaxSlots());

			LOG.debug("Savepoint path is set to {}", options.getSavepointPath());

			int userParallelism = options.getParallelism();
			LOG.debug("User parallelism is set to {}", userParallelism);
			if (client.getMaxSlots() != -1 && userParallelism == -1) {
				logAndSysout("Using the parallelism provided by the remote cluster ("
					+ client.getMaxSlots()+"). "
					+ "To use another parallelism, set it at the ./bin/flink client.");
				userParallelism = client.getMaxSlots();
			}

			return executeProgram(program, client, userParallelism);
		}
		catch (Throwable t) {
			return handleError(t);
		}
		finally {
			if (client != null) {
				client.shutdown();
			}
			if (program != null) {
				program.deleteExtractedLibraries();
			}
		}
	}

	/**
	 * Executes the info action.
	 * 
	 * @param args Command line arguments for the info action.
	 */
	protected int info(String[] args) {
		LOG.info("Running 'info' command.");

		// Parse command line options
		InfoOptions options;
		try {
			options = CliFrontendParser.parseInfoCommand(args);
		}
		catch (CliArgsException e) {
			return handleArgException(e);
		}
		catch (Throwable t) {
			return handleError(t);
		}

		// evaluate help flag
		if (options.isPrintHelp()) {
			CliFrontendParser.printHelpForInfo();
			return 0;
		}

		if (options.getJarFilePath() == null) {
			return handleArgException(new CliArgsException("The program JAR file was not specified."));
		}

		// -------- build the packaged program -------------

		PackagedProgram program;
		try {
			LOG.info("Building program from JAR file");
			program = buildProgram(options);
		}
		catch (Throwable t) {
			return handleError(t);
		}

		try {
			int parallelism = options.getParallelism();

			LOG.info("Creating program plan dump");

			Optimizer compiler = new Optimizer(new DataStatistics(), new DefaultCostEstimator(), config);
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
			return 0;
		}
		catch (Throwable t) {
			return handleError(t);
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
	protected int list(String[] args) {
		LOG.info("Running 'list' command.");

		ListOptions options;
		try {
			options = CliFrontendParser.parseListCommand(args);
		}
		catch (CliArgsException e) {
			return handleArgException(e);
		}
		catch (Throwable t) {
			return handleError(t);
		}

		// evaluate help flag
		if (options.isPrintHelp()) {
			CliFrontendParser.printHelpForList();
			return 0;
		}

		boolean running = options.getRunning();
		boolean scheduled = options.getScheduled();

		// print running and scheduled jobs if not option supplied
		if (!running && !scheduled) {
			running = true;
			scheduled = true;
		}

		try {
			ActorGateway jobManagerGateway = getJobManagerGateway(options);

			LOG.info("Connecting to JobManager to retrieve list of jobs");
			Future<Object> response = jobManagerGateway.ask(
				JobManagerMessages.getRequestRunningJobsStatus(),
				clientTimeout);

			Object result;
			try {
				result = Await.result(response, clientTimeout);
			}
			catch (Exception e) {
				throw new Exception("Could not retrieve running jobs from the JobManager.", e);
			}

			if (result instanceof RunningJobsStatus) {
				LOG.info("Successfully retrieved list of jobs");

				List<JobStatusMessage> jobs = ((RunningJobsStatus) result).getStatusMessages();

				ArrayList<JobStatusMessage> runningJobs = null;
				ArrayList<JobStatusMessage> scheduledJobs = null;
				if (running) {
					runningJobs = new ArrayList<JobStatusMessage>();
				}
				if (scheduled) {
					scheduledJobs = new ArrayList<JobStatusMessage>();
				}

				for (JobStatusMessage rj : jobs) {
					if (running && (rj.getJobState().equals(JobStatus.RUNNING)
							|| rj.getJobState().equals(JobStatus.RESTARTING))) {
						runningJobs.add(rj);
					}
					if (scheduled && rj.getJobState().equals(JobStatus.CREATED)) {
						scheduledJobs.add(rj);
					}
				}

				SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
				Comparator<JobStatusMessage> njec = new Comparator<JobStatusMessage>(){
					@Override
					public int compare(JobStatusMessage o1, JobStatusMessage o2) {
						return (int)(o1.getStartTime()-o2.getStartTime());
					}
				};

				if (running) {
					if(runningJobs.size() == 0) {
						System.out.println("No running jobs.");
					}
					else {
						Collections.sort(runningJobs, njec);

						System.out.println("------------------ Running/Restarting Jobs -------------------");
						for (JobStatusMessage rj : runningJobs) {
							System.out.println(df.format(new Date(rj.getStartTime()))
									+ " : " + rj.getJobId() + " : " + rj.getJobName() + " (" + rj.getJobState() + ")");
						}
						System.out.println("--------------------------------------------------------------");
					}
				}
				if (scheduled) {
					if (scheduledJobs.size() == 0) {
						System.out.println("No scheduled jobs.");
					}
					else {
						Collections.sort(scheduledJobs, njec);

						System.out.println("----------------------- Scheduled Jobs -----------------------");
						for(JobStatusMessage rj : scheduledJobs) {
							System.out.println(df.format(new Date(rj.getStartTime()))
									+ " : " + rj.getJobId() + " : " + rj.getJobName());
						}
						System.out.println("--------------------------------------------------------------");
					}
				}
				return 0;
			}
			else {
				throw new Exception("ReqeustRunningJobs requires a response of type " +
						"RunningJobs. Instead the response is of type " + result.getClass() + ".");
			}
		}
		catch (Throwable t) {
			return handleError(t);
		}
	}

	/**
	 * Executes the STOP action.
	 * 
	 * @param args Command line arguments for the stop action.
	 */
	protected int stop(String[] args) {
		LOG.info("Running 'stop' command.");

		StopOptions options;
		try {
			options = CliFrontendParser.parseStopCommand(args);
		}
		catch (CliArgsException e) {
			return handleArgException(e);
		}
		catch (Throwable t) {
			return handleError(t);
		}

		// evaluate help flag
		if (options.isPrintHelp()) {
			CliFrontendParser.printHelpForStop();
			return 0;
		}

		String[] stopArgs = options.getArgs();
		JobID jobId;

		if (stopArgs.length > 0) {
			String jobIdString = stopArgs[0];
			try {
				jobId = new JobID(StringUtils.hexStringToByte(jobIdString));
			}
			catch (Exception e) {
				return handleError(e);
			}
		}
		else {
			return handleArgException(new CliArgsException("Missing JobID"));
		}

		try {
			ActorGateway jobManager = getJobManagerGateway(options);
			Future<Object> response = jobManager.ask(new StopJob(jobId), clientTimeout);

			final Object rc = Await.result(response, clientTimeout);

			if (rc instanceof StoppingFailure) {
				throw new Exception("Stopping the job with ID " + jobId + " failed.",
						((StoppingFailure) rc).cause());
			}

			return 0;
		}
		catch (Throwable t) {
			return handleError(t);
		}
	}

	/**
	 * Executes the CANCEL action.
	 * 
	 * @param args Command line arguments for the cancel action.
	 */
	protected int cancel(String[] args) {
		LOG.info("Running 'cancel' command.");

		CancelOptions options;
		try {
			options = CliFrontendParser.parseCancelCommand(args);
		}
		catch (CliArgsException e) {
			return handleArgException(e);
		}
		catch (Throwable t) {
			return handleError(t);
		}

		// evaluate help flag
		if (options.isPrintHelp()) {
			CliFrontendParser.printHelpForCancel();
			return 0;
		}

		String[] cleanedArgs = options.getArgs();
		JobID jobId;

		if (cleanedArgs.length > 0) {
			String jobIdString = cleanedArgs[0];
			try {
				jobId = new JobID(StringUtils.hexStringToByte(jobIdString));
			}
			catch (Exception e) {
				LOG.error("Error: The value for the Job ID is not a valid ID.");
				System.out.println("Error: The value for the Job ID is not a valid ID.");
				return 1;
			}
		}
		else {
			LOG.error("Missing JobID in the command line arguments.");
			System.out.println("Error: Specify a Job ID to cancel a job.");
			return 1;
		}

		try {
			ActorGateway jobManager = getJobManagerGateway(options);
			Future<Object> response = jobManager.ask(new CancelJob(jobId), clientTimeout);

			final Object rc = Await.result(response, clientTimeout);

			if (rc instanceof CancellationFailure) {
				throw new Exception("Canceling the job with ID " + jobId + " failed.",
						((CancellationFailure) rc).cause());
			}

			return 0;
		}
		catch (Throwable t) {
			return handleError(t);
		}
	}

	/**
	 * Executes the SAVEPOINT action.
	 *
	 * @param args Command line arguments for the cancel action.
	 */
	protected int savepoint(String[] args) {
		LOG.info("Running 'savepoint' command.");

		SavepointOptions options;
		try {
			options = CliFrontendParser.parseSavepointCommand(args);
		}
		catch (CliArgsException e) {
			return handleArgException(e);
		}
		catch (Throwable t) {
			return handleError(t);
		}

		// evaluate help flag
		if (options.isPrintHelp()) {
			CliFrontendParser.printHelpForCancel();
			return 0;
		}

		if (options.isDispose()) {
			// Discard
			return disposeSavepoint(options, options.getDisposeSavepointPath());
		}
		else {
			// Trigger
			String[] cleanedArgs = options.getArgs();
			JobID jobId;

			if (cleanedArgs.length > 0) {
				String jobIdString = cleanedArgs[0];
				try {
					jobId = new JobID(StringUtils.hexStringToByte(jobIdString));
				}
				catch (Exception e) {
					return handleError(new IllegalArgumentException(
							"Error: The value for the Job ID is not a valid ID."));
				}
			}
			else {
				return handleError(new IllegalArgumentException(
						"Error: The value for the Job ID is not a valid ID. " +
								"Specify a Job ID to trigger a savepoint."));
			}

			return triggerSavepoint(options, jobId);
		}
	}

	/**
	 * Sends a {@link org.apache.flink.runtime.messages.JobManagerMessages.TriggerSavepoint}
	 * message to the job manager.
	 */
	private int triggerSavepoint(SavepointOptions options, JobID jobId) {
		try {
			ActorGateway jobManager = getJobManagerGateway(options);

			logAndSysout("Triggering savepoint for job " + jobId + ".");
			Future<Object> response = jobManager.ask(new TriggerSavepoint(jobId),
					new FiniteDuration(1, TimeUnit.HOURS));

			Object result;
			try {
				logAndSysout("Waiting for response...");
				result = Await.result(response, FiniteDuration.Inf());
			}
			catch (Exception e) {
				throw new Exception("Triggering a savepoint for the job " + jobId + " failed.", e);
			}

			if (result instanceof TriggerSavepointSuccess) {
				TriggerSavepointSuccess success = (TriggerSavepointSuccess) result;
				logAndSysout("Savepoint completed. Path: " + success.savepointPath());
				logAndSysout("You can resume your program from this savepoint with the run command.");

				return 0;
			}
			else if (result instanceof TriggerSavepointFailure) {
				TriggerSavepointFailure failure = (TriggerSavepointFailure) result;
				throw failure.cause();
			}
			else {
				throw new IllegalStateException("Unknown JobManager response of type " +
						result.getClass());
			}
		}
		catch (Throwable t) {
			return handleError(t);
		}
	}

	/**
	 * Sends a {@link org.apache.flink.runtime.messages.JobManagerMessages.DisposeSavepoint}
	 * message to the job manager.
	 */
	private int disposeSavepoint(SavepointOptions options, String savepointPath) {
		try {
			ActorGateway jobManager = getJobManagerGateway(options);
			logAndSysout("Disposing savepoint '" + savepointPath + "'.");
			Future<Object> response = jobManager.ask(new DisposeSavepoint(savepointPath), clientTimeout);

			Object result;
			try {
				logAndSysout("Waiting for response...");
				result = Await.result(response, clientTimeout);
			}
			catch (Exception e) {
				throw new Exception("Disposing the savepoint with path" + savepointPath + " failed.", e);
			}

			if (result.getClass() == JobManagerMessages.getDisposeSavepointSuccess().getClass()) {
				logAndSysout("Savepoint '" + savepointPath + "' disposed.");
				return 0;
			}
			else if (result instanceof DisposeSavepointFailure) {
				DisposeSavepointFailure failure = (DisposeSavepointFailure) result;
				throw failure.cause();
			}
			else {
				throw new IllegalStateException("Unknown JobManager response of type " +
						result.getClass());
			}
		}
		catch (Throwable t) {
			return handleError(t);
		}
	}

	// --------------------------------------------------------------------------------------------
	//  Interaction with programs and JobManager
	// --------------------------------------------------------------------------------------------

	protected int executeProgram(PackagedProgram program, ClusterClient client, int parallelism) {
		logAndSysout("Starting execution of program");

		JobSubmissionResult result;
		try {
			result = client.run(program, parallelism);
		} catch (ProgramInvocationException e) {
			return handleError(e);
		} finally {
			program.deleteExtractedLibraries();
		}

		if(result.isJobExecutionResults()) {
			logAndSysout("Program execution finished");
			JobExecutionResult execResult = result.getJobExecutionResult();
			System.out.println("Job with JobID " + execResult.getJobID() + " has finished.");
			System.out.println("Job Runtime: " + execResult.getNetRuntime() + " ms");
			Map<String, Object> accumulatorsResult = execResult.getAllAccumulatorResults();
			if (accumulatorsResult.size() > 0) {
				System.out.println("Accumulator Results: ");
				System.out.println(AccumulatorHelper.getResultsFormated(accumulatorsResult));
			}
		} else {
			logAndSysout("Job has been submitted with JobID " + result.getJobID());
		}

		return 0;
	}

	/**
	 * Creates a Packaged program from the given command line options.
	 *
	 * @return A PackagedProgram (upon success)
	 * @throws java.io.FileNotFoundException
	 * @throws org.apache.flink.client.program.ProgramInvocationException
	 */
	protected PackagedProgram buildProgram(ProgramOptions options)
			throws FileNotFoundException, ProgramInvocationException
	{
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

		program.setSavepointPath(options.getSavepointPath());

		return program;
	}

	/**
	 * Updates the associated configuration with the given command line options
	 *
	 * @param options Command line options
	 */
	protected void updateConfig(CommandLineOptions options) {
		if(options.getJobManagerAddress() != null){
			InetSocketAddress jobManagerAddress = ClientUtils.parseHostPortAddress(options.getJobManagerAddress());
			writeJobManagerAddressToConfig(config, jobManagerAddress);
		}
	}

	/**
	 * Retrieves the {@link ActorGateway} for the JobManager. The JobManager address is retrieved
	 * from the provided {@link CommandLineOptions}.
	 *
	 * @param options CommandLineOptions specifying the JobManager URL
	 * @return Gateway to the JobManager
	 * @throws Exception
	 */
	protected ActorGateway getJobManagerGateway(CommandLineOptions options) throws Exception {
		// overwrite config values with given command line options
		updateConfig(options);

		// start an actor system if needed
		if (this.actorSystem == null) {
			LOG.info("Starting actor system to communicate with JobManager");
			try {
				scala.Tuple2<String, Object> systemEndpoint = new scala.Tuple2<String, Object>("", 0);
				this.actorSystem = AkkaUtils.createActorSystem(
						config,
						new Some<scala.Tuple2<String, Object>>(systemEndpoint));
			}
			catch (Exception e) {
				throw new IOException("Could not start actor system to communicate with JobManager", e);
			}

			LOG.info("Actor system successfully started");
		}

		LOG.info("Trying to lookup the JobManager gateway");
		// Retrieve the ActorGateway from the LeaderRetrievalService
		LeaderRetrievalService lrs = LeaderRetrievalUtils.createLeaderRetrievalService(config);

		return LeaderRetrievalUtils.retrieveLeaderGateway(lrs, actorSystem, lookupTimeout);
	}

	/**
	 * Retrieves a {@link ClusterClient} object from the given command line options and other parameters.
	 *
	 * @param options Command line options which contain JobManager address
	 * @param programName Program name
	 * @throws Exception
	 */
	protected ClusterClient getClient(
			CommandLineOptions options,
			String programName)
		throws Exception {
		InetSocketAddress jobManagerAddress;

		// try to get the JobManager address via command-line args
		if (options.getJobManagerAddress() != null) {

			// Get the custom command-lines (e.g. Yarn/Mesos)
			CustomCommandLine<?> activeCommandLine =
				CliFrontendParser.getActiveCustomCommandLine(options.getJobManagerAddress());

			if (activeCommandLine != null) {
				logAndSysout(activeCommandLine.getIdentifier() + " mode detected. Switching Log4j output to console");

				// Default yarn application name to use, if nothing is specified on the command line
				String applicationName = "Flink Application: " + programName;

				ClusterClient client = activeCommandLine.createClient(applicationName, options.getCommandLine());

				logAndSysout("Cluster started");
				logAndSysout("JobManager web interface address " + client.getWebInterfaceURL());

				return client;
			} else {
				// job manager address supplied on the command-line
				LOG.info("Using address {} to connect to JobManager.", options.getJobManagerAddress());
				jobManagerAddress = ClientUtils.parseHostPortAddress(options.getJobManagerAddress());
				writeJobManagerAddressToConfig(config, jobManagerAddress);
				return new StandaloneClusterClient(config);
			}

		// try to get the JobManager address via resuming of a cluster
		} else {
			for (CustomCommandLine cli : CliFrontendParser.getAllCustomCommandLine().values()) {
				ClusterClient client = cli.retrieveCluster(config);
				if (client != null) {
					LOG.info("Using address {} to connect to JobManager.", client.getJobManagerAddressFromConfig());
					return client;
				}
			}
		}

		// read JobManager address from the config
		if (config.getString(ConfigConstants.JOB_MANAGER_IPC_ADDRESS_KEY, null) != null) {
			return new StandaloneClusterClient(config);
		// We tried hard but couldn't find a JobManager address
		} else {
			throw new IllegalConfigurationException(
				"The JobManager address is neither provided at the command-line, " +
					"nor configured in flink-conf.yaml.");
		}
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
	private int handleArgException(Exception e) {
		LOG.error("Invalid command line arguments." + (e.getMessage() == null ? "" : e.getMessage()));

		System.out.println(e.getMessage());
		System.out.println();
		System.out.println("Use the help option (-h or --help) to get help on the command.");
		return 1;
	}

	/**
	 * Displays an exception message.
	 * 
	 * @param t The exception to display.
	 * @return The return code for the process.
	 */
	private int handleError(Throwable t) {
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

	private void logAndSysout(String message) {
		LOG.info(message);
		System.out.println(message);
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
			CliFrontendParser.printHelp();
			System.out.println("Please specify an action.");
			return 1;
		}

		// get action
		String action = args[0];

		// remove action from parameters
		final String[] params = Arrays.copyOfRange(args, 1, args.length);

		// do action
		switch (action) {
			case ACTION_RUN:
				// run() needs to run in a secured environment for the optimizer.
				if (SecurityUtils.isSecurityEnabled()) {
					String message = "Secure Hadoop environment setup detected. Running in secure context.";
					LOG.info(message);

					try {
						return SecurityUtils.runSecured(new SecurityUtils.FlinkSecuredRunner<Integer>() {
							@Override
							public Integer run() throws Exception {
								return CliFrontend.this.run(params);
							}
						});
					}
					catch (Exception e) {
						return handleError(e);
					}
				} else {
					return run(params);
				}
			case ACTION_LIST:
				return list(params);
			case ACTION_INFO:
				return info(params);
			case ACTION_CANCEL:
				return cancel(params);
			case ACTION_STOP:
				return stop(params);
			case ACTION_SAVEPOINT:
				return savepoint(params);
			case "-h":
			case "--help":
				CliFrontendParser.printHelp();
				return 0;
			case "-v":
			case "--version":
				String version = EnvironmentInformation.getVersion();
				String commitID = EnvironmentInformation.getRevisionInformation().commitId;
				System.out.print("Version: " + version);
				System.out.println(!commitID.equals(EnvironmentInformation.UNKNOWN) ? ", Commit ID: " + commitID : "");
				return 0;
			default:
				System.out.printf("\"%s\" is not a valid action.\n", action);
				System.out.println();
				System.out.println("Valid actions are \"run\", \"list\", \"info\", \"stop\", or \"cancel\".");
				System.out.println();
				System.out.println("Specify the version option (-v or --version) to print Flink version.");
				System.out.println();
				System.out.println("Specify the help option (-h or --help) to get help on the command.");
				return 1;
		}
	}

	public void shutdown() {
		ActorSystem sys = this.actorSystem;
		if (sys != null) {
			this.actorSystem = null;
			sys.shutdown();
		}
	}

	/**
	 * Submits the job based on the arguments
	 */
	public static void main(String[] args) {
		EnvironmentInformation.logEnvironmentInfo(LOG, "Command Line Client", args);

		try {
			CliFrontend cli = new CliFrontend();
			int retCode = cli.parseParameters(args);
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
		String location = System.getenv(ENV_CONFIG_DIRECTORY);

		if (location != null) {
			if (new File(location).exists()) {
				return location;
			}
			else {
				throw new RuntimeException("The config directory '" + location + "', specified in the '" +
						ENV_CONFIG_DIRECTORY + "' environment variable, does not exist.");
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
					ENV_CONFIG_DIRECTORY + "' environment variable.");
		}
		return location;
	}


	/**
	 * Writes the given job manager address to the associated configuration object
	 *
	 * @param address Address to write to the configuration
	 * @param config The config to write to
	 */
	public static void writeJobManagerAddressToConfig(Configuration config, InetSocketAddress address) {
		config.setString(ConfigConstants.JOB_MANAGER_IPC_ADDRESS_KEY, address.getHostName());
		config.setInteger(ConfigConstants.JOB_MANAGER_IPC_PORT_KEY, address.getPort());
	}

}
