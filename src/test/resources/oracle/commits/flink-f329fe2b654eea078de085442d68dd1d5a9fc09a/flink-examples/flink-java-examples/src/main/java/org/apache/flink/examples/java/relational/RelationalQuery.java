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

package org.apache.flink.examples.java.relational;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.aggregation.Aggregations;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple5;

import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * This program implements the following relational query on the TPC-H data set.
 * 
 * <p>
 * <code><pre>
 * SELECT l_orderkey, o_shippriority, sum(l_extendedprice) as revenue
 *   FROM orders, lineitem
 *   WHERE l_orderkey = o_orderkey
 *     AND o_orderstatus = "X"
 *     AND YEAR(o_orderdate) > Y
 *     AND o_orderpriority LIKE "Z%"
 *   GROUP BY l_orderkey, o_shippriority;
 * </pre></code>
 *        
 * <p>
 * Input files are plain text CSV files using the pipe character ('|') as field separator 
 * as generated by the TPC-H data generator which is available at <a href="http://www.tpc.org/tpch/">http://www.tpc.org/tpch/</a>.
 * 
 * <p>
 * Usage: <code>RelationalQuery &lt;orders-csv path&gt; &lt;lineitem-csv path&gt; &lt;result path&gt;</code><br>
 *  
 * <p>
 * This example shows how to use:
 * <ul>
 * <li> tuple data types
 * <li> inline-defined functions
 * <li> projection and join projection
 * <li> build-in aggregation functions
 * </ul>
 */
@SuppressWarnings("serial")
public class RelationalQuery {
	
	// *************************************************************************
	//     PROGRAM
	// *************************************************************************
	
	private static String STATUS_FILTER = "F";
	private static int YEAR_FILTER = 1993;
	private static String OPRIO_FILTER = "5";
	
	public static void main(String[] args) throws Exception {
		
		if(!parseParameters(args)) {
			return;
		}
		
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		// get orders data set: (orderkey, orderstatus, orderdate, orderpriority, shippriority)
		DataSet<Tuple5<Integer, String, String, String, Integer>> orders = getOrdersDataSet(env);

		// get lineitem data set: (orderkey, extendedprice)
		DataSet<Tuple2<Integer, Double>> lineitems = getLineitemDataSet(env);

		// orders filtered by year: (orderkey, custkey)
		DataSet<Tuple2<Integer, Integer>> ordersFilteredByYear =
				// filter orders
				orders.filter(
								new FilterFunction<Tuple5<Integer, String, String, String, Integer>>() {
									@Override
									public boolean filter(Tuple5<Integer, String, String, String, Integer> t) {
										// status filter
										if(!t.f1.equals(STATUS_FILTER)) {
											return false;
										// year filter
										} else if(Integer.parseInt(t.f2.substring(0, 4)) <= YEAR_FILTER) {
											return false;
										// order priority filter
										} else if(!t.f3.startsWith(OPRIO_FILTER)) {
											return false;
										}
										return true;
									}
								})
				// project fields out that are no longer required
				.project(0,4).types(Integer.class, Integer.class);

		// join orders with lineitems: (orderkey, shippriority, extendedprice)
		DataSet<Tuple3<Integer, Integer, Double>> lineitemsOfOrders = 
				ordersFilteredByYear.joinWithHuge(lineitems)
									.where(0).equalTo(0)
									.projectFirst(0,1).projectSecond(1)
									.types(Integer.class, Integer.class, Double.class);

		// extendedprice sums: (orderkey, shippriority, sum(extendedprice))
		DataSet<Tuple3<Integer, Integer, Double>> priceSums =
				// group by order and sum extendedprice
				lineitemsOfOrders.groupBy(0,1).aggregate(Aggregations.SUM, 2);

		// emit result
		priceSums.writeAsCsv(outputPath);
		
		// execute program
		env.execute("Relational Query Example");
		
	}
	
	// *************************************************************************
	//     UTIL METHODS
	// *************************************************************************
	
	private static String ordersPath;
	private static String lineitemPath;
	private static String outputPath;
	
	private static boolean parseParameters(String[] programArguments) {
		
		if(programArguments.length > 0) {
			if(programArguments.length == 3) {
				ordersPath = programArguments[0];
				lineitemPath = programArguments[1];
				outputPath = programArguments[2];
			} else {
				System.err.println("Usage: RelationalQuery <orders-csv path> <lineitem-csv path> <result path>");
				return false;
			}
		} else {
			System.err.println("This program expects data from the TPC-H benchmark as input data.\n" +
								"  Due to legal restrictions, we can not ship generated data.\n" +
								"  You can find the TPC-H data generator at http://www.tpc.org/tpch/.\n" + 
								"  Usage: RelationalQuery <orders-csv path> <lineitem-csv path> <result path>");
			return false;
		}
		return true;
	}
	
	private static DataSet<Tuple5<Integer, String, String, String, Integer>> getOrdersDataSet(ExecutionEnvironment env) {
		return env.readCsvFile(ordersPath)
					.fieldDelimiter('|')
					.includeFields("101011010")
					.types(Integer.class, String.class, String.class, String.class, Integer.class);
	}

	private static DataSet<Tuple2<Integer, Double>> getLineitemDataSet(ExecutionEnvironment env) {
		return env.readCsvFile(lineitemPath)
					.fieldDelimiter('|')
					.includeFields("1000010000000000")
					.types(Integer.class, Double.class);
	}
	
}
