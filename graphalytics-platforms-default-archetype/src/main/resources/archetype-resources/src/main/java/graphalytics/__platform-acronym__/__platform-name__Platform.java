/*
 * Copyright 2015 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package science.atlarge.graphalytics.${platform-acronym};

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import science.atlarge.graphalytics.domain.algorithms.Algorithm;
import science.atlarge.graphalytics.domain.benchmark.BenchmarkRun;
import science.atlarge.graphalytics.domain.graph.FormattedGraph;
import science.atlarge.graphalytics.execution.Platform;
import science.atlarge.graphalytics.execution.PlatformExecutionException;
import science.atlarge.graphalytics.report.result.BenchmarkMetrics;
import science.atlarge.graphalytics.report.result.BenchmarkMetric;
import science.atlarge.graphalytics.${platform-acronym}.${platform-name}Loader;
import science.atlarge.graphalytics.${platform-acronym}.algorithms.bfs.BreadthFirstSearchJob;
import science.atlarge.graphalytics.${platform-acronym}.algorithms.cdlp.CommunityDetectionLPJob;
import science.atlarge.graphalytics.${platform-acronym}.algorithms.lcc.LocalClusteringCoefficientJob;
import science.atlarge.graphalytics.${platform-acronym}.algorithms.pr.PageRankJob;
import science.atlarge.graphalytics.${platform-acronym}.algorithms.sssp.SingleSourceShortestPathsJob;
import science.atlarge.graphalytics.${platform-acronym}.algorithms.wcc.WeaklyConnectedComponentsJob;
import science.atlarge.graphalytics.${platform-acronym}.${platform-name}Configuration;
import science.atlarge.graphalytics.${platform-acronym}.${platform-name}Collector;
import science.atlarge.graphalytics.${platform-acronym}.${platform-name}Collector;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ${platform-name} platform driver for the Graphalytics benchmark.
 *
 * @author ${developer-name}
 */
public class ${platform-name}Platform implements Platform {

	protected static final Logger LOG = LogManager.getLogger();

	public static final String PLATFORM_NAME = "${platform-acronym}";
	public ${platform-name}Loader loader;

	public ${platform-name}Platform() {

	}

	@Override
	public void verifySetup() throws Exception {

	}

	@Override
	public void loadGraph(FormattedGraph formattedGraph) throws Exception {
		${platform-name}Configuration platformConfig = ${platform-name}Configuration.parsePropertiesFile();
		loader = new ${platform-name}Loader(formattedGraph, platformConfig);

		LOG.info("Loading graph " + formattedGraph.getName());
		try {

			int exitCode = loader.load();
			if (exitCode != 0) {
				throw new PlatformExecutionException("${platform-name} exited with an error code: " + exitCode);
			}
		} catch (Exception e) {
			throw new PlatformExecutionException("Failed to load a ${platform-name} dataset.", e);
		}
		LOG.info("Loaded graph " + formattedGraph.getName());
	}

	@Override
	public void deleteGraph(FormattedGraph formattedGraph) throws Exception {
		LOG.info("Unloading graph " + formattedGraph.getName());
		try {

			int exitCode = loader.unload();
			if (exitCode != 0) {
				throw new PlatformExecutionException("${platform-name} exited with an error code: " + exitCode);
			}
		} catch (Exception e) {
			throw new PlatformExecutionException("Failed to unload a ${platform-name} dataset.", e);
		}
		LOG.info("Unloaded graph " + formattedGraph.getName());
	}

	@Override
	public void prepare(BenchmarkRun benchmarkRun) throws Exception {

	}

	@Override
	public void startup(BenchmarkRun benchmarkRun) throws Exception {
		Path logDir = benchmarkRun.getLogDir().resolve("platform").resolve("runner.logs");
		${platform-name}Collector.startPlatformLogging(logDir);
	}

	@Override
	public void run(BenchmarkRun benchmarkRun) throws PlatformExecutionException {

		Algorithm algorithm = benchmarkRun.getAlgorithm();
		${platform-name}Configuration platformConfig = ${platform-name}Configuration.parsePropertiesFile();
		String inputPath = ${platform-name}Loader.getLoadedPath(benchmarkRun.getFormattedGraph());
		String outputPath = benchmarkRun.getOutputDir().resolve(benchmarkRun.getName()).toAbsolutePath().toString();

		${platform-name}Job job;
		switch (algorithm) {
			case BFS:
				job = new BreadthFirstSearchJob(benchmarkRun, platformConfig, inputPath, outputPath);
				break;
			case CDLP:
				job = new CommunityDetectionLPJob(benchmarkRun, platformConfig, inputPath, outputPath);
				break;
			case LCC:
				job = new LocalClusteringCoefficientJob(benchmarkRun, platformConfig, inputPath, outputPath);
				break;
			case PR:
				job = new PageRankJob(benchmarkRun, platformConfig, inputPath, outputPath);
				break;
			case WCC:
				job = new WeaklyConnectedComponentsJob(benchmarkRun, platformConfig, inputPath, outputPath);
				break;
			case SSSP:
				job = new SingleSourceShortestPathsJob(benchmarkRun, platformConfig, inputPath, outputPath);
				break;
			default:
				throw new PlatformExecutionException("Failed to load algorithm implementation.");
		}

		LOG.info("Executing benchmark with algorithm \"{}\" on graph \"{}\".",
				benchmarkRun.getAlgorithm().getName(),
				benchmarkRun.getFormattedGraph().getName());

		try {

			int exitCode = job.execute();
			if (exitCode != 0) {
				throw new PlatformExecutionException("${platform-name} exited with an error code: " + exitCode);
			}
		} catch (Exception e) {
			throw new PlatformExecutionException("Failed to execute a ${platform-name} job.", e);
		}

		LOG.info("Executed benchmark with algorithm \"{}\" on graph \"{}\".",
				benchmarkRun.getAlgorithm().getName(),
				benchmarkRun.getFormattedGraph().getName());

	}

	@Override
	public BenchmarkMetrics finalize(BenchmarkRun benchmarkRun) throws Exception {
		${platform-name}Collector.stopPlatformLogging();

		Path logDir = benchmarkRun.getLogDir().resolve("platform");

		BenchmarkMetrics metrics = new BenchmarkMetrics();
		metrics.setProcessingTime(${platform-name}Collector.collectProcessingTime(logDir));
		return metrics;
	}

	@Override
	public void terminate(BenchmarkRun benchmarkRun) throws Exception {

	}

	@Override
	public String getPlatformName() {
		return PLATFORM_NAME;
	}
}
