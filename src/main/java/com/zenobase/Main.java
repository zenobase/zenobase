package com.zenobase;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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

import com.zenobase.services.*;

public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	void main() {
		Config config = createConfig();
		var wiring = new Wiring(config);
		AtomicBoolean ready = new AtomicBoolean(false);
		startServer(config, wiring, createObserveFeature(ready, wiring), createCorsFeature(config));
		replay(wiring);
		enableWrites(wiring);
		startScheduler(wiring);
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

	private ObserveFeature createObserveFeature(AtomicBoolean ready, Wiring wiring) {
		return ObserveFeature.builder()
				.addObserver(HealthObserver.builder()
						.useSystemServices(false)
						.details(true)
						.addCheck(createServerCheck(), HealthCheckType.LIVENESS, "server")
						.addCheck(createStartupCheck(ready), HealthCheckType.STARTUP, "startup")
						.addCheck(createOpenSearchCheck(wiring.indexManager()), HealthCheckType.READINESS, "opensearch")
						.build())
				.build();
	}

	private Supplier<HealthCheckResponse> createServerCheck() {
		return () -> HealthCheckResponse.builder().status(true).build();
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

	private void startServer(Config config, Wiring wiring, ObserveFeature observeFeature, CorsFeature corsFeature) {
		var server = WebServer.builder()
				.config(config.get("server"))
				.addFeature(observeFeature)
				.addFeature(corsFeature)
				.routing(routing -> Routing.buildRouting(routing, wiring))
				.build()
				.start();

		logger.info("Server started on port {}", server.port());

		io.helidon.Main.addShutdownHandler(() -> {
			logger.info("Shutting down...");
			wiring.scheduler().close();
			wiring.indexManager().close();
			wiring.bus().close();
		});
	}

	private void replay(Wiring wiring) {
		var replay = wiring.commandReplay();
		var rebuild = wiring.commandRebuild();
		if (replay != null || rebuild != null) {
			if (!wiring.userRepository().isEmpty()) {
				throw new IllegalStateException(
						"Migration incomplete: replay/rebuild is configured but target domain already has data");
			}
		}
		if (replay != null) {
			replay.replay();
		} else if (rebuild != null) {
			rebuild.rebuild();
		}
	}

	private void enableWrites(Wiring wiring) {
		wiring.bus().setReadOnly(false);
	}

	private void startScheduler(Wiring wiring) {
		wiring.scheduler().start();
	}
}
