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

package eu.stratosphere.example.record.relational;

import java.io.Serializable;
import java.util.Iterator;

import eu.stratosphere.api.Job;
import eu.stratosphere.api.Program;
import eu.stratosphere.api.ProgramDescription;
import eu.stratosphere.api.operators.FileDataSink;
import eu.stratosphere.api.operators.FileDataSource;
import eu.stratosphere.api.operators.util.FieldSet;
import eu.stratosphere.api.record.functions.JoinFunction;
import eu.stratosphere.api.record.functions.ReduceFunction;
import eu.stratosphere.api.record.functions.FunctionAnnotation.ConstantFields;
import eu.stratosphere.api.record.functions.FunctionAnnotation.ConstantFieldsSecondExcept;
import eu.stratosphere.api.record.io.CsvInputFormat;
import eu.stratosphere.api.record.io.CsvOutputFormat;
import eu.stratosphere.api.record.operators.JoinOperator;
import eu.stratosphere.api.record.operators.ReduceOperator;
import eu.stratosphere.api.record.operators.ReduceOperator.Combinable;
import eu.stratosphere.types.PactInteger;
import eu.stratosphere.types.PactRecord;
import eu.stratosphere.types.PactString;
import eu.stratosphere.util.Collector;

/**
 * The TPC-H is a decision support benchmark on relational data.
 * Its documentation and the data generator (DBGEN) can be found
 * on http://www.tpc.org/tpch/ .This implementation is tested with
 * the DB2 data format.  
 * 
 * This PACT program implements a query on the TPC-H schema 
 * including one join and an aggregation.
 * This query is used as example in the Asterix project
 * (http://asterix.ics.uci.edu/).
 * 
 * SELECT c_mktsegment, COUNT(o_orderkey)
 *   FROM orders, customer
 *   WHERE c_custkey = o_custkey
 * GROUP BY c_mktsegment;
 * 
 * @author Fabian Hueske (fabian.hueske@tu-berlin.de)
 */

public class TPCHQueryAsterix implements Program, ProgramDescription {

	/**
	 * Realizes the join between Customers and Order table.
	 */
	@ConstantFieldsSecondExcept(0)
	public static class JoinCO extends JoinFunction implements Serializable {
		private static final long serialVersionUID = 1L;

		private final PactInteger one = new PactInteger(1);
		
		/**
		 * Output Schema:
		 *  0: PARTIAL_COUNT=1
		 *  1: C_MKTSEGMENT
		 */
		@Override
		public void match(PactRecord order, PactRecord cust, Collector<PactRecord> out)
				throws Exception {
			cust.setField(0, one);
			out.collect(cust);
		}
	}

	/**
	 * Reduce PACT implements the aggregation of the results. The 
	 * Combinable annotation is set as the partial counts can be calculated
	 * already in the combiner
	 *
	 */
	@Combinable
	@ConstantFields(1)
	public static class AggCO extends ReduceFunction implements Serializable {
		private static final long serialVersionUID = 1L;

		private final PactInteger integer = new PactInteger();
		private PactRecord record = new PactRecord();
	
		/**
		 * Output Schema:
		 * 0: COUNT
		 * 1: C_MKTSEGMENT
		 *
		 */
		@Override
		public void reduce(Iterator<PactRecord> records, Collector<PactRecord> out)
				throws Exception {

			int count = 0;

			while (records.hasNext()) {
				record = records.next();
				count+=record.getField(0, integer).getValue();
			}

			integer.setValue(count);
			record.setField(0, integer);
			out.collect(record);
		}
		
		/**
		 * Computes partial counts
		 */
		public void combine(Iterator<PactRecord> records, Collector<PactRecord> out)
				throws Exception {
			reduce(records, out);
		}

	}


	@Override
	public Job createJob(final String... args) {

		// parse program parameters
		int numSubtasks       = (args.length > 0 ? Integer.parseInt(args[0]) : 1);
		String ordersPath    = (args.length > 1 ? args[1] : "");
		String customerPath  = (args.length > 2 ? args[2] : "");
		String output        = (args.length > 3 ? args[3] : "");

		/*
		 * Output Schema:
		 * 0: CUSTOMER_ID
		 */
		// create DataSourceContract for Orders input
		FileDataSource orders = new FileDataSource(new CsvInputFormat(), ordersPath, "Orders");
		orders.setDegreeOfParallelism(numSubtasks);
		CsvInputFormat.configureRecordFormat(orders)
			.recordDelimiter('\n')
			.fieldDelimiter('|')
			.field(PactInteger.class, 1);
		// compiler hints
		orders.getCompilerHints().setAvgBytesPerRecord(5);
		orders.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(new int[]{0}), 10);
		
		/*
		 * Output Schema:
		 * 0: CUSTOMER_ID
		 * 1: MKT_SEGMENT
		 */
		// create DataSourceContract for Customer input
		FileDataSource customers = new FileDataSource(new CsvInputFormat(), customerPath, "Customers");
		customers.setDegreeOfParallelism(numSubtasks);
		CsvInputFormat.configureRecordFormat(customers)
			.recordDelimiter('\n')
			.fieldDelimiter('|')
			.field(PactInteger.class, 0)
			.field(PactString.class, 6);
		// compiler hints
		customers.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(new int[]{0}), 1);
		customers.getCompilerHints().setAvgBytesPerRecord(20);
		
		// create JoinOperator for joining Orders and LineItems
		JoinOperator joinCO = JoinOperator.builder(new JoinCO(), PactInteger.class, 0, 0)
			.name("JoinCO")
			.build();
		joinCO.setDegreeOfParallelism(numSubtasks);
		// compiler hints
		joinCO.getCompilerHints().setAvgBytesPerRecord(17);

		// create ReduceOperator for aggregating the result
		ReduceOperator aggCO = ReduceOperator.builder(new AggCO(), PactString.class, 1)
			.name("AggCo")
			.build();
		aggCO.setDegreeOfParallelism(numSubtasks);
		// compiler hints
		aggCO.getCompilerHints().setAvgBytesPerRecord(17);
		aggCO.getCompilerHints().setAvgNumRecordsPerDistinctFields(new FieldSet(new int[]{0}), 1);

		// create DataSinkContract for writing the result
		FileDataSink result = new FileDataSink(new CsvOutputFormat(), output, "Output");
		result.setDegreeOfParallelism(numSubtasks);
		CsvOutputFormat.configureRecordFormat(result)
			.recordDelimiter('\n')
			.fieldDelimiter('|')
			.field(PactInteger.class, 0)
			.field(PactString.class, 1);

		// assemble the PACT plan
		result.addInput(aggCO);
		aggCO.addInput(joinCO);
		joinCO.addFirstInput(orders);
		joinCO.addSecondInput(customers);

		// return the PACT plan
		return new Job(result, "TPCH Asterix");
	}


	@Override
	public String getDescription() {
		return "Parameters: [numSubStasks], [orders], [customer], [output]";
	}
}
