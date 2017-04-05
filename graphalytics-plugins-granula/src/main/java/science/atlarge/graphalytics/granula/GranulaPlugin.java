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
package science.atlarge.graphalytics.granula;

import nl.tudelft.granula.archiver.GranulaExecutor;
import nl.tudelft.granula.modeller.entity.Execution;
import nl.tudelft.granula.modeller.job.JobModel;
import nl.tudelft.granula.modeller.platform.PlatformModel;
import nl.tudelft.granula.util.FileUtil;
import nl.tudelft.granula.util.json.JsonUtil;
import science.atlarge.graphalytics.configuration.GraphalyticsLoaderException;
import science.atlarge.graphalytics.domain.benchmark.Benchmark;
import science.atlarge.graphalytics.domain.benchmark.BenchmarkRun;
import science.atlarge.graphalytics.report.result.BenchmarkResult;
import science.atlarge.graphalytics.report.result.BenchmarkSuiteResult;
import science.atlarge.graphalytics.plugin.Plugin;
import science.atlarge.graphalytics.report.BenchmarkReportGenerator;
import science.atlarge.graphalytics.report.BenchmarkReportWriter;
import science.atlarge.graphalytics.report.html.HtmlBenchmarkReportGenerator;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class GranulaPlugin implements Plugin {

	private static final Logger LOG = LogManager.getLogger();

	private static final String ARC_DIR = "archive";
	private static final String LOG_DIR = "log";

	private final GranulaAwarePlatform platform;
	private final BenchmarkReportWriter reportWriter;

	private static final String GRANULA_ENABLED = "benchmark.run.granula.enabled";
	private static final String PLATFORM_LOGGING_ENABLED = "benchmark.run.granula.platform-logging";
	private static final String ENVIRONMENT_LOGGING_ENABLED = "benchmark.run.granula.environment-logging";
	private static final String ARCHIVING_ENABLED = "benchmark.run.granula.archiving";

	public static boolean enabled;
	public static boolean platformLogEnabled;
	public static boolean envLogEnabled;
	public static boolean archivingEnabled;

	public GranulaPlugin(GranulaAwarePlatform platform, BenchmarkReportWriter reportWriter) {
		this.platform = platform;
		this.reportWriter = reportWriter;
		loadConfiguration();
	}

	@Override
	public String getPluginName() {
		return "granula";
	}

	@Override
	public String getPluginDescription() {
		return "Granula: Fine Grained Performance Analysis for Big Data Processing Systems";
	}

	@Override
	public void preBenchmarkSuite(Benchmark benchmark) {
		// No operation
	}

	@Override
	public void preBenchmark(BenchmarkRun benchmarkRun) {
		if(enabled) {
			LOG.debug("Start preBenchmark in Granula");
			if(platformLogEnabled) {
				preserveExecutionLog(platform, benchmarkRun, benchmarkRun.getLogDir());
//				platform.preBenchmark(benchmark, getLogDirectory(benchmarkRun));
			}
		}
	}

	@Override
	public void prepare(BenchmarkRun benchmarkRun) {

	}

	@Override
	public void cleanup(BenchmarkRun benchmarkRun, BenchmarkResult benchmarkResult) {

	}

	@Override
	public void postBenchmark(BenchmarkRun benchmarkRun, BenchmarkResult benchmarkResult) {
		if (enabled) {
			LOG.debug("Start postBenchmark in Granula");
			if (platformLogEnabled) {
//				platform.postBenchmark(benchmark, getLogDirectory(benchmarkRun));
			}
			if (archivingEnabled) {
				try {
					createArchive(benchmarkResult);

					platform.enrichMetrics(benchmarkResult, getArchiveDirectory(benchmarkRun));
				} catch (Exception ex) {
					LOG.error("Failed to generate Granula archives for the benchmark results:", ex);
				}
			}
		}
	}


	@Override
	public void postBenchmarkSuite(Benchmark benchmark, BenchmarkSuiteResult benchmarkSuiteResult) {
	}


	@Override
	public void preReportGeneration(BenchmarkReportGenerator reportGenerator) {
		if (enabled) {
			if (archivingEnabled) {
				if (reportGenerator instanceof HtmlBenchmarkReportGenerator) {
					HtmlBenchmarkReportGenerator htmlReportGenerator = (HtmlBenchmarkReportGenerator)reportGenerator;
					htmlReportGenerator.registerPlugin(new GranulaHtmlGenerator());
				}
			}
		}
	}

	@Override
	public void shutdown() {

	}


	private void loadConfiguration() {
		// Load Granula configuration
		PropertiesConfiguration config;
		try {
			config = new PropertiesConfiguration("granula.properties");
			enabled = config.getBoolean(GRANULA_ENABLED, false);
			platformLogEnabled = config.getBoolean(PLATFORM_LOGGING_ENABLED, false);
			envLogEnabled = config.getBoolean(ENVIRONMENT_LOGGING_ENABLED, false);
			archivingEnabled = config.getBoolean(ARCHIVING_ENABLED, false);

			if(enabled) {
				LOG.info("Granula plugin is found, and is enabled.");
				LOG.info(String.format(" - Logging is %s for Granula.", (platformLogEnabled) ? "enabled" : "disabled"));
				LOG.info(String.format(" - Archiving is %s for Granula.", (archivingEnabled) ? "enabled" : "disabled"));
			} else {
				LOG.info("Granula plugin is found, but is disabled.");
			}

			if (archivingEnabled && !platformLogEnabled) {
				LOG.error(String.format("The archiving feature (%s) is not usable while logging feature (%s) is not enabled. " +
						"Turning off the archiving feature of Granula. ", ARCHIVING_ENABLED, PLATFORM_LOGGING_ENABLED));
				enabled = false;
			}
		} catch (ConfigurationException e) {
			LOG.info("Could not find or load granula.properties.");
		}
	}


	public void preserveExecutionLog(GranulaAwarePlatform platform, BenchmarkRun benchmarkRun, Path benchmarkLogDir) {
		Path backupPath = benchmarkLogDir.resolve("execution");
		backupPath.toFile().mkdirs();

		Path backFile = backupPath.resolve("execution-log.js");

		Execution execution = new Execution();
		execution.setPlatform(platform.getPlatformName());
		execution.setAlgorithm(benchmarkRun.getAlgorithm().getName());
		execution.setDataset(benchmarkRun.getFormattedGraph().getName());
		execution.setJobId(benchmarkRun.getId());
		execution.setLogPath(benchmarkLogDir.toAbsolutePath().toString());
		execution.setStartTime(System.currentTimeMillis());

		FileUtil.writeFile(JsonUtil.toJson(execution), backFile);
	}

	private void readArchive(BenchmarkResult benchmarkResult) {
		Path arcPath = getArchiveDirectory(benchmarkResult.getBenchmarkRun());


	}

	private void createArchive(BenchmarkResult benchmarkResult) {
		Path logPath = benchmarkResult.getBenchmarkRun().getLogDir();
		Path arcPath = getArchiveDirectory(benchmarkResult.getBenchmarkRun());

		Path driverLogPath = logPath.resolve("execution").resolve("execution-log.js");
		Execution execution = (Execution) JsonUtil.fromJson(FileUtil.readFile(driverLogPath), Execution.class);

		try {
			Files.createDirectories(logPath.resolve("platform"));
			Files.createDirectories(logPath.resolve("environment"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		execution.setStartTime(benchmarkResult.getStartOfBenchmark().getTime());
		execution.setEndTime(benchmarkResult.getEndOfBenchmark().getTime());
		execution.setArcPath(arcPath.toAbsolutePath().toString());
		JobModel jobModel = new JobModel(getPlatformModelByMagic(execution.getPlatform()));

		GranulaExecutor granulaExecutor = new GranulaExecutor();
		granulaExecutor.setExecution(execution);
		granulaExecutor.setEnvEnabled(envLogEnabled);
		granulaExecutor.buildJobArchive(jobModel);
	}


	public static PlatformModel getPlatformModelByMagic(String platformName) {

//		InputStream platformFileStream = Graphalytics.class.getResourceAsStream("/" + platformName + ".model");
//		if (platformFileStream == null) {
//			throw new GraphalyticsLoaderException("Missing resource \"" + platformName + ".model\".");
//		}
//
//		String modelClassName;
//		try (Scanner platformScanner = new Scanner("")) {
//			String line = null;
//			if (!platformScanner.hasNext()) {
//				throw new GraphalyticsLoaderException("Expected a single line with a class name in \"" + platformName +
//						".model\", got an empty file.");
//			}
//			line = platformScanner.next();
//			while(line.trim().equals("")) {
//				line = platformScanner.next();
//			}
//			modelClassName = line;
//		}

		Map<String, String> platformNames = new HashMap<>();
		platformNames.put("giraph", "Giraph");
		platformNames.put("graphx", "Graphx");
		platformNames.put("powergraph", "Powergraph");
		platformNames.put("openg", "Openg");
		platformNames.put("graphmat", "Graphmat");
		platformNames.put("pgxd", "Pgxd");
		platformNames.put("reference", "Reference");

		String modelClassName = String.format("nl.tudelft.granula.modeller.platform.%s",platformNames.get(platformName));

		Class<? extends PlatformModel> modelClass;
		try {
			Class<?> modelClassUncasted = Class.forName(modelClassName);
			modelClass = modelClassUncasted.asSubclass(PlatformModel.class);
		} catch (ClassNotFoundException e) {
			throw new GraphalyticsLoaderException("Could not find class \"" + modelClassName + "\".", e);
		}

		PlatformModel platformModel = null;

		try {
			platformModel = modelClass.newInstance();
		} catch (Exception e) {
			throw new GraphalyticsLoaderException("Could not load class \"" + modelClassName + "\".", e);
		}

		return platformModel;
	}

	public static PlatformModel getPlatformModel(String platformName) {

		InputStream platformFileStream = GranulaPlugin.class.getResourceAsStream("/" + platformName + ".model");
		if (platformFileStream == null) {
			throw new GraphalyticsLoaderException("Missing resource \"" + platformName + ".model\".");
		}

		String modelClassName;
		try (Scanner platformScanner = new Scanner(platformFileStream)) {
			String line = null;
			if (!platformScanner.hasNext()) {
				throw new GraphalyticsLoaderException("Expected a single line with a class name in \"" + platformName +
						".model\", got an empty file.");
			}
			line = platformScanner.next();
			while(line.trim().equals("")) {
				line = platformScanner.next();
			}
			modelClassName = line;
		}

		Class<? extends PlatformModel> modelClass;
		try {
			Class<?> modelClassUncasted = Class.forName(modelClassName);
			modelClass = modelClassUncasted.asSubclass(PlatformModel.class);
		} catch (ClassNotFoundException e) {
			throw new GraphalyticsLoaderException("Could not find class \"" + modelClassName + "\".", e);
		}

		PlatformModel platformModel = null;

		try {
			platformModel = modelClass.newInstance();
		} catch (Exception e) {
			throw new GraphalyticsLoaderException("Could not load class \"" + modelClassName + "\".", e);
		}

		return platformModel;
	}

	private Path getArchiveDirectory(BenchmarkRun benchmarkRun) {
		try {
			return reportWriter.getOrCreateReportPath().resolve(ARC_DIR).resolve(benchmarkRun.getId());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Path getLogDirectory(BenchmarkRun benchmarkRun) {
		return benchmarkRun.getLogDir();
	}


}