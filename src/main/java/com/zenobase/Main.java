package com.zenobase;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.cors.CorsFeature;
import io.helidon.webserver.cors.CorsPathConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.*;
import com.zenobase.common.Globals;
import com.zenobase.actions.SentryFilter;
import com.zenobase.controllers.*;
import com.zenobase.mail.Mailer;
import com.zenobase.mail.PasswordResetMailer;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.services.*;
import com.zenobase.tasks.*;
import com.zenobase.tasks.beeminder.*;
import com.zenobase.tasks.demo.*;
import com.zenobase.tasks.dropbox.*;
import com.zenobase.tasks.fitbark.*;
import com.zenobase.tasks.fitbit.*;
import com.zenobase.tasks.foursquare.*;
import com.zenobase.tasks.goodreads.*;
import com.zenobase.tasks.google.*;
import com.zenobase.tasks.hexoskin.*;
import com.zenobase.tasks.ihealth.*;
import com.zenobase.tasks.lastfm.*;
import com.zenobase.tasks.mapmyfitness.*;
import com.zenobase.tasks.netatmo.*;
import com.zenobase.tasks.oura.*;
import com.zenobase.tasks.reporter.*;
import com.zenobase.tasks.rescuetime.*;
import com.zenobase.tasks.runkeeper.*;
import com.zenobase.tasks.sleepcloud.*;
import com.zenobase.tasks.strava.*;
import com.zenobase.tasks.trakt.*;
import com.zenobase.tasks.wakatime.*;
import com.zenobase.tasks.withings.*;

