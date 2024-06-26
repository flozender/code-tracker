/**
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

package org.apache.flink.runtime.jobgraph;

import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.common.operators.util.UserCodeWrapper;
import org.apache.flink.runtime.operators.util.TaskConfig;

/**
 * A JobOutputVertex is a specific sub-type of a {@link AbstractJobOutputVertex} and is designed
 * for Nephele tasks which sink data in a not further specified way. As every job output vertex,
 * a JobOutputVertex must not have any further output.
 */
public class OutputFormatOutputVertex extends AbstractJobOutputVertex {
	/**
	 * Contains the output format associated to this output vertex. It can be <pre>null</pre>.
	 */
	private OutputFormat<?> outputFormat;


	/**
	 * Creates a new job file output vertex with the specified name.
	 * 
	 * @param name
	 *        the name of the new job file output vertex
	 * @param jobGraph
	 *        the job graph this vertex belongs to
	 */
	public OutputFormatOutputVertex(String name, JobGraph jobGraph) {
		this(name, null, jobGraph);
	}
	
	public OutputFormatOutputVertex(String name, JobVertexID id, JobGraph jobGraph) {
		super(name, id, jobGraph);
	}

	/**
	 * Creates a new job file input vertex.
	 * 
	 * @param jobGraph
	 *        the job graph this vertex belongs to
	 */
	public OutputFormatOutputVertex(JobGraph jobGraph) {
		this(null, jobGraph);
	}
	
	public void setOutputFormat(OutputFormat<?> format) {
		this.outputFormat = format;
	}
	
	public void initializeOutputFormatFromTaskConfig(ClassLoader cl) {
		TaskConfig cfg = new TaskConfig(getConfiguration());
		UserCodeWrapper<OutputFormat<?>> wrapper = cfg.<OutputFormat<?>>getStubWrapper(cl);
		
		if (wrapper != null) {
			this.outputFormat = wrapper.getUserCodeObject(OutputFormat.class, cl);
			this.outputFormat.configure(cfg.getStubParameters());
		}
	}

	/**
	 * Returns the output format. It can also be <pre>null</pre>.
	 *
	 * @return output format or <pre>null</pre>
	 */
	public OutputFormat<?> getOutputFormat() { return outputFormat; }
}
