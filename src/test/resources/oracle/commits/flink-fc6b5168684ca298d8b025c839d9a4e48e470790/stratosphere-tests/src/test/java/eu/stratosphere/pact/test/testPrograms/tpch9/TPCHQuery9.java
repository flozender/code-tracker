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

package eu.stratosphere.pact.test.testPrograms.tpch9;

import org.apache.log4j.Logger;

import eu.stratosphere.api.operators.FileDataSink;
import eu.stratosphere.api.operators.FileDataSource;
import eu.stratosphere.api.plan.Plan;
import eu.stratosphere.api.plan.PlanAssembler;
import eu.stratosphere.api.plan.PlanAssemblerDescription;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.MatchContract;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.test.testPrograms.util.IntTupleDataInFormat;
import eu.stratosphere.types.PactInteger;

/**
 * Quote from the TPC-H homepage:
 * "The TPC-H is a decision support benchmark on relational data.
 * Its documentation and the data generator (DBGEN) can be found
 * on http://www.tpc.org/tpch/ .This implementation is tested with
 * the DB2 data format."
 * This PACT program implements the query 9 of the TPC-H benchmark:
 * 
 * <pre>
 * select nation, o_year, sum(amount) as sum_profit
 * from (
 *   select n_name as nation, extract(year from o_orderdate) as o_year,
 *          l_extendedprice * (1 - l_discount) - ps_supplycost * l_quantity as amount
 *   from part, supplier, lineitem, partsupp, orders, nation
 *   where
 *     s_suppkey = l_suppkey and ps_suppkey = l_suppkey and ps_partkey = l_partkey
 *     and p_partkey = l_partkey and o_orderkey = l_orderkey and s_nationkey = n_nationkey
 *     and p_name like '%[COLOR]%'
 * ) as profit
 * group by nation, o_year
 * order by nation, o_year desc;
 * </pre>
 * 
 * Plan:<br>
 * Match "part" and "partsupp" on "partkey" -> "parts" with (partkey, suppkey) as key
 * Match "orders" and "lineitem" on "orderkey" -> "ordered_parts" with (partkey, suppkey) as key
 * Match "parts" and "ordered_parts" on (partkey, suppkey) -> "filtered_parts" with "suppkey" as key
 * Match "supplier" and "nation" on "nationkey" -> "suppliers" with "suppkey" as key
 * Match "filtered_parts" and "suppliers" on" suppkey" -> "partlist" with (nation, o_year) as key
 * Group "partlist" by (nation, o_year), calculate sum(amount)
 * 
 * <b>Attention:</b> The "order by" part is not implemented!
 * 
 * @author Dennis Schneider <dschneid@informatik.hu-berlin.de>
 */

public class TPCHQuery9 implements PlanAssembler, PlanAssemblerDescription {
	public final String ARGUMENTS = "dop partInputPath partSuppInputPath ordersInputPath lineItemInputPath supplierInputPath nationInputPath outputPath";

	private static Logger LOGGER = Logger.getLogger(TPCHQuery9.class);

	private int degreeOfParallelism = 1;

	private String partInputPath, partSuppInputPath, ordersInputPath, lineItemInputPath, supplierInputPath,
			nationInputPath;

