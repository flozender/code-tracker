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

package eu.stratosphere.nephele.io.channels.bytebuffered;

import eu.stratosphere.core.io.IOReadableWritable;
import eu.stratosphere.nephele.io.OutputGate;
import eu.stratosphere.nephele.io.channels.ChannelID;
import eu.stratosphere.nephele.io.channels.ChannelType;

public final class NetworkOutputChannel<T extends IOReadableWritable> extends AbstractByteBufferedOutputChannel<T> {

	public NetworkOutputChannel(OutputGate<T> outputGate, int channelIndex, ChannelID channelID,
			ChannelID connectedChannelID) {
		super(outputGate, channelIndex, channelID, connectedChannelID);
	}

	@Override
	public ChannelType getType() {

		return ChannelType.NETWORK;
	}

}