public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		String overridePath = System.getProperty("config.file", "conf/application-local.yaml");
		Config config = Config.builder()
				.addSource(ConfigSources.environmentVariables())
				.addSource(ConfigSources.file(overridePath).optional())
				.addSource(ConfigSources.classpath("application.yaml"))
				.build();
		Injector injector = createInjector(config);

		Globals.put(Injector.class, injector);

		Set<String> allowedOrigins = Set.copyOf(config.get("cors.allowed.origins").asList(String.class).orElse(List.of("https://zenobase.com")));
		WebServer server = WebServer.builder()
				.config(config.get("server"))
				.addFeature(CorsFeature.builder()
						.addPath(CorsPathConfig.builder()
								.pathPattern("/{+}")
								.allowOrigins(allowedOrigins)
								.allowMethods(Set.of("GET", "POST", "PUT", "DELETE", "OPTIONS"))
								.allowHeaders(Set.of("*"))
								.exposeHeaders(Set.of("Link", "Location", "X-Command-ID", "X-Credentials"))
								.allowCredentials(true)
								.maxAge(Duration.ofSeconds(3600))
								.build())
						.build())
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

		replay(injector, config);
		enableWrites(injector);
		startScheduler(injector);
	}

	static Injector createInjector(Config config) {
		try {
			return Guice.createInjector(new ZenobaseModule(config));
		} catch (com.google.inject.CreationException e) {
			for (com.google.inject.spi.Message msg : e.getErrorMessages()) {
				logger.error("Guice: {}", msg.getMessage());
			}
			throw e;
		}
	}

	private static void replay(Injector injector, Config config) {
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

	private static void enableWrites(Injector injector) {
		Bus bus = injector.getInstance(Bus.class);
		bus.setReadOnly(false);
	}

	private static void startScheduler(Injector injector) {
		injector.getInstance(Scheduler.class).start();
	}

	static class ZenobaseModule extends AbstractModule {

		private final Config config;

		ZenobaseModule(Config config) {
			this.config = config;
		}

		@Override
		protected void configure() {
			bindConfiguration();

			Bus bus = new LocalBus();
			bus.setReadOnly(true);
			bind(Bus.class).toInstance(bus);

			bind(ClientFactory.class).to(OpenSearchClientFactory.class).in(Singleton.class);
			bind(IndexManager.class).in(Singleton.class);
			bind(BucketRepository.class).in(Singleton.class);
			bind(EventRepository.class).in(Singleton.class);
			bind(CommandDispatcher.class).in(Singleton.class);
			bind(CommandRepository.class).in(Singleton.class);
			bind(UserRepository.class).in(Singleton.class);
			bind(CommandParserRegistry.class).in(Singleton.class);
			bind(CommandHandlerRegistry.class).in(Singleton.class);
			bind(CommandReplay.class).in(Singleton.class);
			bind(Mailer.class).in(Singleton.class);
			bind(VerificationMailer.class).in(Singleton.class);
			bind(PasswordResetMailer.class).in(Singleton.class);
			bind(AuthorizationContext.class).in(Singleton.class);
			bind(TaskRepository.class).in(Singleton.class);
			bind(TaskRefresher.class).in(Singleton.class);
			bind(CredentialsRepository.class).in(Singleton.class);
			bind(AuthorizationRepository.class).in(Singleton.class);
			bind(QuotaManager.class).in(Singleton.class);
			bind(PaymentGateway.class).in(Singleton.class);
			bind(Scheduler.class).asEagerSingleton();

			if (isConfigured("foursquare")) {
				bind(FoursquareVenues.class).in(Singleton.class);
			}

			Multibinder<CommandParser> parsers = Multibinder.newSetBinder(binder(), CommandParser.class);
			parsers.addBinding().to(CreateBucketCommand.Parser.class);
			parsers.addBinding().to(DeleteBucketCommand.Parser.class);
			parsers.addBinding().to(RestoreBucketCommand.Parser.class);
			parsers.addBinding().to(UpdateBucketCommand.Parser.class);
			parsers.addBinding().to(UpdateEventCommand.Parser.class);
			parsers.addBinding().to(CreateEventCommand.Parser.class);
			parsers.addBinding().to(DeleteEventCommand.Parser.class);
			parsers.addBinding().to(CreateEventsCommand.Parser.class);
			parsers.addBinding().to(DeleteEventsCommand.Parser.class);
			parsers.addBinding().to(CreateUserCommand.Parser.class);
			parsers.addBinding().to(DeleteUserCommand.Parser.class);
			parsers.addBinding().to(ChangeUserEmailCommand.Parser.class);
			parsers.addBinding().to(SuspendUserCommand.Parser.class);
			parsers.addBinding().to(ChangeUserPasswordCommand.Parser.class);
			parsers.addBinding().to(ChangeUserVerifiedCommand.Parser.class);
			parsers.addBinding().to(ChangeQuotaCommand.Parser.class);
			parsers.addBinding().to(SpendQuotaCommand.Parser.class);
			parsers.addBinding().to(OptOutCommand.Parser.class);
			parsers.addBinding().to(OptInCommand.Parser.class);
			parsers.addBinding().to(CreateTaskCommand.Parser.class);
			parsers.addBinding().to(UpdateTaskCommand.Parser.class);
			parsers.addBinding().to(DeleteTaskCommand.Parser.class);
			parsers.addBinding().to(CreateCredentialsCommand.Parser.class);
			parsers.addBinding().to(UpdateCredentialsCommand.Parser.class);
			parsers.addBinding().to(DeleteCredentialsCommand.Parser.class);
			parsers.addBinding().to(CompoundCommand.Parser.class);
			parsers.addBinding().to(CreateAuthorizationCommand.Parser.class);
			parsers.addBinding().to(DeleteAuthorizationCommand.Parser.class);

			Multibinder<CommandHandler<?>> handlers =
					Multibinder.newSetBinder(binder(), new TypeLiteral<CommandHandler<?>>() {});
			handlers.addBinding().to(CreateBucketCommand.Handler.class);
			handlers.addBinding().to(DeleteBucketCommand.Handler.class);
			handlers.addBinding().to(RestoreBucketCommand.Handler.class);
			handlers.addBinding().to(UpdateBucketCommand.Handler.class);
			handlers.addBinding().to(UpdateEventCommand.Handler.class);
			handlers.addBinding().to(CreateEventCommand.Handler.class);
			handlers.addBinding().to(DeleteEventCommand.Handler.class);
			handlers.addBinding().to(CreateEventsCommand.Handler.class);
			handlers.addBinding().to(DeleteEventsCommand.Handler.class);
			handlers.addBinding().to(CreateUserCommand.Handler.class);
			handlers.addBinding().to(DeleteUserCommand.Handler.class);
			handlers.addBinding().to(ChangeUserEmailCommand.Handler.class);
			handlers.addBinding().to(SuspendUserCommand.Handler.class);
			handlers.addBinding().to(ChangeUserPasswordCommand.Handler.class);
			handlers.addBinding().to(ChangeUserVerifiedCommand.Handler.class);
			handlers.addBinding().to(ChangeQuotaCommand.Handler.class);
			handlers.addBinding().to(SpendQuotaCommand.Handler.class);
			handlers.addBinding().to(OptOutCommand.Handler.class);
			handlers.addBinding().to(OptInCommand.Handler.class);
			handlers.addBinding().to(CreateTaskCommand.Handler.class);
			handlers.addBinding().to(UpdateTaskCommand.Handler.class);
			handlers.addBinding().to(DeleteTaskCommand.Handler.class);
			handlers.addBinding().to(CreateCredentialsCommand.Handler.class);
			handlers.addBinding().to(UpdateCredentialsCommand.Handler.class);
			handlers.addBinding().to(DeleteCredentialsCommand.Handler.class);
			handlers.addBinding().to(CreateAuthorizationCommand.Handler.class);
			handlers.addBinding().to(DeleteAuthorizationCommand.Handler.class);

			Multibinder<CredentialsManager> credentials =
					Multibinder.newSetBinder(binder(), new TypeLiteral<CredentialsManager>() {});
			credentials.addBinding().to(DemoCredentialsManager.class);
			bindIfConfigured("fitbit", FitbitCredentialsManager.class, credentials);
			bindIfConfigured("foursquare", FoursquareCredentialsManager.class, credentials);
			bindIfConfigured("withings", WithingsCredentialsManager.class, credentials);
			bindIfConfigured("netatmo", NetatmoCredentialsManager.class, credentials);
			bindIfConfigured("runkeeper", RunkeeperCredentialsManager.class, credentials);
			bindIfConfigured("strava", StravaCredentialsManager.class, credentials);
			bindIfConfigured("mapmyfitness", MapMyFitnessCredentialsManager.class, credentials);
			bindIfConfigured("dropbox", DropboxCredentialsManager.class, credentials);
			bindIfConfigured("lastfm", LastFmCredentialsManager.class, credentials);
			bindIfConfigured("rescuetime", RescueTimeCredentialsManager.class, credentials);
			bindIfConfigured("google", GoogleCredentialsManager.class, credentials);
			bindIfConfigured("ihealth", IHealthCredentialsManager.class, credentials);
			bindIfConfigured("beeminder", BeeminderCredentialsManager.class, credentials);
			bindIfConfigured("hexoskin", HexoskinCredentialsManager.class, credentials);
			bindIfConfigured("trakt", TraktCredentialsManager.class, credentials);
			bindIfConfigured("wakatime", WakaTimeCredentialsManager.class, credentials);
			bindIfConfigured("fitbark", FitBarkCredentialsManager.class, credentials);
			bindIfConfigured("goodreads", GoodreadsCredentialsManager.class, credentials);
			bindIfConfigured("oura", OuraCredentialsManager.class, credentials);
			bind(CredentialsManagerRegistry.class).in(Singleton.class);

			Multibinder<TaskManager> tasks = Multibinder.newSetBinder(binder(), new TypeLiteral<TaskManager>() {});
			tasks.addBinding().to(DemoTaskManager.class);
			bindIfConfigured("fitbit", FitbitActivitiesTaskManager.class, tasks);
			bindIfConfigured("fitbit", FitbitBurnTaskManager.class, tasks);
			bindIfConfigured("fitbit", FitbitCardioTaskManager.class, tasks);
			bindIfConfigured("fitbit", FitbitStepsTaskManager.class, tasks);
			bindIfConfigured("fitbit", FitbitSleepTaskManager.class, tasks);
			bindIfConfigured("fitbit", FitbitWeightTaskManager.class, tasks);
			bindIfConfigured("fitbit", FitbitFoodTaskManager.class, tasks);
			bindIfConfigured("foursquare", FoursquareTaskManager.class, tasks);
			bindIfConfigured("withings", WithingsCardioTaskManager.class, tasks);
			bindIfConfigured("withings", WithingsStepsTaskManager.class, tasks);
			bindIfConfigured("withings", WithingsWeightTaskManager.class, tasks);
			bindIfConfigured("withings", WithingsSleepTaskManager.class, tasks);
			bindIfConfigured("withings", WithingsTemperatureTaskManager.class, tasks);
			bindIfConfigured("netatmo", NetatmoTaskManager.class, tasks);
			bindIfConfigured("runkeeper", RunkeeperActivitiesTaskManager.class, tasks);
			bindIfConfigured("runkeeper", RunkeeperWeightTaskManager.class, tasks);
			bindIfConfigured("strava", StravaTaskManager.class, tasks);
			bindIfConfigured("mapmyfitness", MapMyFitnessActivitiesTaskManager.class, tasks);
			bindIfConfigured("mapmyfitness", MapMyFitnessSleepTaskManager.class, tasks);
			bindIfConfigured("mapmyfitness", MapMyFitnessWeightTaskManager.class, tasks);
			bindIfConfigured("dropbox", ReporterTaskManager.class, tasks);
			bindIfConfigured("lastfm", LastFmTaskManager.class, tasks);
			bindIfConfigured("rescuetime", RescueTimeProductivityTaskManager.class, tasks);
			bindIfConfigured("google", SleepCloudTaskManager.class, tasks);
			bindIfConfigured("google", GoogleFitActivitiesTaskManager.class, tasks);
			bindIfConfigured("google", GoogleFitCardioTaskManager.class, tasks);
			bindIfConfigured("google", GoogleFitFoodTaskManager.class, tasks);
			bindIfConfigured("google", GoogleFitWeightTaskManager.class, tasks);
			bindIfConfigured("ihealth", IHealthActivitiesTaskManager.class, tasks);
			bindIfConfigured("ihealth", IHealthCardioTaskManager.class, tasks);
			bindIfConfigured("ihealth", IHealthFoodTaskManager.class, tasks);
			bindIfConfigured("ihealth", IHealthGlucoseTaskManager.class, tasks);
			bindIfConfigured("ihealth", IHealthSleepTaskManager.class, tasks);
			bindIfConfigured("ihealth", IHealthStepsTaskManager.class, tasks);
			bindIfConfigured("ihealth", IHealthWeightTaskManager.class, tasks);
			bindIfConfigured("beeminder", BeeminderTaskManager.class, tasks);
			bindIfConfigured("hexoskin", HexoskinActivitiesTaskManager.class, tasks);
			bindIfConfigured("hexoskin", HexoskinSleepTaskManager.class, tasks);
			bindIfConfigured("trakt", TraktTaskManager.class, tasks);
			bindIfConfigured("wakatime", WakaTimeTaskManager.class, tasks);
			bindIfConfigured("fitbark", FitBarkTaskManager.class, tasks);
			bindIfConfigured("goodreads", GoodreadsTaskManager.class, tasks);
			bindIfConfigured("oura", OuraSleepTaskManager.class, tasks);
			bindIfConfigured("oura", OuraStepsTaskManager.class, tasks);
			bindIfConfigured("oura", OuraReadinessTaskManager.class, tasks);
			bind(TaskManagerRegistry.class).in(Singleton.class);

			Multibinder<Job> jobs = Multibinder.newSetBinder(binder(), Job.class);
			jobs.addBinding().to(AuthorizationExpirationJob.class);
			jobs.addBinding().to(BucketRefreshJob.class);
			jobs.addBinding().to(CredentialsCleanupJob.class);
			jobs.addBinding().to(SnapshotJob.class);

			bind(SentryFilter.class).in(Singleton.class);

			bind(AccountController.class).in(Singleton.class);
			bind(BucketController.class).in(Singleton.class);
			bind(BucketListController.class).in(Singleton.class);
			bind(EventController.class).in(Singleton.class);
			bind(EventListController.class).in(Singleton.class);
			bind(TagController.class).in(Singleton.class);
			bind(PasswordResetController.class).in(Singleton.class);
			bind(JournalController.class).in(Singleton.class);
			bind(StatusController.class).in(Singleton.class);
			bind(UserController.class).in(Singleton.class);
			bind(UserListController.class).in(Singleton.class);
			bind(WhoController.class).in(Singleton.class);
			bind(TaskController.class).in(Singleton.class);
			bind(TaskListController.class).in(Singleton.class);
			bind(CredentialsController.class).in(Singleton.class);
			bind(CredentialsListController.class).in(Singleton.class);
			bind(OAuthController.class).in(Singleton.class);
			bind(AuthorizationController.class).in(Singleton.class);
			bind(AuthorizationListController.class).in(Singleton.class);
			bind(PaymentController.class).in(Singleton.class);
			bind(SnapshotController.class).in(Singleton.class);
			bind(SchedulerController.class).in(Singleton.class);
			bind(RedirectController.class).in(Singleton.class);
			bind(OpenGraphController.class).in(Singleton.class);
			bind(QuotaController.class).in(Singleton.class);
		}

		private <T> void bindIfConfigured(String prefix, Class<? extends T> type, Multibinder<T> binder) {
			if (isConfigured(prefix)) {
				binder.addBinding().to(type).in(Singleton.class);
			}
		}

		private boolean isConfigured(String key) {
			return config.get(key).exists();
		}

		private void bindConfiguration() {
			bindConfigNode(config);
		}

		private void bindConfigNode(Config node) {
			if (node.isLeaf()) {
				node.asString().ifPresent(value -> {
					String key = node.key().toString();
					try {
						bindConstant().annotatedWith(Names.named(key)).to(value);
					} catch (Exception e) {
						// ignore non-bindable values
					}
				});
			} else {
				node.asNodeList().ifPresent(children -> {
					for (Config child : children) {
						bindConfigNode(child);
					}
				});
			}
		}
	}
}
