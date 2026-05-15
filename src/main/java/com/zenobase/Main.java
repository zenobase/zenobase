package com.zenobase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.zenobase.common.Globals;
import com.zenobase.jobs.Scheduler;
import com.zenobase.metrics.JvmMetricsEmfTask;
import com.zenobase.repositories.IndexManager;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandRebuild;
import com.zenobase.services.CommandReplay;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheckType;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.cors.CorsFeature;
import io.helidon.webserver.cors.CorsPathConfig;
import io.helidon.webserver.observe.ObserveFeature;
import io.helidon.webserver.observe.health.HealthObserver;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	void main() {
		var start = System.nanoTime();
		var config = createConfig();
		var injector = createInjector(config);
		Globals.put(Injector.class, injector);
		var ready = new AtomicBoolean(false);
		startServer(config, injector, createObserveFeature(ready, injector), createCorsFeature(config));
		startMetricsCollection(injector);
		try {
			replay(injector);
			enableWrites(injector);
			startScheduler(injector);
			ready.set(true);
		} catch (Exception e) {
			logger.error("Startup failed", e);
			System.exit(1);
		}
		logger.info("Startup took {} ms", Duration.ofNanos(System.nanoTime() - start).toMillis());
	}

	private Config createConfig() {
		var start = System.nanoTime();
		var overridePath = System.getProperty("config.file", "conf/application-local.yaml");
		var config = Config.builder()
			.addSource(ConfigSources.environmentVariables())
			.addSource(ConfigSources.file(overridePath).optional())
			.addSource(ConfigSources.classpath("application.yaml"))
			.build();
		logger.info("Config loading took {} ms", Duration.ofNanos(System.nanoTime() - start).toMillis());
		return config;
	}

	private Injector createInjector(Config config) {
		var start = System.nanoTime();
		var injector = Guice.createInjector(new Module(config));
		logger.info("Guice wiring took {} ms", Duration.ofNanos(System.nanoTime() - start).toMillis());
		return injector;
	}

	private ObserveFeature createObserveFeature(AtomicBoolean ready, Injector injector) {
		return ObserveFeature.builder()
			.addObserver(
				HealthObserver.builder()
					.useSystemServices(false)
					.details(true)
					.addCheck(createServerCheck(), HealthCheckType.LIVENESS, "server")
					.addCheck(
						createReadinessCheck(ready, injector.getInstance(IndexManager.class)),
						HealthCheckType.READINESS,
						"readiness"
					)
					.build()
			)
			.build();
	}

	private Supplier<HealthCheckResponse> createServerCheck() {
		return () -> HealthCheckResponse.builder().status(true).build();
	}

	private Supplier<HealthCheckResponse> createReadinessCheck(AtomicBoolean ready, IndexManager indexManager) {
		return () -> {
			if (!ready.get()) {
				return HealthCheckResponse.builder().status(false).build();
			}
			var health = indexManager.getCluster().getHealth();
			return HealthCheckResponse.builder()
				.status(health.status() != HealthStatus.Red)
				.detail("status", health.status().jsonValue())
				.detail("data_nodes", health.numberOfDataNodes())
				.build();
		};
	}

	private CorsFeature createCorsFeature(Config config) {
		var allowedOrigins = Set.copyOf(
			config.get("cors.allowed.origins").asList(String.class).orElse(List.of("https://zenobase.com"))
		);
		return CorsFeature.builder()
			// MCP discovery + RPC: open to any origin so browser-based MCP clients (Claude's connector flow on
			// claude.ai, etc.) can complete the OAuth handshake without a per-origin allowlist. CORS spec forbids
			// `Allow-Origin: *` together with credentials, but MCP uses bearer tokens in the Authorization header,
			// not cookies, so credentials aren't needed. `WWW-Authenticate` is exposed so clients can read the RFC
			// 9728 challenge and discover the protected-resource metadata URL.
			.addPath(
				CorsPathConfig.builder()
					.pathPattern("/mcp")
					.allowOrigins(Set.of("*"))
					.allowMethods(Set.of("GET", "POST", "OPTIONS"))
					.allowHeaders(Set.of("*"))
					.exposeHeaders(Set.of("WWW-Authenticate"))
					.allowCredentials(false)
					.maxAge(Duration.ofHours(1))
					.build()
			)
			.addPath(
				CorsPathConfig.builder()
					.pathPattern("/.well-known/oauth-protected-resource")
					.allowOrigins(Set.of("*"))
					.allowMethods(Set.of("GET", "OPTIONS"))
					.allowHeaders(Set.of("*"))
					.exposeHeaders(Set.of("WWW-Authenticate"))
					.allowCredentials(false)
					.maxAge(Duration.ofHours(1))
					.build()
			)
			.addPath(
				CorsPathConfig.builder()
					.pathPattern("/{+}")
					.allowOrigins(allowedOrigins)
					.allowMethods(Set.of("GET", "POST", "PUT", "DELETE", "OPTIONS"))
					.allowHeaders(Set.of("*"))
					.exposeHeaders(Set.of("Link", "Location", "X-Command-ID", "X-Credentials"))
					.allowCredentials(true)
					.maxAge(Duration.ofHours(1))
					.build()
			)
			.build();
	}

	private void startServer(Config config, Injector injector, ObserveFeature observeFeature, CorsFeature corsFeature) {
		var start = System.nanoTime();
		var server = WebServer.builder()
			.config(config.get("server"))
			.addFeature(observeFeature)
			.addFeature(corsFeature)
			.routing(routing -> Routing.buildRouting(routing, injector))
			.build()
			.start();

		logger.info(
			"Server started on port {} in {} ms",
			server.port(),
			Duration.ofNanos(System.nanoTime() - start).toMillis()
		);

		io.helidon.Main.addShutdownHandler(() -> {
			logger.info("Shutting down...");
			injector.getInstance(Scheduler.class).close();
			injector.getInstance(IndexManager.class).close();
			injector.getInstance(Bus.class).close();
			injector.getInstance(JvmMetricsEmfTask.class).close();
		});
	}

	private void replay(Injector injector) {
		var replayBinding = injector.getExistingBinding(Key.get(CommandReplay.class));
		var rebuildBinding = injector.getExistingBinding(Key.get(CommandRebuild.class));
		if (replayBinding == null && rebuildBinding == null) {
			return;
		}
		var users = injector.getInstance(UserRepository.class);
		if (!users.isEmpty()) {
			throw new IllegalStateException(
				"Migration incomplete: replay/rebuild is configured but target domain already has data"
			);
		}
		if (replayBinding != null) {
			replayBinding.getProvider().get().replay();
		} else if (rebuildBinding != null) {
			rebuildBinding.getProvider().get().rebuild();
		}
		injector.getInstance(IndexManager.class).flushAll();
	}

	private void enableWrites(Injector injector) {
		var bus = injector.getInstance(Bus.class);
		bus.setReadOnly(false);
	}

	private void startScheduler(Injector injector) {
		var start = System.nanoTime();
		injector.getInstance(Scheduler.class).start();
		logger.info("Scheduler started in {} ms", Duration.ofNanos(System.nanoTime() - start).toMillis());
	}

	private void startMetricsCollection(Injector injector) {
		var start = System.nanoTime();
		injector.getInstance(JvmMetricsEmfTask.class).start();
		logger.info("Metrics collection started in {} ms", Duration.ofNanos(System.nanoTime() - start).toMillis());
	}
}
