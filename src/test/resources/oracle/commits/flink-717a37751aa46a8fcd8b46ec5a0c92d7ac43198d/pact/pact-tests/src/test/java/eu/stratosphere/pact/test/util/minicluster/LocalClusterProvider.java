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

package eu.stratosphere.pact.test.util.minicluster;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.jobgraph.JobGraph;
import eu.stratosphere.nephele.template.IllegalConfigurationException;
import eu.stratosphere.pact.test.util.Constants;
import eu.stratosphere.pact.test.util.filesystem.HDFSProvider;
import eu.stratosphere.pact.test.util.filesystem.LocalFSProvider;
import eu.stratosphere.pact.test.util.filesystem.MiniDFSProvider;

public class LocalClusterProvider extends ClusterProvider {

	// config parameters
	private int numTaskTrackers;

	private NepheleMiniCluster nephele;

	public LocalClusterProvider(Configuration config)
														throws Exception {
		super(config);

		this.numTaskTrackers = Integer.parseInt(config.getString(
			Constants.CLUSTER_NUM_TASKTRACKER, "-1"));
		if (numTaskTrackers == -1) {
			throw new Exception("Number of task trackers was not specified");
		}
	}

	@Override
	protected void startFS() throws Exception {
		if (fsIsRunning()) {
			return;
		}

		if(config.getString(Constants.FILESYSTEM_TYPE, "").equals("local_fs")) {
			filesystemProvider = new LocalFSProvider();
		} else if(config.getString(Constants.FILESYSTEM_TYPE, "").equals("mini_hdfs")) {
			filesystemProvider = new MiniDFSProvider();
		} else {
			throw new IllegalConfigurationException("Invalid file system type: "+config.getString(Constants.FILESYSTEM_TYPE, ""));
		}
		
		filesystemProvider.start();
		filesystemRunning = true;
	}

	@Override
	protected void startNephele() throws Exception {
		if (nepheleIsRunning()) {
			return;
		}

		String nepheleConfigDir = System.getProperty("user.dir") + "/tmp/nephele/config";
		if (filesystemProvider == null) {
			startFS();
		}
		String hdfsConfigDir = "";
		if(this.config.getString(Constants.FILESYSTEM_TYPE, "").equals("mini_hdfs")) {
			hdfsConfigDir = ((HDFSProvider)filesystemProvider).getConfigDir();
		}
		nephele = new NepheleMiniCluster(nepheleConfigDir, hdfsConfigDir, numTaskTrackers);
		nepheleRunning = true;
	}

	@Override
	protected void stopFS() throws Exception {
		if (!fsIsRunning()) {
			return;
		}

		filesystemProvider.stop();
		filesystemRunning = false;
	}

	@Override
	protected void stopNephele() throws Exception {
		if (!nepheleIsRunning()) {
			return;
		}

		nephele.stop();
		nepheleRunning = false;
	}

	@Override
	public void submitJobAndWait(JobGraph jobGraph, String jarFilePath) throws Exception {
		nephele.submitJobAndWait(jobGraph);
	}

	/*
	 * (non-Javadoc)
	 * @see eu.stratosphere.pact.test.util.minicluster.ClusterProvider#clearHDFS()
	 */
	@Override
	protected void clearFS() throws Exception {
		filesystemProvider.delete("/", true);
	}

}
