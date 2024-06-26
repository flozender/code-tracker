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

package org.apache.flink.streaming.api.datastream;

import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.functions.RichGroupReduceFunction;
import org.apache.flink.api.common.functions.RichReduceFunction;
import org.apache.flink.api.common.typeinfo.BasicArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.api.invokable.StreamInvokable;
import org.apache.flink.streaming.api.invokable.operator.windowing.StreamWindow;
import org.apache.flink.streaming.api.invokable.operator.windowing.StreamWindowTypeInfo;
import org.apache.flink.streaming.api.invokable.operator.windowing.WindowFlattener;
import org.apache.flink.streaming.api.invokable.operator.windowing.WindowMapper;
import org.apache.flink.streaming.api.invokable.operator.windowing.WindowMerger;
import org.apache.flink.streaming.api.invokable.operator.windowing.WindowPartitioner;
import org.apache.flink.streaming.api.invokable.operator.windowing.WindowReducer;

/**
 * A {@link DiscretizedStream} represents a data stream that has been divided
 * into windows (predefined chunks). User defined function such as
 * {@link #reduceWindow(ReduceFunction)},
 * {@link #mapWindow(GroupReduceFunction)} or aggregations can be applied to the
 * windows.
 * 
 * @param <OUT>
 *            The output type of the {@link DiscretizedStream}
 */
public class DiscretizedStream<OUT> extends WindowedDataStream<OUT> {

	private SingleOutputStreamOperator<StreamWindow<OUT>, ?> discretizedStream;
	private WindowTransformation transformation;

	protected DiscretizedStream(SingleOutputStreamOperator<StreamWindow<OUT>, ?> discretizedStream,
			KeySelector<OUT, ?> groupByKey, WindowTransformation tranformation) {
		super();
		this.groupByKey = groupByKey;
		this.discretizedStream = discretizedStream;
		this.transformation = tranformation;
	}

	/**
	 * Applies a reduce transformation on the windowed data stream by reducing
	 * the current window at every trigger.The user can also extend the
	 * {@link RichReduceFunction} to gain access to other features provided by
	 * the {@link org.apache.flink.api.common.functions.RichFunction} interface.
	 * 
	 * @param reduceFunction
	 *            The reduce function that will be applied to the windows.
	 * @return The transformed DataStream
	 */
	@Override
	public DiscretizedStream<OUT> reduceWindow(ReduceFunction<OUT> reduceFunction) {

		DiscretizedStream<OUT> out = partition(transformation).transform(
				WindowTransformation.REDUCEWINDOW, "Window Reduce", getType(),
				new WindowReducer<OUT>(reduceFunction)).merge();

		if (!isGrouped()) {
			return out.transform(WindowTransformation.REDUCEWINDOW, "Window Reduce", out.getType(),
					new WindowReducer<OUT>(reduceFunction));
		} else {
			return out;
		}
	}

	/**
	 * Applies a reduceGroup transformation on the windowed data stream by
	 * reducing the current window at every trigger. In contrast with the
	 * standard binary reducer, with reduceGroup the user can access all
	 * elements of the window at the same time through the iterable interface.
	 * The user can also extend the {@link RichGroupReduceFunction} to gain
	 * access to other features provided by the
	 * {@link org.apache.flink.api.common.functions.RichFunction} interface.
	 * 
	 * @param reduceFunction
	 *            The reduce function that will be applied to the windows.
	 * @return The transformed DataStream
	 */
	@Override
	public <R> DiscretizedStream<R> mapWindow(GroupReduceFunction<OUT, R> reduceFunction) {

		TypeInformation<R> retType = TypeExtractor.getGroupReduceReturnTypes(reduceFunction,
				getType());

		return mapWindow(reduceFunction, retType);
	}

	@Override
	public <R> DiscretizedStream<R> mapWindow(GroupReduceFunction<OUT, R> reduceFunction,
			TypeInformation<R> returnType) {
		DiscretizedStream<R> out = partition(transformation).transform(
				WindowTransformation.REDUCEWINDOW, "Window Reduce", returnType,
				new WindowMapper<OUT, R>(reduceFunction));

		if (isGrouped()) {
			return out.merge();
		} else {
			return out;
		}
	}

