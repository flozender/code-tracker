/***********************************************************************************************************************
 * Copyright (C) 2010-2013 by the Stratosphere project (http://stratosphere.eu)
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

package eu.stratosphere.test.testPrograms.tpch4;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.log4j.Logger;

import eu.stratosphere.api.common.Plan;
import eu.stratosphere.api.common.Program;
import eu.stratosphere.api.common.ProgramDescription;
import eu.stratosphere.api.common.operators.FileDataSink;
import eu.stratosphere.api.common.operators.FileDataSource;
import eu.stratosphere.api.java.record.functions.JoinFunction;
import eu.stratosphere.api.java.record.functions.MapFunction;
import eu.stratosphere.api.java.record.functions.ReduceFunction;
import eu.stratosphere.api.java.record.operators.JoinOperator;
import eu.stratosphere.api.java.record.operators.MapOperator;
import eu.stratosphere.api.java.record.operators.ReduceOperator;
import eu.stratosphere.configuration.Configuration;
import eu.stratosphere.test.testPrograms.util.IntTupleDataInFormat;
import eu.stratosphere.test.testPrograms.util.StringTupleDataOutFormat;
import eu.stratosphere.test.testPrograms.util.Tuple;
import eu.stratosphere.types.IntValue;
import eu.stratosphere.types.Record;
import eu.stratosphere.types.StringValue;
import eu.stratosphere.util.Collector;

/**
 * Implementation of the TPC-H Query 4 as a PACT program.
 * 
 * @author Mathias Peters <mathias.peters@informatik.hu-berlin.de>
 * @author Moritz Kaufmann <moritz.kaufmann@campus.tu-berlin.de>
 */
public class TPCHQuery4 implements Program, ProgramDescription {

	private static Logger LOGGER = Logger.getLogger(TPCHQuery4.class);
	
	private int degreeOfParallelism = 1;
	private String ordersInputPath;
	private String lineItemInputPath;
	private String outputPath;
	
	
	/**
	 * Small {@link MapFunction} to filer out the irrelevant orders.
	 *
	 */
	//@SameKey
	public static class OFilter extends MapFunction {

		private final String dateParamString = "1995-01-01";
		private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		private final GregorianCalendar gregCal = new GregorianCalendar();
		
		private Date paramDate;
		private Date plusThreeMonths;
		
