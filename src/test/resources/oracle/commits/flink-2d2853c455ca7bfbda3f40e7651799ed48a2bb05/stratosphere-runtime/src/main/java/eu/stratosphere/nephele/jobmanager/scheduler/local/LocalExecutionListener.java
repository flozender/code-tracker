/***********************************************************************************************************************
 * Copyright (C) 2010-2013 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 **********************************************************************************************************************/

package eu.stratosphere.nephele.jobmanager.scheduler.local;

import eu.stratosphere.nephele.executiongraph.ExecutionVertex;
import eu.stratosphere.nephele.jobmanager.scheduler.AbstractExecutionListener;

/**
 * This is a wrapper class for the {@link LocalScheduler} to receive
 * notifications about state changes of vertices belonging
 * to scheduled jobs.
 * <p>
 * This class is thread-safe.
 * 
 * @author warneke
 */
public class LocalExecutionListener extends AbstractExecutionListener {

	public LocalExecutionListener(final LocalScheduler scheduler, final ExecutionVertex executionVertex) {
		super(scheduler, executionVertex);
	}

}