	private <R> DiscretizedStream<R> transform(WindowTransformation transformation,
			String operatorName, TypeInformation<R> retType,
			StreamInvokable<StreamWindow<OUT>, StreamWindow<R>> invokable) {

		return wrap(discretizedStream.transform(operatorName, new StreamWindowTypeInfo<R>(retType),
				invokable), transformation);
	}

	private DiscretizedStream<OUT> partition(WindowTransformation transformation) {

		int parallelism = discretizedStream.getParallelism();

		if (isGrouped()) {
			DiscretizedStream<OUT> out = transform(transformation, "Window partitioner", getType(),
					new WindowPartitioner<OUT>(groupByKey)).setParallelism(parallelism);

			out.groupByKey = null;

			return out;
		} else if (transformation == WindowTransformation.MAPWINDOW) {
			return transform(
					transformation,
					"Window partitioner",
					getType(),
					new WindowPartitioner<OUT>(parallelism)).setParallelism(parallelism);
		} else {
			return this;
		}
	}

	private DiscretizedStream<OUT> setParallelism(int parallelism) {
		return wrap(discretizedStream.setParallelism(parallelism));
	}

	private DiscretizedStream<OUT> merge() {
		TypeInformation<StreamWindow<OUT>> type = discretizedStream.getType();

		return wrap(discretizedStream.groupBy(new WindowKey<OUT>()).transform("Window Merger",
				type, new WindowMerger<OUT>()));
	}

	public DataStream<OUT> flatten() {
		return discretizedStream.transform("Window Flatten", getType(), new WindowFlattener<OUT>());
	}

	@SuppressWarnings("rawtypes")
	private <R> DiscretizedStream<R> wrap(SingleOutputStreamOperator stream) {
		return wrap(stream, transformation);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <R> DiscretizedStream<R> wrap(SingleOutputStreamOperator stream,
			WindowTransformation transformation) {
		return new DiscretizedStream<R>(stream, (KeySelector<R, ?>) this.groupByKey, transformation);
	}

	public DataStream<StreamWindow<OUT>> getDiscretizedStream() {
		return discretizedStream;
	}

	@SuppressWarnings("rawtypes")
	protected Class<?> getClassAtPos(int pos) {
		Class<?> type;
		TypeInformation<OUT> outTypeInfo = getType();
		if (outTypeInfo.isTupleType()) {
			type = ((TupleTypeInfo) outTypeInfo).getTypeAt(pos).getTypeClass();

		} else if (outTypeInfo instanceof BasicArrayTypeInfo) {

			type = ((BasicArrayTypeInfo) outTypeInfo).getComponentTypeClass();

		} else if (outTypeInfo instanceof PrimitiveArrayTypeInfo) {
			Class<?> clazz = outTypeInfo.getTypeClass();
			if (clazz == boolean[].class) {
				type = Boolean.class;
			} else if (clazz == short[].class) {
				type = Short.class;
			} else if (clazz == int[].class) {
				type = Integer.class;
			} else if (clazz == long[].class) {
				type = Long.class;
			} else if (clazz == float[].class) {
				type = Float.class;
			} else if (clazz == double[].class) {
				type = Double.class;
			} else if (clazz == char[].class) {
				type = Character.class;
			} else {
				throw new IndexOutOfBoundsException("Type could not be determined for array");
			}

		} else if (pos == 0) {
			type = outTypeInfo.getTypeClass();
		} else {
			throw new IndexOutOfBoundsException("Position is out of range");
		}
		return type;
	}

	/**
	 * Gets the output type.
	 * 
	 * @return The output type.
	 */
	public TypeInformation<OUT> getType() {
		return ((StreamWindowTypeInfo<OUT>) discretizedStream.getType()).getInnerType();
	}

	protected DiscretizedStream<OUT> copy() {
		return new DiscretizedStream<OUT>(discretizedStream.copy(), groupByKey, transformation);
	}

}
