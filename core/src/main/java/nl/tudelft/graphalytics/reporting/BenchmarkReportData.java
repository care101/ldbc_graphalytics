package nl.tudelft.graphalytics.reporting;

import nl.tudelft.graphalytics.domain.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class for BenchmarkSuiteResult, with many convenient accessors for use by the templating engine.
 *
 * @author Tim Hegeman
 */
public class BenchmarkReportData {

	private final BenchmarkSuiteResult benchmarkSuiteResult;
	private final Collection<Graph> orderedGraphCollection;
	private final Collection<Algorithm> orderedAlgorithmCollection;
	/**
	 * Is unmodifiable and contains unmodifiable maps
	 */
	private final Map<Graph, Map<Algorithm, BenchmarkResult>> graphAlgorithmResults;
	/**
	 * Is unmodifiable and contains unmodifiable maps
	 */
	private final Map<Algorithm, Map<Graph, BenchmarkResult>> algorithmGraphResults;

	/**
	 * @param benchmarkSuiteResult the results of running a benchmark suite, from which data is extracted
	 */
	public BenchmarkReportData(BenchmarkSuiteResult benchmarkSuiteResult) {
		this.benchmarkSuiteResult = benchmarkSuiteResult;
		this.orderedGraphCollection = benchmarkSuiteResult.getBenchmarkSuite().getGraphs();
		this.orderedAlgorithmCollection = benchmarkSuiteResult.getBenchmarkSuite().getAlgorithms();
		this.graphAlgorithmResults = constructGraphAlgorithmResults(benchmarkSuiteResult);
		this.algorithmGraphResults = constructAlgorithmGraphResults(benchmarkSuiteResult);
	}

	private static Map<Graph, Map<Algorithm, BenchmarkResult>> constructGraphAlgorithmResults(
			BenchmarkSuiteResult benchmarkSuiteResult) {
		// Construct a map of maps to hold the results
		Map<Graph, Map<Algorithm, BenchmarkResult>> graphAlgorithmResults = new HashMap<>();
		for (Graph graph : benchmarkSuiteResult.getBenchmarkSuite().getGraphs()) {
			graphAlgorithmResults.put(graph, new HashMap<Algorithm, BenchmarkResult>());
		}

		// Insert the results from the benchmark suite
		for (BenchmarkResult benchmarkResult : benchmarkSuiteResult.getBenchmarkResults()) {
			graphAlgorithmResults.get(benchmarkResult.getBenchmark().getGraph()).put(
					benchmarkResult.getBenchmark().getAlgorithm(), benchmarkResult);
		}

		// Make the map unmodifiable
		for (Graph graph : benchmarkSuiteResult.getBenchmarkSuite().getGraphs()) {
			graphAlgorithmResults.put(graph, Collections.unmodifiableMap(graphAlgorithmResults.get(graph)));
		}
		return Collections.unmodifiableMap(graphAlgorithmResults);
	}

	private static Map<Algorithm, Map<Graph, BenchmarkResult>> constructAlgorithmGraphResults(
			BenchmarkSuiteResult benchmarkSuiteResult) {
		// Construct a map of maps to hold the results
		Map<Algorithm, Map<Graph, BenchmarkResult>> algorithmGraphResults = new HashMap<>();
		for (Algorithm algorithm : benchmarkSuiteResult.getBenchmarkSuite().getAlgorithms()) {
			algorithmGraphResults.put(algorithm, new HashMap<Graph, BenchmarkResult>());
		}

		// Insert the results from the benchmark suite
		for (BenchmarkResult benchmarkResult : benchmarkSuiteResult.getBenchmarkResults()) {
			algorithmGraphResults.get(benchmarkResult.getBenchmark().getAlgorithm()).put(
					benchmarkResult.getBenchmark().getGraph(), benchmarkResult);
		}

		// Make the map unmodifiable
		for (Algorithm algorithm : benchmarkSuiteResult.getBenchmarkSuite().getAlgorithms()) {
			algorithmGraphResults.put(algorithm, Collections.unmodifiableMap(algorithmGraphResults.get(algorithm)));
		}
		return Collections.unmodifiableMap(algorithmGraphResults);
	}

	/**
	 * @return an ordered collection of all graphs processed in the benchmark suite
	 */
	public Collection<Graph> getGraphs() {
		return orderedGraphCollection;
	}

	/**
	 * @return an ordered collection of all algorithms executed in the benchmark suite
	 */
	public Collection<Algorithm> getAlgorithms() {
		return orderedAlgorithmCollection;
	}

	/**
	 * @param graph     a graph from the benchmark suite
	 * @param algorithm an algorithm from the benchmark suite
	 * @return the execution results for executing the specified algorithm on the specified graph
	 */
	public BenchmarkResult getResults(Graph graph, Algorithm algorithm) {
		return graphAlgorithmResults.get(graph).get(algorithm);
	}

	/**
	 * @param graph a graph from the benchmark suite
	 * @return a map containing the results for executing any algorithm on the specified graph
	 */
	public Map<Algorithm, BenchmarkResult> getResults(Graph graph) {
		return graphAlgorithmResults.get(graph);
	}

	/**
	 * @param algorithm an algorithm from the benchmark suite
	 * @return a map containing the results for executing the specified algorithm on any graph
	 */
	public Map<Graph, BenchmarkResult> getResults(Algorithm algorithm) {
		return algorithmGraphResults.get(algorithm);
	}

	/**
	 * @return all benchmark results, with graph as primary key and algorithm as secondary key
	 */
	public Map<Graph, Map<Algorithm, BenchmarkResult>> getResultsPerGraph() {
		return graphAlgorithmResults;
	}

	/**
	 * @return all benchmark results, with algorithm as primary key and graph as secondary key
	 */
	public Map<Algorithm, Map<Graph, BenchmarkResult>> getResultsPerAlgorithm() {
		return algorithmGraphResults;
	}

	/**
	 * @return platform-specific configuration details
	 */
	public PlatformConfiguration getPlatformConfiguration() {
		return benchmarkSuiteResult.getPlatformConfiguration();
	}

	/**
	 * @return information about the system used to run the benchmark suite on
	 */
	public SystemDetails getSystemDetails() {
		return benchmarkSuiteResult.getSystemDetails();
	}

}
