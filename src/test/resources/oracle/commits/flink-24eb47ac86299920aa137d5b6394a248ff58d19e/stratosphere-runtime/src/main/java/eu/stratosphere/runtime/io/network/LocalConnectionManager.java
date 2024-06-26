/***********************************************************************************************************************
 * Copyright (C) 2010-2014 by the Apache Flink project (http://flink.incubator.apache.org)
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

package eu.stratosphere.runtime.io.network;

import java.io.IOException;

public class LocalConnectionManager implements NetworkConnectionManager {

	@Override
	public void start(ChannelManager channelManager) throws IOException {
	}

	@Override
	public void enqueue(Envelope envelope, RemoteReceiver receiver) throws IOException {
	}

	@Override
	public void shutdown() throws IOException {
	}
}
