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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.io.IOReadableWritable;
import eu.stratosphere.nephele.io.channels.ChannelType;
import eu.stratosphere.nephele.io.compression.CompressionLevel;
import eu.stratosphere.nephele.template.AbstractInvokable;
import eu.stratosphere.nephele.template.IllegalConfigurationException;
import eu.stratosphere.nephele.types.StringRecord;
import eu.stratosphere.nephele.util.EnumUtils;
import eu.stratosphere.nephele.util.StringUtils;

/**
 * An abstract base class for a job vertex in Nephele.
 * 
 * @author warneke
 */
public abstract class AbstractJobVertex implements IOReadableWritable {

	/**
	 * List of outgoing edges.
	 */
	private final ArrayList<JobEdge> forwardEdges = new ArrayList<JobEdge>();

	/**
	 * List of incoming edges.
	 */
	private final ArrayList<JobEdge> backwardEdges = new ArrayList<JobEdge>();

	/**
	 * The name of the vertex or task, respectively.
	 */
	private final String name;

	/**
	 * The ID of the vertex.
	 */
	private final JobVertexID id;

	/**
	 * The graph this vertex belongs to.
	 */
	private final JobGraph jobGraph;

	/**
	 * Number of subtasks to split this task into at runtime.
	 */
	private int numberOfSubtasks = -1;

	/**
	 * The type of instance to be assigned to this task at runtime.
	 */
	private String instanceType = null;

	/**
	 * Number of subtasks to share the same instance at runtime.
	 */
	private int numberOfSubtasksPerInstance = -1;

	/**
	 * Other task to share a (set of) of instances with at runtime.
	 */
	private AbstractJobVertex vertexToShareInstancesWith = null;

	/**
	 * Custom configuration passed to the assigned task at runtime.
	 */
	private final Configuration configuration = new Configuration();

	/**
	 * Constructs a new job vertex and assigns it with the given name.
	 * 
	 * @param name
	 *        the name of the new job vertex
	 * @param id
	 *        the ID of this vertex
	 * @param jobGraph
	 *        the job graph this vertex belongs to
	 */
	protected AbstractJobVertex(String name, JobVertexID id, JobGraph jobGraph) {

		this.name = name;
		this.id = (id == null) ? new JobVertexID() : id;
		this.jobGraph = jobGraph;
	}

	/**
	 * Connects the job vertex to the specified job vertex.
	 * 
	 * @param vertex
	 *        the vertex this vertex should connect to
	 * @throws JobGraphDefinitionException
	 *         thrown if the given vertex cannot be connected to <code>vertex</code> in the requested manner
	 */
	public void connectTo(AbstractJobVertex vertex) throws JobGraphDefinitionException {
		this.connectTo(vertex, null, null, -1, -1);
	}

	/**
	 * Connects the job vertex to the specified job vertex.
	 * 
	 * @param vertex
	 *        the vertex this vertex should connect to
	 * @param indexOfOutputGate
	 *        index of the producing task's output gate to be used, <code>-1</code> will determine the next free index
	 *        number
	 * @param indexOfInputGate
	 *        index of the consuming task's input gate to be used, <code>-1</code> will determine the next free index
	 *        number
	 * @throws JobGraphDefinitionException
	 *         thrown if the given vertex cannot be connected to <code>vertex</code> in the requested manner
	 */
	public void connectTo(AbstractJobVertex vertex, int indexOfOutputGate, int indexOfInputGate)
			throws JobGraphDefinitionException {
		this.connectTo(vertex, null, null, indexOfOutputGate, indexOfInputGate);
	}

	/**
	 * Connects the job vertex to the specified job vertex.
	 * 
	 * @param vertex
	 *        the vertex this vertex should connect to
	 * @param channelType
	 *        the channel type the two vertices should be connected by at runtime
	 * @param compressionLevel
	 *        the compression level the corresponding channel should have at runtime
	 * @throws JobGraphDefinitionException
	 *         thrown if the given vertex cannot be connected to <code>vertex</code> in the requested manner
	 */
	public void connectTo(AbstractJobVertex vertex, ChannelType channelType, CompressionLevel compressionLevel)
			throws JobGraphDefinitionException {
		this.connectTo(vertex, channelType, compressionLevel, -1, -1);
	}

