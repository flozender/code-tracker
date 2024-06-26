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


package org.apache.flink.hadoopcompatibility.mapred;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.api.common.io.FileInputFormat.FileBaseStatistics;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.api.java.typeutils.WritableTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.hadoopcompatibility.mapred.utils.HadoopUtils;
import org.apache.flink.hadoopcompatibility.mapred.wrapper.HadoopDummyReporter;
import org.apache.flink.hadoopcompatibility.mapred.wrapper.HadoopInputSplit;
import org.apache.flink.types.TypeInformation;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.util.ReflectionUtils;

public class HadoopInputFormat<K extends Writable, V extends Writable> implements InputFormat<Tuple2<K,V>, HadoopInputSplit>, ResultTypeQueryable<Tuple2<K,V>> {
	
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG = LoggerFactory.getLogger(HadoopInputFormat.class);
	
	private org.apache.hadoop.mapred.InputFormat<K, V> mapredInputFormat;
	private Class<K> keyClass;
	private Class<V> valueClass;
	private JobConf jobConf;
	
	private transient K key;
	private transient V value;
	
	private transient RecordReader<K, V> recordReader;
	private transient boolean fetched = false;
	private transient boolean hasNext;
	
	public HadoopInputFormat() {
		super();
	}
	
	public HadoopInputFormat(org.apache.hadoop.mapred.InputFormat<K,V> mapredInputFormat, Class<K> key, Class<V> value, JobConf job) {
		super();
		this.mapredInputFormat = mapredInputFormat;
		this.keyClass = key;
		this.valueClass = value;
		HadoopUtils.mergeHadoopConf(job);
		this.jobConf = job;
	}
	
	public void setJobConf(JobConf job) {
		this.jobConf = job;
	}
	
	public org.apache.hadoop.mapred.InputFormat<K,V> getHadoopInputFormat() {
		return mapredInputFormat;
	}
	
	public void setHadoopInputFormat(org.apache.hadoop.mapred.InputFormat<K,V> mapredInputFormat) {
		this.mapredInputFormat = mapredInputFormat;
	}
	
	public JobConf getJobConf() {
		return jobConf;
	}
	
	// --------------------------------------------------------------------------------------------
	//  InputFormat
	// --------------------------------------------------------------------------------------------
	
	@Override
	public void configure(Configuration parameters) {
		// nothing to do
	}
	
