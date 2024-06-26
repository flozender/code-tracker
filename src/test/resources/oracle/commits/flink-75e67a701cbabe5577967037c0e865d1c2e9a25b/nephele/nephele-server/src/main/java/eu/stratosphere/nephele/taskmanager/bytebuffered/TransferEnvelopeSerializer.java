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

package eu.stratosphere.nephele.taskmanager.bytebuffered;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import eu.stratosphere.nephele.event.task.EventList;
import eu.stratosphere.nephele.io.IOReadableWritable;
import eu.stratosphere.nephele.io.channels.Buffer;
import eu.stratosphere.nephele.io.channels.ChannelID;
import eu.stratosphere.nephele.io.channels.SerializationBuffer;

public class TransferEnvelopeSerializer {

	private enum SerializationState {
		NOTSERIALIZED,
		SEQUENCENUMBERSERIALIZED,
		SOURCESERIALIZED,
		TARGETSERIALIZED,
		NOTIFICATIONSSERIALIZED,
		FULLYSERIALIZED
	};

	private final static int SIZEOFINT = 4;

	private TransferEnvelope transferEnvelope = null;

	private SerializationState serializationState;

	private final SerializationBuffer<IOReadableWritable> serializationBuffer = new SerializationBuffer<IOReadableWritable>();

	private final ByteBuffer tempBuffer = ByteBuffer.allocate(64); // TODO: Make this configurable

	private boolean serializationStarted = false;

	private boolean bufferExistanceSerialized = false;

	public void setTransferEnvelope(TransferEnvelope transferEnvelope) {

		this.transferEnvelope = transferEnvelope;
		reset();
	}

	public boolean write(WritableByteChannel writableByteChannel) throws IOException {

		while (true) {

			boolean moreDataFollows = false;

			// System.out.println("OUTGOING State: " + this.serializationState);

			switch (serializationState) {
			case NOTSERIALIZED:
				moreDataFollows = writeSequenceNumber(writableByteChannel, this.transferEnvelope.getSequenceNumber());
				break;
			case SEQUENCENUMBERSERIALIZED:
				moreDataFollows = writeChannelID(writableByteChannel, this.transferEnvelope.getSource());
				break;
			case SOURCESERIALIZED:
				moreDataFollows = writeChannelID(writableByteChannel, this.transferEnvelope.getTarget());
				break;
			case TARGETSERIALIZED:
				moreDataFollows = writeNotification(writableByteChannel, this.transferEnvelope.getEventList());
				break;
			case NOTIFICATIONSSERIALIZED:
				moreDataFollows = writeBuffer(writableByteChannel, this.transferEnvelope.getBuffer());
				break;
			case FULLYSERIALIZED:
				return false;
			}

			if (moreDataFollows) {
				return true;
			}
		}
	}

	private boolean writeSequenceNumber(WritableByteChannel writableByteChannel, int sequenceNumber) throws IOException {

		if (sequenceNumber < 0) {
			throw new IOException("Invalid sequence number: " + sequenceNumber);
		}

		if (!this.serializationStarted) {
			this.tempBuffer.clear();
			integerToByteBuffer(sequenceNumber, 0, this.tempBuffer);
			this.serializationStarted = true;
		}

		if (writableByteChannel.write(this.tempBuffer) == -1) {
			throw new IOException("Unexpected end of stream while serializing the sequence number");
		}

		if (!this.tempBuffer.hasRemaining()) {
			this.serializationState = SerializationState.SEQUENCENUMBERSERIALIZED;
			this.serializationStarted = false;
			return false;
		}

		return true;
	}

	private boolean writeChannelID(WritableByteChannel writableByteChannel, ChannelID channelID) throws IOException {

		if (!writeIOReadableWritable(writableByteChannel, channelID)) {
			// We're done, all the data has been written to the channel
			if (this.serializationState == SerializationState.SEQUENCENUMBERSERIALIZED) {
				// System.out.println("OUTGOING Serialized source: " + channelID);
				this.serializationState = SerializationState.SOURCESERIALIZED;
			} else {
				// System.out.println("OUTGOING Serialized target: " + channelID);
				this.serializationState = SerializationState.TARGETSERIALIZED;
			}
			return false;
		}

		return true;
	}

	private boolean writeIOReadableWritable(WritableByteChannel writableByteChannel,
			IOReadableWritable ioReadableWritable) throws IOException {

		if (!this.serializationStarted) {
			this.serializationBuffer.clear();
			this.serializationBuffer.serialize(ioReadableWritable);
			this.serializationStarted = true;
		}

		if (this.serializationBuffer.dataLeftFromPreviousSerialization()) {
			this.serializationBuffer.read(writableByteChannel);
		} else {
			this.serializationStarted = false;
			return false;
		}

		return true;
	}

	private boolean writeNotification(WritableByteChannel writableByteChannel, EventList notificationList)
			throws IOException {

		if (!writeIOReadableWritable(writableByteChannel, notificationList)) {
			// We're done, all the data has been written to the channel
			this.serializationState = SerializationState.NOTIFICATIONSSERIALIZED;
			return false;
		}

		return true;

	}

	public void reset() {
		this.serializationState = SerializationState.NOTSERIALIZED;
		this.serializationStarted = false;
		this.bufferExistanceSerialized = false;
	}

	private boolean writeBuffer(WritableByteChannel writableByteChannel, Buffer buffer) throws IOException {

		while (true) {

			if (!this.bufferExistanceSerialized) {

				if (!this.serializationStarted) {
					this.tempBuffer.position(0);

					if (buffer == null) {
						this.tempBuffer.put(0, (byte) 0);
						this.tempBuffer.limit(1);
					} else {
						this.tempBuffer.put(0, (byte) 1);
						// System.out.println("OUTGOING: Buffer size is " + buffer.size());
						integerToByteBuffer(buffer.size(), 1, this.tempBuffer);
					}
					this.serializationStarted = true;
				}

				if (this.tempBuffer.hasRemaining()) {
					if (writableByteChannel.write(tempBuffer) == 0) {
						return true;
					}
				} else {
					this.bufferExistanceSerialized = true;
					this.serializationStarted = false;
					if (buffer == null) {
						// That's it, we're done. No buffer will follow
						this.serializationState = SerializationState.FULLYSERIALIZED;
						return false;
					}
				}

			} else {

				buffer.read(writableByteChannel);

				if (!buffer.hasRemaining()) {
					this.serializationState = SerializationState.FULLYSERIALIZED;
					return false;
				}

				return true;
			}
		}
	}

	private void integerToByteBuffer(int integerToSerialize, int offset, ByteBuffer byteBuffer) throws IOException {

		if ((offset + SIZEOFINT) > byteBuffer.capacity()) {
			throw new IOException("Cannot convert integer to byte buffer, buffer is too small (" + byteBuffer.limit()
				+ ", required " + (offset + SIZEOFINT) + ")");
		}

		byteBuffer.limit(offset + SIZEOFINT);

		for (int i = 0; i < SIZEOFINT; ++i) {
			final int shift = i << 3; // i * 8
			byteBuffer.put((offset + SIZEOFINT - 1) - i, (byte) ((integerToSerialize & (0xff << shift)) >>> shift));
		}
	}
}