	/**
	 * Connects the job vertex to the specified job vertex.
	 * 
	 * @param vertex
	 *        the vertex this vertex should connect to
	 * @param channelType
	 *        the channel type the two vertices should be connected by at runtime
	 * @param compressionLevel
	 *        the compression level the corresponding channel should have at runtime
	 * @param indexOfOutputGate
	 *        index of the producing task's output gate to be used, <code>-1</code> will determine the next free index
	 *        number
	 * @param indexOfInputGate
	 *        index of the consuming task's input gate to be used, <code>-1</code> will determine the next free index
	 *        number
	 * @throws JobGraphDefinitionException
	 *         thrown if the given vertex cannot be connected to <code>vertex</code> in the requested manner
	 */
	public void connectTo(AbstractJobVertex vertex, ChannelType channelType, CompressionLevel compressionLevel,
			int indexOfOutputGate, int indexOfInputGate) throws JobGraphDefinitionException {

		if (vertex == null) {
			throw new JobGraphDefinitionException("Target vertex is null!");
		}

		if (indexOfOutputGate == -1) {
			indexOfOutputGate = getFirstFreeOutputGateIndex();
		}

		// Make sure the array is big enough
		for (int i = this.forwardEdges.size(); i <= indexOfOutputGate; i++) {
			this.forwardEdges.add(null);
		}

		if (this.forwardEdges.get(indexOfOutputGate) != null) {
			throw new JobGraphDefinitionException("Source vertex " + this.name + " already has an edge at index "
				+ indexOfOutputGate);
		}

		if (indexOfInputGate == -1) {
			indexOfInputGate = vertex.getFirstFreeInputGateIndex();
		} else {
			if (vertex.getBackwardConnection(indexOfInputGate) != null) {
				throw new JobGraphDefinitionException("Target vertex " + vertex.getName()
					+ " already has an edge at index " + indexOfInputGate);
			}
		}

		// Add new edge
		this.forwardEdges.set(indexOfOutputGate, new JobEdge(vertex, channelType, compressionLevel, indexOfInputGate));
		vertex.connectBacklink(this, channelType, compressionLevel, indexOfOutputGate, indexOfInputGate);
	}

	/**
	 * Returns the index of this vertex's first free output gate.
	 * 
	 * @return the index of the first free output gate
	 */
	protected int getFirstFreeOutputGateIndex() {

		for (int i = 0; i < this.forwardEdges.size(); i++) {

			if (this.forwardEdges.get(i) == null) {
				return i;
			}
		}

		return this.forwardEdges.size();
	}

	/**
	 * Returns the index of this vertex's first free input gate.
	 * 
	 * @return the index of the first free input gate
	 */
	protected int getFirstFreeInputGateIndex() {

		for (int i = 0; i < this.backwardEdges.size(); i++) {

			if (this.backwardEdges.get(i) == null) {
				return i;
			}
		}

		return this.backwardEdges.size();
	}

	/**
	 * Creates a backward link from a connected job vertex.
	 * 
	 * @param vertex
	 *        the job vertex to connect to
	 * @param channelType
	 *        the channel type the two vertices should be connected by at runtime
	 * @param compressionLevel
	 *        the compression level the corresponding channel should have at runtime
	 * @param indexOfOutputGate
	 *        index of the producing task's output gate to be used
	 * @param indexOfInputGate
	 *        index of the consuming task's input gate to be used
	 */
	private void connectBacklink(AbstractJobVertex vertex, ChannelType channelType, CompressionLevel compressionLevel,
			int indexOfOutputGate, int indexOfInputGate) {

		// Make sure the array is big enough
		for (int i = this.backwardEdges.size(); i <= indexOfInputGate; i++) {
			this.backwardEdges.add(null);
		}

		this.backwardEdges.set(indexOfInputGate, new JobEdge(vertex, channelType, compressionLevel, indexOfOutputGate));
	}

	/**
	 * Returns the name of the vertex.
	 * 
	 * @return the name of the vertex or <code>null</code> if no name is set.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the number of forward connections.
	 * 
	 * @return the number of forward connections
	 */
	public int getNumberOfForwardConnections() {
		return this.forwardEdges.size();
	}

	/**
	 * Returns the number of backward connections.
	 * 
	 * @return the number of backward connections
	 */
	public int getNumberOfBackwardConnections() {
		return this.backwardEdges.size();
	}

	/**
	 * Returns the forward edge with index <code>index</code>.
	 * 
	 * @param index
	 *        the index of the edge
	 * @return the forward edge or <code>null</code> if no edge exists at the specified index.
	 */
	public JobEdge getForwardConnection(int index) {

		if (index < this.forwardEdges.size()) {
			return this.forwardEdges.get(index);
		}

		return null;
	}