	@Override
	public BaseStatistics getStatistics(BaseStatistics cachedStats) throws IOException {
		// only gather base statistics for FileInputFormats
		if(!(mapredInputFormat instanceof FileInputFormat)) {
			return null;
		}
		
		final FileBaseStatistics cachedFileStats = (cachedStats != null && cachedStats instanceof FileBaseStatistics) ?
				(FileBaseStatistics) cachedStats : null;
		
		try {
			final org.apache.hadoop.fs.Path[] paths = FileInputFormat.getInputPaths(this.jobConf);
			
			return getFileStats(cachedFileStats, paths, new ArrayList<FileStatus>(1));
		} catch (IOException ioex) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("Could not determine statistics due to an io error: "
						+ ioex.getMessage());
			}
		} catch (Throwable t) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Unexpected problem while getting the file statistics: "
						+ t.getMessage(), t);
			}
		}
		
		// no statistics available
		return null;
	}
	
	@Override
	public HadoopInputSplit[] createInputSplits(int minNumSplits)
			throws IOException {
		org.apache.hadoop.mapred.InputSplit[] splitArray = mapredInputFormat.getSplits(jobConf, minNumSplits);
		HadoopInputSplit[] hiSplit = new HadoopInputSplit[splitArray.length];
		for(int i=0;i<splitArray.length;i++){
			hiSplit[i] = new HadoopInputSplit(splitArray[i], jobConf);
		}
		return hiSplit;
	}
	
	@Override
	public InputSplitAssigner getInputSplitAssigner(HadoopInputSplit[] inputSplits) {
		return new DefaultInputSplitAssigner(inputSplits);
	}
	
	@Override
	public void open(HadoopInputSplit split) throws IOException {
		this.recordReader = this.mapredInputFormat.getRecordReader(split.getHadoopInputSplit(), jobConf, new HadoopDummyReporter());
		key = this.recordReader.createKey();
		value = this.recordReader.createValue();
		this.fetched = false;
	}
	
	@Override
	public boolean reachedEnd() throws IOException {
		if(!fetched) {
			fetchNext();
		}
		return !hasNext;
	}
	
	private void fetchNext() throws IOException {
		hasNext = this.recordReader.next(key, value);
		fetched = true;
	}
	
	@Override
	public Tuple2<K, V> nextRecord(Tuple2<K, V> record) throws IOException {
		if(!fetched) {
			fetchNext();
		}
		if(!hasNext) {
			return null;
		}
		record.f0 = key;
		record.f1 = value;
		fetched = false;
		return record;
	}
	
	@Override
	public void close() throws IOException {
		this.recordReader.close();
	}
	
	// --------------------------------------------------------------------------------------------
	//  Helper methods
	// --------------------------------------------------------------------------------------------
	
	private FileBaseStatistics getFileStats(FileBaseStatistics cachedStats, org.apache.hadoop.fs.Path[] hadoopFilePaths,
			ArrayList<FileStatus> files) throws IOException {
		
		long latestModTime = 0L;
		
		// get the file info and check whether the cached statistics are still valid.
		for(org.apache.hadoop.fs.Path hadoopPath : hadoopFilePaths) {
			
			final Path filePath = new Path(hadoopPath.toUri());
			final FileSystem fs = FileSystem.get(filePath.toUri());
			
			final FileStatus file = fs.getFileStatus(filePath);
			latestModTime = Math.max(latestModTime, file.getModificationTime());
			
			// enumerate all files and check their modification time stamp.
			if (file.isDir()) {
				FileStatus[] fss = fs.listStatus(filePath);
				files.ensureCapacity(files.size() + fss.length);
				
				for (FileStatus s : fss) {
					if (!s.isDir()) {
						files.add(s);
						latestModTime = Math.max(s.getModificationTime(), latestModTime);
					}
				}
			} else {
				files.add(file);
			}
		}
		
		// check whether the cached statistics are still valid, if we have any
		if (cachedStats != null && latestModTime <= cachedStats.getLastModificationTime()) {
			return cachedStats;
		}
		
		// calculate the whole length
		long len = 0;
		for (FileStatus s : files) {
			len += s.getLen();
		}
		
		// sanity check
		if (len <= 0) {
			len = BaseStatistics.SIZE_UNKNOWN;
		}
		
		return new FileBaseStatistics(latestModTime, len, BaseStatistics.AVG_RECORD_BYTES_UNKNOWN);
	}
	
	// --------------------------------------------------------------------------------------------
	//  Custom serialization methods
	// --------------------------------------------------------------------------------------------
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeUTF(mapredInputFormat.getClass().getName());
		out.writeUTF(keyClass.getName());
		out.writeUTF(valueClass.getName());
		jobConf.write(out);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		String hadoopInputFormatClassName = in.readUTF();
		String keyClassName = in.readUTF();
		String valueClassName = in.readUTF();
		if(jobConf == null) {
			jobConf = new JobConf();
		}
		jobConf.readFields(in);
		try {
			this.mapredInputFormat = (org.apache.hadoop.mapred.InputFormat<K,V>) Class.forName(hadoopInputFormatClassName, true, Thread.currentThread().getContextClassLoader()).newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Unable to instantiate the hadoop input format", e);
		}
		try {
			this.keyClass = (Class<K>) Class.forName(keyClassName, true, Thread.currentThread().getContextClassLoader());
		} catch (Exception e) {
			throw new RuntimeException("Unable to find key class.", e);
		}
		try {
			this.valueClass = (Class<V>) Class.forName(valueClassName, true, Thread.currentThread().getContextClassLoader());
		} catch (Exception e) {
			throw new RuntimeException("Unable to find value class.", e);
		}
		ReflectionUtils.setConf(mapredInputFormat, jobConf);
	}
	
	// --------------------------------------------------------------------------------------------
	//  ResultTypeQueryable
	// --------------------------------------------------------------------------------------------
	
	@Override
	public TypeInformation<Tuple2<K,V>> getProducedType() {
		return new TupleTypeInfo<Tuple2<K,V>>(new WritableTypeInfo<K>((Class<K>) keyClass), new WritableTypeInfo<V>((Class<V>) valueClass));
	}
}