	private String outputPath;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Plan getPlan(String... args) throws IllegalArgumentException {

		if (args.length != 8)
		{
			LOGGER.warn("number of arguments do not match!");
			
			this.degreeOfParallelism = 1;
			this.partInputPath = "";
			this.partSuppInputPath = "";
			this.ordersInputPath = "";
			this.lineItemInputPath = "";
			this.supplierInputPath = "";
			this.nationInputPath = "";
			this.outputPath = "";
		}else
		{
			this.degreeOfParallelism = Integer.parseInt(args[0]);
			this.partInputPath = args[1];
			this.partSuppInputPath = args[2];
			this.ordersInputPath = args[3];
			this.lineItemInputPath = args[4];
			this.supplierInputPath = args[5];
			this.nationInputPath = args[6];
			this.outputPath = args[7];
		}
		
		/* Create the 6 data sources: */
		/* part: (partkey | name, mfgr, brand, type, size, container, retailprice, comment) */
		FileDataSource partInput = new FileDataSource(
			new IntTupleDataInFormat(), this.partInputPath, "\"part\" source");
		//partInput.setOutputContract(UniqueKey.class);
//		partInput.getCompilerHints().setAvgNumValuesPerKey(1);

		/* partsupp: (partkey | suppkey, availqty, supplycost, comment) */
		FileDataSource partSuppInput = new FileDataSource(
			new IntTupleDataInFormat(), this.partSuppInputPath, "\"partsupp\" source");

		/* orders: (orderkey | custkey, orderstatus, totalprice, orderdate, orderpriority, clerk, shippriority, comment) */
		FileDataSource ordersInput = new FileDataSource(
			new IntTupleDataInFormat(), this.ordersInputPath, "\"orders\" source");
		//ordersInput.setOutputContract(UniqueKey.class);
//		ordersInput.getCompilerHints().setAvgNumValuesPerKey(1);

		/* lineitem: (orderkey | partkey, suppkey, linenumber, quantity, extendedprice, discount, tax, ...) */
		FileDataSource lineItemInput = new FileDataSource(
			new IntTupleDataInFormat(), this.lineItemInputPath, "\"lineitem\" source");

		/* supplier: (suppkey | name, address, nationkey, phone, acctbal, comment) */
		FileDataSource supplierInput = new FileDataSource(
			new IntTupleDataInFormat(), this.supplierInputPath, "\"supplier\" source");
		//supplierInput.setOutputContract(UniqueKey.class);
//		supplierInput.getCompilerHints().setAvgNumValuesPerKey(1);

		/* nation: (nationkey | name, regionkey, comment) */
		FileDataSource nationInput = new FileDataSource(
			new IntTupleDataInFormat(), this.nationInputPath, "\"nation\" source");
		//nationInput.setOutputContract(UniqueKey.class);
//		nationInput.getCompilerHints().setAvgNumValuesPerKey(1);

		/* Filter on part's name, project values to NULL: */
		MapContract filterPart = MapContract.builder(PartFilter.class)
			.name("filterParts")
			.build();

		/* Map to change the key element of partsupp, project value to (supplycost, suppkey): */
		MapContract mapPartsupp = MapContract.builder(PartsuppMap.class)
			.name("mapPartsupp")
			.build();

		/* Map to extract the year from order: */
		MapContract mapOrder = MapContract.builder(OrderMap.class)
			.name("mapOrder")
			.build();

		/* Project value to (partkey, suppkey, quantity, price = extendedprice*(1-discount)): */
		MapContract mapLineItem = MapContract.builder(LineItemMap.class)
			.name("proj.Partsupp")
			.build();

		/* - change the key of supplier to nationkey, project value to suppkey */
		MapContract mapSupplier = MapContract.builder(SupplierMap.class)
			.name("proj.Partsupp")
			.build();

		/* Equijoin on partkey of part and partsupp: */
		MatchContract partsJoin = MatchContract.builder(PartJoin.class, PactInteger.class, 0, 0)
			.name("partsJoin")
			.build();

		/* Equijoin on orderkey of orders and lineitem: */
		MatchContract orderedPartsJoin =
			MatchContract.builder(OrderedPartsJoin.class, PactInteger.class, 0, 0)
			.name("orderedPartsJoin")
			.build();

		/* Equijoin on nationkey of supplier and nation: */
		MatchContract suppliersJoin =
			MatchContract.builder(SuppliersJoin.class, PactInteger.class, 0, 0)
			.name("suppliersJoin")
			.build();

		/* Equijoin on (partkey,suppkey) of parts and orderedParts: */
		MatchContract filteredPartsJoin =
			MatchContract.builder(FilteredPartsJoin.class, IntPair.class, 0, 0)
			.name("filteredPartsJoin")
			.build();

		/* Equijoin on suppkey of filteredParts and suppliers: */
		MatchContract partListJoin =
			MatchContract.builder(PartListJoin.class, PactInteger.class , 0, 0)
			.name("partlistJoin")
			.build();

		/* Aggregate sum(amount) by (nation,year): */
		ReduceContract sumAmountAggregate =
			ReduceContract.builder(AmountAggregate.class, StringIntPair.class, 0)
			.name("groupyBy")
			.build();

		/* Connect input filters: */
		filterPart.addInput(partInput);
		mapPartsupp.addInput(partSuppInput);
		mapOrder.addInput(ordersInput);
		mapLineItem.addInput(lineItemInput);
		mapSupplier.addInput(supplierInput);

		/* Connect equijoins: */
		partsJoin.addFirstInput(filterPart);
		partsJoin.addSecondInput(mapPartsupp);
		orderedPartsJoin.addFirstInput(mapOrder);
		orderedPartsJoin.addSecondInput(mapLineItem);
		suppliersJoin.addFirstInput(mapSupplier);
		suppliersJoin.addSecondInput(nationInput);
		filteredPartsJoin.addFirstInput(partsJoin);
		filteredPartsJoin.addSecondInput(orderedPartsJoin);
		partListJoin.addFirstInput(filteredPartsJoin);
		partListJoin.addSecondInput(suppliersJoin);

		/* Connect aggregate: */
		sumAmountAggregate.addInput(partListJoin);

		/* Connect sink: */
		FileDataSink result = new FileDataSink(new StringIntPairStringDataOutFormat(), this.outputPath, "Results sink");
		result.addInput(sumAmountAggregate);

		Plan p = new Plan(result, "TPC-H query 9");
		p.setDefaultParallelism(this.degreeOfParallelism);
		return p;
	}

	@Override
	public String getDescription() {
		return "TPC-H query 9, parameters: " + this.ARGUMENTS;
	}
}
