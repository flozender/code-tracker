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

package eu.stratosphere.nephele.jobgraph;

/**
 * Defines the possible status of a job once it has been
 * accepted by the job manager.
 * This class is thread-safe.
 * 
 * @author warneke
 */
public enum JobStatus {

	/**
	 * All tasks of the job are in the execution state CREATED.
	 */
	CREATED,

	/**
	 * All tasks of the job are in the execution state
	 */
	SCHEDULED,

	/**
	 * At least one task of the job is running, none has definitely failed.
	 */
	RUNNING,

	/**
	 * At least one task of the job has definitively failed and cannot
	 * be recovered anymore.
	 */
	FAILED,

	/**
	 * All tasks of the job are either canceled or finished
	 */
	CANCELLED,

	/**
	 * All of the job's tasks have successfully finished.
	 */
	FINISHED
};