		@Override
		public void open(Configuration parameters) {				
			try {
				this.paramDate = sdf.parse(this.dateParamString);
				this.plusThreeMonths = getPlusThreeMonths(paramDate);
				
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
		
		/* (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stub.MapStub#map(eu.stratosphere.pact.common.type.Key, eu.stratosphere.pact.common.type.Value, eu.stratosphere.pact.common.stub.Collector)
		 */
		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			Tuple tuple = record.getField(1, Tuple.class);
			Date orderDate;
			
			String orderStringDate = tuple.getStringValueAt(4);
			
			try {
				orderDate = sdf.parse(orderStringDate);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			
			if(paramDate.before(orderDate) && plusThreeMonths.after(orderDate))
			{
				out.collect(record);
			}

		}

		/**
		 * Calculates the {@link Date} which is three months after the given one.
		 * @param paramDate of type {@link Date}.
		 * @return a {@link Date} three month later.
		 */
		private Date getPlusThreeMonths(Date paramDate) {
			
			gregCal.setTime(paramDate);
			gregCal.add(Calendar.MONTH, 3);
			Date plusThreeMonths = gregCal.getTime();
			return plusThreeMonths;
		}
	}
	
	/**
	 * Simple filter for the line item selection. It filters all teh tuples that do
	 * not satisfy the &quot;l_commitdate &lt; l_receiptdate&quot; condition.
	 * 
	 */
	//@SameKey
	public static class LiFilter extends MapFunction {

		private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		@Override
		public void map(Record record, Collector<Record> out) throws Exception {
			Tuple tuple = record.getField(1, Tuple.class);
			String commitString = tuple.getStringValueAt(11);
			String receiptString = tuple.getStringValueAt(12);

			Date commitDate;
			Date receiptDate;
			
			try {
				commitDate = sdf.parse(commitString);
				receiptDate = sdf.parse(receiptString);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}

			if (commitDate.before(receiptDate)) {
				out.collect(record);
			}

		}
	}
	
	/**
	 * Implements the equijoin on the orderkey and performs the projection on 
	 * the order priority as well.
	 *
	 */
	public static class JoinLiO extends JoinFunction {
		
		@Override
		public void match(Record order, Record line, Collector<Record> out)
				throws Exception {
			Tuple orderTuple = order.getField(1, Tuple.class);
			
			orderTuple.project(32);
			String newOrderKey = orderTuple.getStringValueAt(0);
			
			order.setField(0, new StringValue(newOrderKey));
			out.collect(order);
		}
	}
	
	/**
	 * Implements the count(*) part. 
	 *
	 */
	//@SameKey
	public static class CountAgg extends ReduceFunction {
		
		/* (non-Javadoc)
		 * @see eu.stratosphere.pact.common.stub.ReduceStub#reduce(eu.stratosphere.pact.common.type.Key, java.util.Iterator, eu.stratosphere.pact.common.stub.Collector)
		 */
		@Override
		public void reduce(Iterator<Record> records, Collector<Record> out) throws Exception {	
			long count = 0;
			Record rec = null;
			
			while(records.hasNext()) {
			 	rec = records.next();
			 	count++;
			}
			
			if(rec != null)
			{
				Tuple tuple = new Tuple();
				tuple.addAttribute("" + count);
				rec.setField(1, tuple);
			}
			
			out.collect(rec);
		}
	}
	

	@Override
	public Plan getPlan(String... args) throws IllegalArgumentException {
		
		if(args == null || args.length != 4)
		{
			LOGGER.warn("number of arguments do not match!");
			this.ordersInputPath = "";
			this.lineItemInputPath = "";
			this.outputPath = "";
		}else
		{
			setArgs(args);
		}
		
		FileDataSource orders = 
			new FileDataSource(new IntTupleDataInFormat(), this.ordersInputPath, "Orders");
		orders.setDegreeOfParallelism(this.degreeOfParallelism);
		//orders.setOutputContract(UniqueKey.class);
		
		FileDataSource lineItems =
			new FileDataSource(new IntTupleDataInFormat(), this.lineItemInputPath, "LineItems");
		lineItems.setDegreeOfParallelism(this.degreeOfParallelism);
		
		FileDataSink result = 
				new FileDataSink(new StringTupleDataOutFormat(), this.outputPath, "Output");
		result.setDegreeOfParallelism(degreeOfParallelism);
		
		MapOperator lineFilter = 
				MapOperator.builder(LiFilter.class)
			.name("LineItemFilter")
			.build();
		lineFilter.setDegreeOfParallelism(degreeOfParallelism);
		
		MapOperator ordersFilter = 
				MapOperator.builder(OFilter.class)
			.name("OrdersFilter")
			.build();
		ordersFilter.setDegreeOfParallelism(degreeOfParallelism);
		
		JoinOperator join = 
				JoinOperator.builder(JoinLiO.class, IntValue.class, 0, 0)
			.name("OrdersLineitemsJoin")
			.build();
			join.setDegreeOfParallelism(degreeOfParallelism);
		
		ReduceOperator aggregation = 
				ReduceOperator.builder(CountAgg.class, StringValue.class, 0)
			.name("AggregateGroupBy")
			.build();
		aggregation.setDegreeOfParallelism(this.degreeOfParallelism);
		
		lineFilter.addInput(lineItems);
		ordersFilter.addInput(orders);
		join.addFirstInput(ordersFilter);
		join.addSecondInput(lineFilter);
		aggregation.addInput(join);
		result.addInput(aggregation);
		
			
		return new Plan(result, "TPC-H 4");
	}

	/**
	 * Get the args into the members.
	 * @param args
	 */
	private void setArgs(String[] args) {
		this.degreeOfParallelism = Integer.parseInt(args[0]);
		this.ordersInputPath = args[1];
		this.lineItemInputPath = args[2];
		this.outputPath = args[3];
	}


	@Override
	public String getDescription() {
		return "Parameters: [dop] [orders-input] [lineitem-input] [output]";
	}

}
