package eu.stratosphere.pact.test.iterative.nephele.customdanglingpagerank;

import eu.stratosphere.api.functions.AbstractStub;
import eu.stratosphere.api.functions.GenericCoGrouper;
import eu.stratosphere.configuration.Configuration;
import eu.stratosphere.pact.test.iterative.nephele.ConfigUtils;
import eu.stratosphere.pact.test.iterative.nephele.customdanglingpagerank.types.VertexWithRank;
import eu.stratosphere.pact.test.iterative.nephele.customdanglingpagerank.types.VertexWithRankAndDangling;
import eu.stratosphere.pact.test.iterative.nephele.danglingpagerank.PageRankStats;
import eu.stratosphere.pact.test.iterative.nephele.danglingpagerank.PageRankStatsAggregator;
import eu.stratosphere.util.Collector;

import java.util.Iterator;
import java.util.Set;

public class CustomCompensatableDotProductCoGroup extends AbstractStub implements GenericCoGrouper<VertexWithRankAndDangling, VertexWithRank, VertexWithRankAndDangling> {

	public static final String AGGREGATOR_NAME = "pagerank.aggregator";
	
	private VertexWithRankAndDangling accumulator = new VertexWithRankAndDangling();

	private PageRankStatsAggregator aggregator;

	private long numVertices;

	private long numDanglingVertices;

	private double dampingFactor;

	private double danglingRankFactor;

	private static final double BETA = 0.85;
	
	private int workerIndex;
	
	private int currentIteration;
	
	private int failingIteration;
	
	private Set<Integer> failingWorkers;

	@Override
	public void open(Configuration parameters) throws Exception {
		workerIndex = getRuntimeContext().getIndexOfThisSubtask();
		currentIteration = getIterationRuntimeContext().getSuperstepNumber();
		
		failingIteration = ConfigUtils.asInteger("compensation.failingIteration", parameters);
		failingWorkers = ConfigUtils.asIntSet("compensation.failingWorker", parameters);
		numVertices = ConfigUtils.asLong("pageRank.numVertices", parameters);
		numDanglingVertices = ConfigUtils.asLong("pageRank.numDanglingVertices", parameters);
		
		dampingFactor = (1d - BETA) / (double) numVertices;

		aggregator = (PageRankStatsAggregator) getIterationRuntimeContext().<PageRankStats>getIterationAggregator(AGGREGATOR_NAME);
		
		if (currentIteration == 1) {
			danglingRankFactor = BETA * (double) numDanglingVertices / ((double) numVertices * (double) numVertices);
		} else {
			PageRankStats previousAggregate = getIterationRuntimeContext().getPreviousIterationAggregate(AGGREGATOR_NAME);
			danglingRankFactor = BETA * previousAggregate.danglingRank() / (double) numVertices;
		}
	}

	@Override
	public void coGroup(Iterator<VertexWithRankAndDangling> currentPageRankIterator, Iterator<VertexWithRank> partialRanks,
			Collector<VertexWithRankAndDangling> collector)
	{
		if (!currentPageRankIterator.hasNext()) {
			long missingVertex = partialRanks.next().getVertexID();
			throw new IllegalStateException("No current page rank for vertex [" + missingVertex + "]!");
		}

		VertexWithRankAndDangling currentPageRank = currentPageRankIterator.next();

		long edges = 0;
		double summedRank = 0;
		while (partialRanks.hasNext()) {
			summedRank += partialRanks.next().getRank();
			edges++;
		}

		double rank = BETA * summedRank + dampingFactor + danglingRankFactor;

		double currentRank = currentPageRank.getRank();
		boolean isDangling = currentPageRank.isDangling();

		double danglingRankToAggregate = isDangling ? rank : 0;
		long danglingVerticesToAggregate = isDangling ? 1 : 0;

		double diff = Math.abs(currentRank - rank);

		aggregator.aggregate(diff, rank, danglingRankToAggregate, danglingVerticesToAggregate, 1, edges, summedRank, 0);

		accumulator.setVertexID(currentPageRank.getVertexID());
		accumulator.setRank(rank);
		accumulator.setDangling(isDangling);

		collector.collect(accumulator);
	}

	@Override
	public void close() throws Exception {
		if (currentIteration == failingIteration && failingWorkers.contains(workerIndex)) {
			aggregator.reset();
		}
	}

	@Override
	public void combineFirst(Iterator<VertexWithRankAndDangling> records, Collector<VertexWithRankAndDangling> out) {}

	@Override
	public void combineSecond(Iterator<VertexWithRank> records, Collector<VertexWithRank> out) {}
}
