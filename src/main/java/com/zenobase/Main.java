package com.zenobase;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.google.common.base.Strings;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheckType;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.cors.CorsFeature;
import io.helidon.webserver.cors.CorsPathConfig;
import io.helidon.webserver.observe.ObserveFeature;
import io.helidon.webserver.observe.health.HealthObserver;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.common.Globals;
import com.zenobase.services.*;

public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	void main() {
		Config config = createConfig();
		Injector injector = createInjector(config);
		Globals.put(Injector.class, injector);
		AtomicBoolean ready = new AtomicBoolean(false);
		startServer(config, injector, createObserveFeature(ready, injector), createCorsFeature(config));
		replay(injector, config);
		enableWrites(injector);
		startScheduler(injector);
		ready.set(true);
	}

	private Config createConfig() {
		var overridePath = System.getProperty("config.file", "conf/application-local.yaml");
		return Config.builder()
				.addSource(ConfigSources.environmentVariables())
				.addSource(ConfigSources.file(overridePath).optional())
				.addSource(ConfigSources.classpath("application.yaml"))
				.build();
	}

	private Injector createInjector(Config config) {
		return Guice.createInjector(new Module(config));
	}

	private ObserveFeature createObserveFeature(AtomicBoolean ready, Injector injector) {
		return ObserveFeature.builder()
				.addObserver(HealthObserver.builder()
						.useSystemServices(false)
						.details(true)
						.addCheck(
								createServerCheck(injector.getInstance(Bus.class)), HealthCheckType.LIVENESS, "server")
						.addCheck(createStartupCheck(ready), HealthCheckType.STARTUP, "startup")
						.addCheck(
								createOpenSearchCheck(injector.getInstance(IndexManager.class)),
								HealthCheckType.READINESS,
								"opensearch")
						.build())
				.build();
	}

	private Supplier<HealthCheckResponse> createServerCheck(Bus bus) {
		return () -> HealthCheckResponse.builder()
				.status(true)
				.detail("read_only", bus.isReadOnly())
				.detail("scheduler_disabled", bus.isSchedulerDisabled())
				.build();
	}

	private Supplier<HealthCheckResponse> createStartupCheck(AtomicBoolean ready) {
		return () -> HealthCheckResponse.builder().status(ready.get()).build();
	}

	private Supplier<HealthCheckResponse> createOpenSearchCheck(IndexManager indexManager) {
		return () -> {
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
				config.get("cors.allowed.origins").asList(String.class).orElse(List.of("https://zenobase.com")));
		return CorsFeature.builder()
				.addPath(CorsPathConfig.builder()
						.pathPattern("/{+}")
						.allowOrigins(allowedOrigins)
						.allowMethods(Set.of("GET", "POST", "PUT", "DELETE", "OPTIONS"))
						.allowHeaders(Set.of("*"))
						.exposeHeaders(Set.of("Link", "Location", "X-Command-ID", "X-Credentials"))
						.allowCredentials(true)
						.maxAge(Duration.ofSeconds(3600))
						.build())
				.build();
	}

	private void startServer(Config config, Injector injector, ObserveFeature observeFeature, CorsFeature corsFeature) {
		var server = WebServer.builder()
				.config(config.get("server"))
				.addFeature(observeFeature)
				.addFeature(corsFeature)
				.routing(routing -> Routing.buildRouting(routing, injector))
				.build()
				.start();

		logger.info("Server started on port {}", server.port());

		io.helidon.Main.addShutdownHandler(() -> {
			logger.info("Shutting down...");
			injector.getInstance(Scheduler.class).close();
			injector.getInstance(IndexManager.class).close();
			injector.getInstance(Bus.class).close();
		});
	}

	private void replay(Injector injector, Config config) {
		var esConfig = config.get("opensearch");
		var replayHost = esConfig.get("replay").asString().orElse("");
		var rebuildHost = esConfig.get("rebuild").asString().orElse("");
		var replayConfigured = !Strings.isNullOrEmpty(replayHost);
		var rebuildConfigured = !Strings.isNullOrEmpty(rebuildHost);
		if (replayConfigured || rebuildConfigured) {
			var users = injector.getInstance(UserRepository.class);
			if (!users.isEmpty()) {
				throw new IllegalStateException(
						"Migration incomplete: replay/rebuild is configured but target domain already has data");
			}
			if (replayConfigured) {
				injector.getInstance(CommandReplay.class).replay();
			} else {
				injector.getInstance(CommandRebuild.class).rebuild();
			}
		}
	}

	private void enableWrites(Injector injector) {
		var bus = injector.getInstance(Bus.class);
		bus.setReadOnly(false);
	}

	private void startScheduler(Injector injector) {
		injector.getInstance(Scheduler.class).start();
	}
}
