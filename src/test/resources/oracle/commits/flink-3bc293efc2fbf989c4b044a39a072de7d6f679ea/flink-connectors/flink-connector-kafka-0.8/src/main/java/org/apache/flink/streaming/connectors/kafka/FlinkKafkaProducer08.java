/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.connectors.kafka;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkFixedPartitioner;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaDelegatePartitioner;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.streaming.connectors.kafka.partitioner.KafkaPartitioner;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchemaWrapper;

import java.util.Properties;

/**
 * Flink Sink to produce data into a Kafka topic. This producer is compatible with Kafka 0.8.
 *
 * <p>Please note that this producer does not have any reliability guarantees.
 *
 * @param <IN> Type of the messages to write into Kafka.
 */
public class FlinkKafkaProducer08<IN> extends FlinkKafkaProducerBase<IN>  {

	private static final long serialVersionUID = 1L;

	// ------------------- Keyless serialization schema constructors ----------------------
	/**
	 * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to
	 * the topic.
	 *
	 * @param brokerList
	 *			Comma separated addresses of the brokers
	 * @param topicId
	 * 			ID of the Kafka topic.
	 * @param serializationSchema
	 * 			User defined (keyless) serialization schema.
	 */
	public FlinkKafkaProducer08(String brokerList, String topicId, SerializationSchema<IN> serializationSchema) {
		this(topicId, new KeyedSerializationSchemaWrapper<>(serializationSchema), getPropertiesFromBrokerList(brokerList), new FlinkFixedPartitioner<IN>());
	}

	/**
	 * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to
	 * the topic.
	 *
	 * @param topicId
	 * 			ID of the Kafka topic.
	 * @param serializationSchema
	 * 			User defined (keyless) serialization schema.
	 * @param producerConfig
	 * 			Properties with the producer configuration.
	 */
	public FlinkKafkaProducer08(String topicId, SerializationSchema<IN> serializationSchema, Properties producerConfig) {
		this(topicId, new KeyedSerializationSchemaWrapper<>(serializationSchema), producerConfig, new FlinkFixedPartitioner<IN>());
	}

	/**
	 * The main constructor for creating a FlinkKafkaProducer.
	 *
	 * @param topicId The topic to write data to
	 * @param serializationSchema A (keyless) serializable serialization schema for turning user objects into a kafka-consumable byte[]
	 * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is the only required argument.
	 * @param customPartitioner A serializable partitioner for assigning messages to Kafka partitions.
	 */
	public FlinkKafkaProducer08(String topicId, SerializationSchema<IN> serializationSchema, Properties producerConfig, FlinkKafkaPartitioner<IN> customPartitioner) {
		this(topicId, new KeyedSerializationSchemaWrapper<>(serializationSchema), producerConfig, customPartitioner);

	}

	// ------------------- Key/Value serialization schema constructors ----------------------

	/**
	 * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to
	 * the topic.
	 *
	 * @param brokerList
	 *			Comma separated addresses of the brokers
	 * @param topicId
	 * 			ID of the Kafka topic.
	 * @param serializationSchema
	 * 			User defined serialization schema supporting key/value messages
	 */
	public FlinkKafkaProducer08(String brokerList, String topicId, KeyedSerializationSchema<IN> serializationSchema) {
		this(topicId, serializationSchema, getPropertiesFromBrokerList(brokerList), new FlinkFixedPartitioner<IN>());
	}

	/**
	 * Creates a FlinkKafkaProducer for a given topic. The sink produces its input to
	 * the topic.
	 *
	 * @param topicId
	 * 			ID of the Kafka topic.
	 * @param serializationSchema
	 * 			User defined serialization schema supporting key/value messages
	 * @param producerConfig
	 * 			Properties with the producer configuration.
	 */
	public FlinkKafkaProducer08(String topicId, KeyedSerializationSchema<IN> serializationSchema, Properties producerConfig) {
		this(topicId, serializationSchema, producerConfig, new FlinkFixedPartitioner<IN>());
	}

	/**
	 * The main constructor for creating a FlinkKafkaProducer.
	 *
	 * @param topicId The topic to write data to
	 * @param serializationSchema A serializable serialization schema for turning user objects into a kafka-consumable byte[] supporting key/value messages
	 * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is the only required argument.
	 * @param customPartitioner A serializable partitioner for assigning messages to Kafka partitions.
	 */
	public FlinkKafkaProducer08(String topicId, KeyedSerializationSchema<IN> serializationSchema, Properties producerConfig, FlinkKafkaPartitioner<IN> customPartitioner) {
		super(topicId, serializationSchema, producerConfig, customPartitioner);
	}

	// ------------------- Deprecated constructors ----------------------

	/**
	 * The main constructor for creating a FlinkKafkaProducer.
	 *
	 * @param topicId The topic to write data to
	 * @param serializationSchema A (keyless) serializable serialization schema for turning user objects into a kafka-consumable byte[]
	 * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is the only required argument.
	 * @param customPartitioner A serializable partitioner for assigning messages to Kafka partitions.
	 *
	 * @deprecated This is a deprecated constructor that does not correctly handle partitioning when
	 *             producing to multiple topics. Use
	 *             {@link #FlinkKafkaProducer08(String, SerializationSchema, Properties, FlinkKafkaPartitioner)} instead.
	 */
	@Deprecated
	public FlinkKafkaProducer08(String topicId, SerializationSchema<IN> serializationSchema, Properties producerConfig, KafkaPartitioner<IN> customPartitioner) {
		this(topicId, new KeyedSerializationSchemaWrapper<>(serializationSchema), producerConfig, customPartitioner);
	}

	/**
	 * The main constructor for creating a FlinkKafkaProducer.
	 *
	 * @param topicId The topic to write data to
	 * @param serializationSchema A serializable serialization schema for turning user objects into a kafka-consumable byte[] supporting key/value messages
	 * @param producerConfig Configuration properties for the KafkaProducer. 'bootstrap.servers.' is the only required argument.
	 * @param customPartitioner A serializable partitioner for assigning messages to Kafka partitions.
	 *
	 * @deprecated This is a deprecated constructor that does not correctly handle partitioning when
	 *             producing to multiple topics. Use
	 *             {@link #FlinkKafkaProducer08(String, KeyedSerializationSchema, Properties, FlinkKafkaPartitioner)} instead.
	 */
	@Deprecated
	public FlinkKafkaProducer08(String topicId, KeyedSerializationSchema<IN> serializationSchema, Properties producerConfig, KafkaPartitioner<IN> customPartitioner) {
		super(topicId, serializationSchema, producerConfig, new FlinkKafkaDelegatePartitioner<>(customPartitioner));
	}

	// ---------------------------------------------------------------------

	@Override
	protected void flush() {
		// The Kafka 0.8 producer doesn't support flushing, we wait here
		// until all pending records are confirmed
		synchronized (pendingRecordsLock) {
			while (pendingRecords > 0) {
				try {
					pendingRecordsLock.wait();
				} catch (InterruptedException e) {
					// this can be interrupted when the Task has been cancelled.
					// by throwing an exception, we ensure that this checkpoint doesn't get confirmed
					throw new RuntimeException("Flushing got interrupted while checkpointing", e);
				}
			}
		}
	}

}
