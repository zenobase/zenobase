package com.zenobase.metrics;

import jakarta.inject.Inject;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.cloudwatchlogs.emf.sinks.ConsoleSink;
import software.amazon.cloudwatchlogs.emf.sinks.ISink;

/**
 * Emits JVM metrics in CloudWatch EMF format every minute. Runs on its own scheduler
 * (independent of {@link com.zenobase.jobs.Scheduler}) so metrics flow during replay,
 * which is when the JVM is under the most memory pressure.
 */
public class JvmMetricsEmfTask {

	private static final Logger logger = LoggerFactory.getLogger(JvmMetricsEmfTask.class);
	private static final String NAMESPACE = "Zenobase/Jvm";
	private static final String SERVICE = "zenobase-api";
	private static final long PERIOD_SECONDS = 60L;
	private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

	// Avoids the global EnvironmentConfigurationProvider so an unrelated MetricsLogger
	// elsewhere in the process doesn't inherit our stdout sink.
	private static final Environment ENVIRONMENT = new StdoutEmfEnvironment();

	private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
	private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
	private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
	private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "jvm-metrics-emf");
		t.setDaemon(true);
		return t;
	});

	@Inject
	public JvmMetricsEmfTask() {}

	@SuppressWarnings("FutureReturnValueIgnored")
	public void start() {
		executor.scheduleAtFixedRate(this::emit, 0L, PERIOD_SECONDS, TimeUnit.SECONDS);
	}

	public void close() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	void emit() {
		try {
			MetricsLogger metrics = newMetricsLogger();
			populate(metrics);
			metrics.flush();
		} catch (Exception e) {
			logger.warn("Failed to emit JVM metrics", e);
		}
	}

	MetricsLogger newMetricsLogger() {
		MetricsLogger metrics = new MetricsLogger(ENVIRONMENT);
		metrics.setNamespace(NAMESPACE);
		metrics.setDimensions(DimensionSet.of("Service", SERVICE));
		return metrics;
	}

	void populate(MetricsLogger metrics) {
		var heap = memoryBean.getHeapMemoryUsage();
		var nonHeap = memoryBean.getNonHeapMemoryUsage();
		long gcPauseCumulativeMs = 0L;
		for (var gc : gcBeans) {
			long t = gc.getCollectionTime();
			if (t >= 0) {
				gcPauseCumulativeMs += t;
			}
		}
		metrics.putMetric("JvmHeapUsed", (double) heap.getUsed(), Unit.BYTES);
		metrics.putMetric("JvmHeapCommitted", (double) heap.getCommitted(), Unit.BYTES);
		metrics.putMetric("JvmNonHeapUsed", (double) nonHeap.getUsed(), Unit.BYTES);
		metrics.putMetric("JvmThreadsLive", threadBean.getThreadCount(), Unit.COUNT);
		metrics.putMetric("JvmGcPauseCumulativeMs", (double) gcPauseCumulativeMs, Unit.MILLISECONDS);
		metrics.putMetric("JvmUptime", (double) runtimeBean.getUptime(), Unit.MILLISECONDS);
	}

	private static final class StdoutEmfEnvironment implements Environment {

		private final ISink sink = new ConsoleSink();

		@Override
		public boolean probe() {
			return false;
		}

		@Override
		public String getName() {
			return SERVICE;
		}

		@Override
		public String getType() {
			return "Helidon";
		}

		@Override
		public String getLogGroupName() {
			return "/zenobase/api";
		}

		@Override
		public void configureContext(MetricsContext context) {}

		@Override
		public ISink getSink() {
			return sink;
		}
	}
}