	/**
	 * Returns the backward edge with index <code>index</code>.
	 * 
	 * @param index
	 *        the index of the edge
	 * @return the backward edge or <code>null</code> if no edge exists at the specified index
	 */
	public JobEdge getBackwardConnection(int index) {

		if (index < this.backwardEdges.size()) {
			return this.backwardEdges.get(index);
		}

		return null;
	}

	/**
	 * Returns the index of the edge which is used to connect the given job vertex to this job vertex.
	 * 
	 * @param jv
	 *        the connected job vertex
	 * @return the index of the edge which is used to connect the given job vertex to this job vertex or -1 if the given
	 *         vertex is not connected to this job vertex
	 */
	/*
	 * public int getBackwardConnectionIndex(AbstractJobVertex jv) {
	 * if(jv == null) {
	 * return -1;
	 * }
	 * final Iterator<JobEdge> it = this.backwardEdges.iterator();
	 * int i = 0;
	 * while(it.hasNext()) {
	 * final JobEdge edge = it.next();
	 * if(edge.getConnectedVertex() == jv) {
	 * return i;
	 * }
	 * i++;
	 * }
	 * return -1;
	 * }
	 */

	/**
	 * Returns the ID of this job vertex.
	 * 
	 * @return the ID of this job vertex
	 */
	public JobVertexID getID() {
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void read(DataInput in) throws IOException {

		if (jobGraph == null) {
			throw new IOException("jobGraph is null, cannot deserialize");
		}

		// Read instance type
		this.instanceType = StringRecord.readString(in);

		// Read number of subtasks
		this.numberOfSubtasks = in.readInt();

		// Read number of subtasks per instance
		this.numberOfSubtasksPerInstance = in.readInt();

		// Read vertex to share instances with
		if (in.readBoolean()) {
			final JobVertexID id = new JobVertexID();
			id.read(in);
			final AbstractJobVertex vertexToShareInstancesWith = this.jobGraph.findVertexByID(id);
			if (vertexToShareInstancesWith == null) {
				throw new IOException("Cannot find vertex with id " + id + " share instances with");
			}

			this.vertexToShareInstancesWith = vertexToShareInstancesWith;
		}

		// Read the configuration
		this.configuration.read(in);

		// Read number of forward edges
		final int numForwardEdges = in.readInt();

		// Now reconnect to other vertices via the reconstruction map
		final JobVertexID tmpID = new JobVertexID();
		for (int i = 0; i < numForwardEdges; i++) {
			if (in.readBoolean()) {
				tmpID.read(in);
				final AbstractJobVertex jv = jobGraph.findVertexByID(tmpID);
				if (jv == null) {
					throw new IOException("Cannot find vertex with id " + tmpID);
				}

				final ChannelType channelType = EnumUtils.readEnum(in, ChannelType.class);
				final CompressionLevel compressionLevel = EnumUtils.readEnum(in, CompressionLevel.class);
				final int indexOfInputGate = in.readInt();

				try {
					this.connectTo(jv, channelType, compressionLevel, i, indexOfInputGate);
				} catch (JobGraphDefinitionException e) {
					throw new IOException(StringUtils.stringifyException(e));
				}
			} else {
				this.forwardEdges.add(null);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(DataOutput out) throws IOException {

		// Instance type
		StringRecord.writeString(out, this.instanceType);

		// Number of subtasks
		out.writeInt(this.numberOfSubtasks);

		// Number of subtasks per instance
		out.writeInt(this.numberOfSubtasksPerInstance);

		// Vertex to share instance with
		if (this.vertexToShareInstancesWith != null) {
			out.writeBoolean(true);
			this.vertexToShareInstancesWith.getID().write(out);
		} else {
			out.writeBoolean(false);
		}

		// Write the configuration
		this.configuration.write(out);

		// We ignore the backward edges and connect them when we reconstruct the graph on the remote side, only write
		// number of forward edges
		out.writeInt(this.forwardEdges.size());

		// Now output the IDs of the vertices this vertex is connected to
		for (int i = 0; i < this.forwardEdges.size(); i++) {
			final JobEdge edge = this.forwardEdges.get(i);
			if (edge == null) {
				out.writeBoolean(false);
			} else {
				out.writeBoolean(true);
				edge.getConnectedVertex().getID().write(out);
				EnumUtils.writeEnum(out, edge.getChannelType());
				EnumUtils.writeEnum(out, edge.getCompressionLevel());
				out.writeInt(edge.getIndexOfInputGate());
			}
		}
	}

	/**
	 * Returns the job graph this job vertex belongs to.
	 * 
	 * @return the job graph this job vertex belongs to or <code>null</code> if no job graph has been set yet
	 */
	public JobGraph getJobGraph() {
		return this.jobGraph;
	}

	/**
	 * Sets the number of subtasks the task this vertex represents should be split into at runtime.
	 * 
	 * @param numberOfSubtasks
	 *        the number of subtasks this vertex represents should be split into at runtime
	 */
	public void setNumberOfSubtasks(int numberOfSubtasks) {
		this.numberOfSubtasks = numberOfSubtasks;
	}

	/**
	 * Returns the number of subtasks the task this vertex represents should be split into at runtime.
	 * 
	 * @return the number of subtasks this vertex represents should be split into at runtime, <code>-1</code> if
	 *         unspecified
	 */
	public int getNumberOfSubtasks() {
		return this.numberOfSubtasks;
	}

	/**
	 * Sets the instance type the task this vertex represents should run on.
	 * 
	 * @param instanceType
	 *        the instance type the task this vertex represents should run on
	 */
	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}

	/**
	 * Returns the instance type the task this vertex represents should run on.
	 * 
	 * @return the instance type the task this vertex represents should run on, <code>null</code> if unspecified
	 */
	public String getInstanceType() {
		return this.instanceType;
	}

	/**
	 * Sets the number of subtasks that should be assigned to the same instance.
	 * 
	 * @param numberOfSubtasksPerInstance
	 *        the number of subtasks that should be assigned to the same instance
	 */
	public void setNumberOfSubtasksPerInstance(int numberOfSubtasksPerInstance) {
		this.numberOfSubtasksPerInstance = numberOfSubtasksPerInstance;
	}

	/**
	 * Returns the number of subtasks that should be assigned to the same instance, <code>-1</code> if undefined.
	 * 
	 * @return the number of subtasks that should be assigned to the same instance, <code>-1</code> if undefined
	 */
	public int getNumberOfSubtasksPerInstance() {
		return this.numberOfSubtasksPerInstance;
	}

	/**
	 * Sets the vertex this vertex should share its instances with at runtime.
	 * 
	 * @param vertex
	 *        the vertex this vertex should share its instances with at runtime
	 */
	public void setVertexToShareInstancesWith(AbstractJobVertex vertex) {
		this.vertexToShareInstancesWith = vertex;
	}

	/**
	 * Returns the vertex this vertex should share its instance with at runtime.
	 * 
	 * @return the vertex this vertex should share its instance with at runtime, <code>null</code> if undefined
	 */
	public AbstractJobVertex getVertexToShareInstancesWith() {
		return this.vertexToShareInstancesWith;
	}

	/**
	 * Returns the vertex's configuration object which can be used to pass custom settings to the task at runtime.
	 * 
	 * @return the vertex's configuration object
	 */
	public Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Performs task specific checks if the
	 * respective task has been configured properly.
	 * 
	 * @param invokable
	 *        an instance of the task this vertex represents
	 * @throws IllegalConfigurationException
	 *         thrown if the respective tasks is not configured properly
	 */
	public abstract void checkConfiguration(AbstractInvokable invokable) throws IllegalConfigurationException;

	/**
	 * Returns the minimum number of subtasks the respective task
	 * must be split into at runtime.
	 * 
	 * @param invokable
	 *        an instance of the task this vertex represents
	 * @return the minimum number of subtasks the respective task must be split into at runtime
	 */
	public abstract int getMinimumNumberOfSubtasks(AbstractInvokable invokable);

	/**
	 * Returns the maximum number of subtasks the respective task
	 * can be split into at runtime.
	 * 
	 * @param invokable
	 *        an instance of the task this vertex represents
	 * @return the maximum number of subtasks the respective task can be split into at runtime, <code>-1</code> for
	 *         infinity
	 */
	public abstract int getMaximumNumberOfSubtasks(AbstractInvokable invokable);

	/**
	 * Returns the invokable class which represents the task of this vertex
	 * 
	 * @return the invokable class, <code>null</code> if it is not set
	 */
	public abstract Class<? extends AbstractInvokable> getInvokableClass();
}
