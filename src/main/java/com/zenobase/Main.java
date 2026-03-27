package com.zenobase;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
		String overridePath = System.getProperty("config.file", "conf/application-local.yaml");
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
		IndexManager indexManager = injector.getInstance(IndexManager.class);
		return ObserveFeature.builder()
				.addObserver(HealthObserver.builder()
						.useSystemServices(false)
						.addCheck(
								() -> HealthCheckResponse.builder().status(true).build(),
								HealthCheckType.LIVENESS,
								"server")
						.addCheck(
								() -> HealthCheckResponse.builder()
										.status(ready.get())
										.build(),
								HealthCheckType.STARTUP,
								"startup")
						.addCheck(
								() -> HealthCheckResponse.builder()
										.status(indexManager
														.getCluster()
														.getHealth()
														.status()
												!= HealthStatus.Red)
										.build(),
								HealthCheckType.READINESS,
								"opensearch")
						.build())
				.build();
	}

	private CorsFeature createCorsFeature(Config config) {
		Set<String> allowedOrigins = Set.copyOf(
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
		WebServer server = WebServer.builder()
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
		Config esConfig = config.get("opensearch");
		String replayHost = esConfig.get("replay").asString().orElse("");
		String rebuildHost = esConfig.get("rebuild").asString().orElse("");
		boolean replayConfigured = !Strings.isNullOrEmpty(replayHost);
		boolean rebuildConfigured = !Strings.isNullOrEmpty(rebuildHost);
		if (replayConfigured || rebuildConfigured) {
			UserRepository users = injector.getInstance(UserRepository.class);
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
		Bus bus = injector.getInstance(Bus.class);
		bus.setReadOnly(false);
	}

	private void startScheduler(Injector injector) {
		injector.getInstance(Scheduler.class).start();
	}
}
