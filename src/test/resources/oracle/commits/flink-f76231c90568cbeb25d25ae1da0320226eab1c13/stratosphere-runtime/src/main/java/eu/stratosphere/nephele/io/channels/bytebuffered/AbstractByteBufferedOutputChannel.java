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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.stratosphere.core.io.IOReadableWritable;
import eu.stratosphere.nephele.event.task.AbstractEvent;
import eu.stratosphere.nephele.event.task.AbstractTaskEvent;
import eu.stratosphere.nephele.io.OutputGate;
import eu.stratosphere.nephele.io.channels.AbstractOutputChannel;
import eu.stratosphere.nephele.io.channels.Buffer;
import eu.stratosphere.nephele.io.channels.ChannelID;
import eu.stratosphere.nephele.io.channels.ChannelType;
import eu.stratosphere.nephele.io.channels.SerializationBuffer;

public abstract class AbstractByteBufferedOutputChannel<T extends IOReadableWritable> extends AbstractOutputChannel<T> {

	/**
	 * The serialization buffer used to serialize records.
	 */
	private final SerializationBuffer<T> serializationBuffer = new SerializationBuffer<T>();

	/**
	 * Buffer for the serialized output data.
	 */
	private Buffer dataBuffer = null;

	/**
	 * Stores whether the channel is requested to be closed.
	 */
	private boolean closeRequested = false;

	/**
	 * The output channel broker the channel should contact to request and release write buffers.
	 */
	private ByteBufferedOutputChannelBroker outputChannelBroker = null;


	/**
	 * Stores the number of bytes transmitted through this output channel since its instantiation.
	 */
	private long amountOfDataTransmitted = 0L;

	private static final Log LOG = LogFactory.getLog(AbstractByteBufferedOutputChannel.class);

	/**
	 * Creates a new byte buffered output channel.
	 * 
	 * @param outputGate
	 *        the output gate this channel is wired to
	 * @param channelIndex
	 *        the channel's index at the associated output gate
	 * @param channelID
	 *        the ID of the channel
	 * @param connectedChannelID
	 *        the ID of the channel this channel is connected to
	 */
	protected AbstractByteBufferedOutputChannel(final OutputGate<T> outputGate, final int channelIndex,
			final ChannelID channelID, final ChannelID connectedChannelID) {
		super(outputGate, channelIndex, channelID, connectedChannelID);
	}


	@Override
	public boolean isClosed() throws IOException, InterruptedException {

		if (this.closeRequested && this.dataBuffer == null
			&& !this.serializationBuffer.dataLeftFromPreviousSerialization()) {

			if (!this.outputChannelBroker.hasDataLeftToTransmit()) {
				return true;
			}
		}

		return false;
	}


	@Override
	public void requestClose() throws IOException, InterruptedException {

		if (!this.closeRequested) {
			this.closeRequested = true;
			if (this.serializationBuffer.dataLeftFromPreviousSerialization()) {
				// make sure we serialized all data before we send the close event
				flush();
			}

			if (getType() == ChannelType.INMEMORY || !isBroadcastChannel() || getChannelIndex() == 0) {
				transferEvent(new ByteBufferedChannelCloseEvent());
				flush();
			}
		}
	}

	/**
	 * Requests a new write buffer from the framework. This method blocks until the requested buffer is available.
	 * 
	 * @throws InterruptedException
	 *         thrown if the thread is interrupted while waiting for the buffer
	 * @throws IOException
	 *         thrown if an I/O error occurs while waiting for the buffer
	 */
	private void requestWriteBufferFromBroker() throws InterruptedException, IOException {
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
		this.dataBuffer = this.outputChannelBroker.requestEmptyWriteBuffer();
	}

	/**
	 * Returns the filled buffer to the framework and triggers further processing.
	 * 
	 * @throws IOException
	 *         thrown if an I/O error occurs while releasing the buffers
	 * @throws InterruptedException
	 *         thrown if the thread is interrupted while releasing the buffers
	 */
	private void releaseWriteBuffer() throws IOException, InterruptedException {
		// Keep track of number of bytes transmitted through this channel
		this.amountOfDataTransmitted += this.dataBuffer.size();

		this.outputChannelBroker.releaseWriteBuffer(this.dataBuffer);
		this.dataBuffer = null;
	}


	@Override
	public void writeRecord(T record) throws IOException, InterruptedException {

		// Get a write buffer from the broker
		if (this.dataBuffer == null) {
			requestWriteBufferFromBroker();
		}

		if (this.closeRequested) {
			throw new IOException("Channel is aready requested to be closed");
		}

		// Check if we can accept new records or if there are still old
		// records to be transmitted
		while (this.serializationBuffer.dataLeftFromPreviousSerialization()) {

			this.serializationBuffer.read(this.dataBuffer);
			if (this.dataBuffer.remaining() == 0) {
				releaseWriteBuffer();
				requestWriteBufferFromBroker();
			}
		}

		if (this.serializationBuffer.dataLeftFromPreviousSerialization()) {
			throw new IOException("Serialization buffer is expected to be empty!");
		}

		this.serializationBuffer.serialize(record);

		this.serializationBuffer.read(this.dataBuffer);
		if (this.dataBuffer.remaining() == 0) {
			releaseWriteBuffer();
		}
	}

	/**
	 * Sets the output channel broker this channel should contact to request and release write buffers.
	 * 
	 * @param byteBufferedOutputChannelBroker
	 *        the output channel broker the channel should contact to request and release write buffers
	 */
	public void setByteBufferedOutputChannelBroker(ByteBufferedOutputChannelBroker byteBufferedOutputChannelBroker) {

		this.outputChannelBroker = byteBufferedOutputChannelBroker;
	}


	public void processEvent(AbstractEvent event) {

		if (event instanceof AbstractTaskEvent) {
			getOutputGate().deliverEvent((AbstractTaskEvent) event);
		} else {
			LOG.error("Channel " + getID() + " received unknown event " + event);
		}
	}


	@Override
	public void transferEvent(AbstractEvent event) throws IOException, InterruptedException {

		flush();
		this.outputChannelBroker.transferEventToInputChannel(event);
	}


	@Override
	public void flush() throws IOException, InterruptedException {

		// Get rid of remaining data in the serialization buffer
		while (this.serializationBuffer.dataLeftFromPreviousSerialization()) {

			if (this.dataBuffer == null) {

				try {
					requestWriteBufferFromBroker();
				} catch (InterruptedException e) {
					LOG.error(e);
				}
			}
			this.serializationBuffer.read(this.dataBuffer);
			if (this.dataBuffer.remaining() == 0) {
				releaseWriteBuffer();
			}
		}

		// Get rid of the leased write buffer
		if (this.dataBuffer != null) {
			releaseWriteBuffer();
		}
	}


	@Override
	public void releaseAllResources() {

		// TODO: Reconsider release of broker's resources here
		this.closeRequested = true;

		this.serializationBuffer.clear();

		if (this.dataBuffer != null) {
			this.dataBuffer.recycleBuffer();
			this.dataBuffer = null;
		}
	}


	@Override
	public long getAmountOfDataTransmitted() {

		return this.amountOfDataTransmitted;
	}
}
