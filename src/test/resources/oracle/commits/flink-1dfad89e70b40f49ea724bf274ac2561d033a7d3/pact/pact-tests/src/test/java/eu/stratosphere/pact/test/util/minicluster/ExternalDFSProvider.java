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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class ExternalDFSProvider extends HDFSProvider {
	public ExternalDFSProvider(String configDir) {
		super(configDir);
	}

	@Override
	public void start() throws Exception {
		Configuration config = new Configuration(false);
		config.addResource(new Path(configDir + "/hadoop-default.xml"));
		config.addResource(new Path(configDir + "/hadoop-site.xml"));

		hdfs = FileSystem.get(config);
	}

	@Override
	public void stop() {
		hdfs = null;
	}
}
