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

package eu.stratosphere.configuration;

/**
 * This class contains all constants for the configuration. That includes the configuration keys and
 * the default values.
 */
public final class ConfigConstants {

	// ------------------------------------------------------------------------
	// Configuration Keys
	// ------------------------------------------------------------------------

	// -------------------------- Addresses and Ports -------------------------

	/**
	 * The key for the config parameter defining the network address to connect to
	 * for communication with the job manager.
	 */
	public static final String JOB_MANAGER_IPC_ADDRESS_KEY = "jobmanager.rpc.address";

	/**
	 * The key for the config parameter defining the network port to connect to
	 * for communication with the job manager.
	 */
	public static final String JOB_MANAGER_IPC_PORT_KEY = "jobmanager.rpc.port";

	/**
	 * The key for the parameter defining the task manager's IPC port from the configuration.
	 */
	public static final String TASK_MANAGER_IPC_PORT_KEY = "taskmanager.rpc.port";

	/**
	 * The key for the config parameter defining the task manager's data port from the configuration.
	 */
	public static final String TASK_MANAGER_DATA_PORT_KEY = "taskmanager.data.port";

	/**
	 * The key for the config parameter defining the directories for temporary files.
	 */
	public static final String TASK_MANAGER_TMP_DIR_KEY = "taskmanager.tmp.dirs";
	
	/**
	 * The key for the config parameter defining the default number of retries for failed tasks.
	 */
	public static final String JOB_EXECUTION_RETRIES_KEY = "job.execution.retries";

	/**
	 * The key for the config parameter defining the amount of memory available for the task manager's
	 * memory manager (in megabytes).
	 */
	public static final String MEMORY_MANAGER_AVAILABLE_MEMORY_SIZE_KEY = "taskmanager.memory.size";
	
	/**
	 * The key for the config parameter defining the fraction of free memory allocated by the memory manager.
	 */
	public static final String MEMORY_MANAGER_AVAILABLE_MEMORY_FRACTION_KEY = "taskmanager.memory.fraction";

	/**
	 * The key defining the amount polling interval (in seconds) for the JobClient.
	 */
	public static final String JOBCLIENT_POLLING_INTERVAL_KEY = "jobclient.polling.interval";
	
	/**
	 * The key for the config parameter defining flag to terminate a job at job-client shutdown.
	 */
	public static final String JOBCLIENT_SHUTDOWN_TERMINATEJOB_KEY = "jobclient.shutdown.terminatejob";
	
	
	// ------------------------ Web Frontend JobManager------------------------

	/**
	 * The key for Stratosphere's base dir path
	 */
	public static final String STRATOSPHERE_BASE_DIR_PATH_KEY = "stratosphere.base.dir.path";
	
	/**
	 * The key for the config parameter defining port for the pact web-frontend server.
	 */
	public static final String JOB_MANAGER_WEB_PORT_KEY = "jobmanager.web.port";

	/**
	 * The key for the config parameter defining the directory containing the web documents.
	 */
	public static final String JOB_MANAGER_WEB_ROOT_PATH_KEY = "jobmanager.web.rootpath";

	/**
	 * The key for the config parameter defining the port to the htaccess file protecting the web server.
	 */
	public static final String JOB_MANAGER_WEB_ACCESS_FILE_KEY = "jobmanager.web.access";
	
	/**
	 * The key for the config parameter defining the number of archived jobs for the jobmanager
	 */
	public static final String JOB_MANAGER_WEB_ARCHIVE_COUNT = "jobmanager.web.archive";

	// ------------------------------------------------------------------------
	// Default Values
	// ------------------------------------------------------------------------

	/**
	 * The default network port to connect to for communication with the job manager.
	 */
	public static final int DEFAULT_JOB_MANAGER_IPC_PORT = 6123;

	/**
	 * The default network port the task manager expects incoming IPC connections.
	 */
	public static final int DEFAULT_TASK_MANAGER_IPC_PORT = 6122;

	/**
	 * The default network port the task manager expects to receive transfer envelopes on.
	 */
	public static final int DEFAULT_TASK_MANAGER_DATA_PORT = 6121;

	/**
	 * The default fraction of the free memory allocated by the task manager's memory manager.
	 */
	public static final float DEFAULT_MEMORY_MANAGER_MEMORY_FRACTION = 0.7f;
	
	/**
	 * The default number of retries for failed tasks.
	 */
	public static final int DEFAULT_JOB_EXECUTION_RETRIES = 0;

	/**
	 * The default minimal amount of memory that the memory manager does not occupy (in megabytes).
	 */
	public static final long DEFAULT_MEMORY_MANAGER_MIN_UNRESERVED_MEMORY = 256 * 1024 * 1024;

	/**
	 * The default directory for temporary files of the task manager.
	 */
	public static final String DEFAULT_TASK_MANAGER_TMP_PATH = System.getProperty("java.io.tmpdir");

	/**
	 * The default value for the JobClient's polling interval. 2 Seconds.
	 */
	public static final int DEFAULT_JOBCLIENT_POLLING_INTERVAL = 2;
	
	/**
	 * The default value for the flag to terminate a job on job-client shutdown.
	 */
	public static final boolean DEFAULT_JOBCLIENT_SHUTDOWN_TERMINATEJOB = true;
	
	/**
	 */
	public static final int DEFAULT_WEB_FRONTEND_PORT = 8081;

	/**
	 * The default directory name of the info server
	 */
	public static final String DEFAULT_JOB_MANAGER_WEB_PATH_NAME = "web-docs-infoserver";
	
	/**
	 * The default path of the directory for info server containing the web documents.
	 */
	public static final String DEFAULT_JOB_MANAGER_WEB_ROOT_PATH = "./resources/"+DEFAULT_JOB_MANAGER_WEB_PATH_NAME+"/";
	
	/**
	 * The default number of archived jobs for the jobmanager
	 */
	public static final int DEFAULT_JOB_MANAGER_WEB_ARCHIVE_COUNT = 20;

	// ----------------------------- Instances --------------------------------

	/**
	 * The default definition for an instance type, if no other configuration is provided.
	 */
	public static final String DEFAULT_INSTANCE_TYPE = "default,2,1,1024,10,10";

	/**
	 * The default index for the default instance type.
	 */
	public static final int DEFAULT_DEFAULT_INSTANCE_TYPE_INDEX = 1;


	// ------------------------------------------------------------------------

	/**
	 * Private default constructor to prevent instantiation.
	 */
	private ConfigConstants() {
	}
}
