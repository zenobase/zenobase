package com.zenobase;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.zenobase.auth.IdentityProvider;
import com.zenobase.auth.TokenValidator;
import com.zenobase.auth.UserStateCache;
import com.zenobase.auth.auth0.Auth0ManagementService;
import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import com.zenobase.auth.auth0.Auth0UserSynchronizer;
import com.zenobase.auth.local.LocalIdentityProvider;
import com.zenobase.commands.*;
import com.zenobase.controllers.*;
import com.zenobase.filters.ScopeFilter;
import com.zenobase.jobs.*;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.GrantedBuckets;
import com.zenobase.mcp.McpAuthFilter;
import com.zenobase.mcp.ProtectedResourceMetadataController;
import com.zenobase.mcp.resources.BucketResourceProvider;
import com.zenobase.mcp.tools.BucketsTool;
import com.zenobase.mcp.tools.EventsTool;
import com.zenobase.mcp.tools.HistogramTool;
import com.zenobase.mcp.tools.SchemaTool;
import com.zenobase.mcp.tools.StatsTool;
import com.zenobase.mcp.tools.TermsTool;
import com.zenobase.mcp.tools.TimelineTool;
import com.zenobase.metrics.JvmMetricsEmfTask;
import com.zenobase.repositories.*;
import com.zenobase.services.*;
import com.zenobase.tasks.*;
import com.zenobase.tasks.demo.DemoCredentialsManager;
import com.zenobase.tasks.demo.DemoTaskManager;
import com.zenobase.tasks.fitbit.*;
import com.zenobase.tasks.foursquare.FoursquareCredentialsManager;
import com.zenobase.tasks.foursquare.FoursquareTaskManager;
import com.zenobase.tasks.foursquare.FoursquareVenues;
import com.zenobase.tasks.goodreads.GoodreadsCredentialsManager;
import com.zenobase.tasks.goodreads.GoodreadsTaskManager;
import com.zenobase.tasks.google.*;
import com.zenobase.tasks.hexoskin.HexoskinActivitiesTaskManager;
import com.zenobase.tasks.hexoskin.HexoskinCredentialsManager;
import com.zenobase.tasks.hexoskin.HexoskinSleepTaskManager;
import com.zenobase.tasks.lastfm.LastFmCredentialsManager;
import com.zenobase.tasks.lastfm.LastFmTaskManager;
import com.zenobase.tasks.mapmyfitness.MapMyFitnessActivitiesTaskManager;
import com.zenobase.tasks.mapmyfitness.MapMyFitnessCredentialsManager;
import com.zenobase.tasks.mapmyfitness.MapMyFitnessSleepTaskManager;
import com.zenobase.tasks.mapmyfitness.MapMyFitnessWeightTaskManager;
import com.zenobase.tasks.netatmo.NetatmoCredentialsManager;
import com.zenobase.tasks.netatmo.NetatmoTaskManager;
import com.zenobase.tasks.oura.OuraCredentialsManager;
import com.zenobase.tasks.oura.OuraReadinessTaskManager;
import com.zenobase.tasks.oura.OuraSleepTaskManager;
import com.zenobase.tasks.oura.OuraStepsTaskManager;
import com.zenobase.tasks.rescuetime.RescueTimeCredentialsManager;
import com.zenobase.tasks.rescuetime.RescueTimeProductivityTaskManager;
import com.zenobase.tasks.sleepcloud.SleepCloudTaskManager;
import com.zenobase.tasks.strava.StravaCredentialsManager;
import com.zenobase.tasks.strava.StravaTaskManager;
import com.zenobase.tasks.trakt.TraktCredentialsManager;
import com.zenobase.tasks.trakt.TraktTaskManager;
import com.zenobase.tasks.wakatime.WakaTimeCredentialsManager;
import com.zenobase.tasks.wakatime.WakaTimeTaskManager;
import com.zenobase.tasks.withings.*;
import io.helidon.config.Config;
import java.util.List;

class Module extends AbstractModule {

	private final Config config;

	Module(Config config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		bindConfiguration();
		bindServices();
		bindCommandParsers();
		bindCommandHandlers();
		bindCredentialsManagers();
		bindTaskManagers();
		bindJobs();
		bindMcp();
		bindControllers();
	}

	private void bindServices() {
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

		if (isConfigured("opensearch.replay")) {
			bind(CommandReplay.class).in(Singleton.class);
		} else if (isConfigured("opensearch.rebuild")) {
			bind(CommandRebuild.class).in(Singleton.class);
		}

		var tokenValidators = Multibinder.newSetBinder(binder(), TokenValidator.class);
		if (isConfigured("auth0.domain")) {
			bind(Auth0TokenValidator.class).in(Singleton.class);
			bind(Auth0UserSynchronizer.class).in(Singleton.class);
			bind(Auth0TokenAuthorizer.class).in(Singleton.class);
			tokenValidators.addBinding().to(Auth0TokenAuthorizer.class);
		}
		if (isConfigured("auth0.m2m.client_id")) {
			bind(Auth0ManagementService.class).in(Singleton.class);
			bind(IdentityProvider.class).to(Auth0ManagementService.class).in(Singleton.class);
		} else {
			bind(IdentityProvider.class).to(LocalIdentityProvider.class).in(Singleton.class);
		}
		bind(AuthorizationContext.class).in(Singleton.class);
		bind(UserStateCache.class).in(Singleton.class);
		bind(ExternalClientRepository.class).in(Singleton.class);
		bind(TaskRepository.class).in(Singleton.class);
		bind(TaskRefresher.class).in(Singleton.class);
		bind(CredentialsRepository.class).in(Singleton.class);
		bind(QuotaManager.class).in(Singleton.class);
		bind(Scheduler.class).asEagerSingleton();
		bind(JvmMetricsEmfTask.class).in(Singleton.class);

		if (isConfigured("foursquare")) {
			bind(FoursquareVenues.class).in(Singleton.class);
		}
	}

	private void bindMcp() {
		bind(ConsentEnforcer.class).in(Singleton.class);
		bind(GrantedBuckets.class).in(Singleton.class);
		bind(BucketResourceProvider.class).in(Singleton.class);
		bind(McpAuthFilter.class).in(Singleton.class);
		bind(BucketsTool.class).in(Singleton.class);
		bind(SchemaTool.class).in(Singleton.class);
		bind(EventsTool.class).in(Singleton.class);
		bind(StatsTool.class).in(Singleton.class);
		bind(HistogramTool.class).in(Singleton.class);
		bind(TimelineTool.class).in(Singleton.class);
		bind(TermsTool.class).in(Singleton.class);
	}

	private void bindCommandParsers() {
		var parsers = Multibinder.newSetBinder(binder(), CommandParser.class);
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
		parsers.addBinding().to(ChangeUserVerifiedCommand.Parser.class);
		parsers.addBinding().to(ChangeExternalIdCommand.Parser.class);
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
		parsers.addBinding().to(CreateExternalClientCommand.Parser.class);
		parsers.addBinding().to(DeleteExternalClientCommand.Parser.class);
		parsers.addBinding().to(UpdateExternalClientGrantsCommand.Parser.class);
		parsers.addBinding().to(CompoundCommand.Parser.class);
	}

	private void bindCommandHandlers() {
		var handlers = Multibinder.newSetBinder(binder(), new TypeLiteral<CommandHandler<?>>() {});
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
		handlers.addBinding().to(ChangeUserVerifiedCommand.Handler.class);
		handlers.addBinding().to(ChangeExternalIdCommand.Handler.class);
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
		handlers.addBinding().to(CreateExternalClientCommand.Handler.class);
		handlers.addBinding().to(DeleteExternalClientCommand.Handler.class);
		handlers.addBinding().to(UpdateExternalClientGrantsCommand.Handler.class);
	}

	private void bindCredentialsManagers() {
		var credentials = Multibinder.newSetBinder(binder(), new TypeLiteral<CredentialsManager>() {});
		credentials.addBinding().to(DemoCredentialsManager.class);
		bindIfConfigured("fitbit", FitbitCredentialsManager.class, credentials);
		bindIfConfigured("foursquare", FoursquareCredentialsManager.class, credentials);
		bindIfConfigured("goodreads", GoodreadsCredentialsManager.class, credentials);
		bindIfConfigured("google", GoogleCredentialsManager.class, credentials);
		bindIfConfigured("hexoskin", HexoskinCredentialsManager.class, credentials);
		bindIfConfigured("lastfm", LastFmCredentialsManager.class, credentials);
		bindIfConfigured("mapmyfitness", MapMyFitnessCredentialsManager.class, credentials);
		bindIfConfigured("netatmo", NetatmoCredentialsManager.class, credentials);
		bindIfConfigured("oura", OuraCredentialsManager.class, credentials);
		bindIfConfigured("rescuetime", RescueTimeCredentialsManager.class, credentials);
		bindIfConfigured("strava", StravaCredentialsManager.class, credentials);
		bindIfConfigured("trakt", TraktCredentialsManager.class, credentials);
		bindIfConfigured("wakatime", WakaTimeCredentialsManager.class, credentials);
		bindIfConfigured("withings", WithingsCredentialsManager.class, credentials);
		bind(CredentialsManagerRegistry.class).in(Singleton.class);
	}

	private void bindTaskManagers() {
		var tasks = Multibinder.newSetBinder(binder(), new TypeLiteral<TaskManager>() {});
		tasks.addBinding().to(DemoTaskManager.class);
		bindIfConfigured("fitbit", FitbitActivitiesTaskManager.class, tasks);
		bindIfConfigured("fitbit", FitbitBurnTaskManager.class, tasks);
		bindIfConfigured("fitbit", FitbitCardioTaskManager.class, tasks);
		bindIfConfigured("fitbit", FitbitStepsTaskManager.class, tasks);
		bindIfConfigured("fitbit", FitbitSleepTaskManager.class, tasks);
		bindIfConfigured("fitbit", FitbitWeightTaskManager.class, tasks);
		bindIfConfigured("fitbit", FitbitFoodTaskManager.class, tasks);
		bindIfConfigured("foursquare", FoursquareTaskManager.class, tasks);
		bindIfConfigured("goodreads", GoodreadsTaskManager.class, tasks);
		bindIfConfigured("google", SleepCloudTaskManager.class, tasks);
		bindIfConfigured("google", GoogleFitActivitiesTaskManager.class, tasks);
		bindIfConfigured("google", GoogleFitCardioTaskManager.class, tasks);
		bindIfConfigured("google", GoogleFitFoodTaskManager.class, tasks);
		bindIfConfigured("google", GoogleFitWeightTaskManager.class, tasks);
		bindIfConfigured("hexoskin", HexoskinActivitiesTaskManager.class, tasks);
		bindIfConfigured("hexoskin", HexoskinSleepTaskManager.class, tasks);
		bindIfConfigured("lastfm", LastFmTaskManager.class, tasks);
		bindIfConfigured("mapmyfitness", MapMyFitnessActivitiesTaskManager.class, tasks);
		bindIfConfigured("mapmyfitness", MapMyFitnessSleepTaskManager.class, tasks);
		bindIfConfigured("mapmyfitness", MapMyFitnessWeightTaskManager.class, tasks);
		bindIfConfigured("netatmo", NetatmoTaskManager.class, tasks);
		bindIfConfigured("oura", OuraSleepTaskManager.class, tasks);
		bindIfConfigured("oura", OuraStepsTaskManager.class, tasks);
		bindIfConfigured("oura", OuraReadinessTaskManager.class, tasks);
		bindIfConfigured("rescuetime", RescueTimeProductivityTaskManager.class, tasks);
		bindIfConfigured("strava", StravaTaskManager.class, tasks);
		bindIfConfigured("trakt", TraktTaskManager.class, tasks);
		bindIfConfigured("wakatime", WakaTimeTaskManager.class, tasks);
		bindIfConfigured("withings", WithingsCardioTaskManager.class, tasks);
		bindIfConfigured("withings", WithingsStepsTaskManager.class, tasks);
		bindIfConfigured("withings", WithingsWeightTaskManager.class, tasks);
		bindIfConfigured("withings", WithingsSleepTaskManager.class, tasks);
		bindIfConfigured("withings", WithingsTemperatureTaskManager.class, tasks);
		bind(TaskManagerRegistry.class).in(Singleton.class);
	}

	private void bindJobs() {
		Multibinder<Job> jobs = Multibinder.newSetBinder(binder(), Job.class);
		jobs.addBinding().to(BucketRefreshJob.class);
		jobs.addBinding().to(CredentialsCleanupJob.class);
		jobs.addBinding().to(SnapshotJob.class);
		jobs.addBinding().to(UnverifiedEmailSuspensionJob.class);
	}

	private void bindControllers() {
		bind(ScopeFilter.class).in(Singleton.class);

		bind(AccountController.class).in(Singleton.class);
		bind(BucketController.class).in(Singleton.class);
		bind(BucketListController.class).in(Singleton.class);
		bind(EventController.class).in(Singleton.class);
		bind(EventListController.class).in(Singleton.class);
		bind(TagController.class).in(Singleton.class);
		bind(JournalController.class).in(Singleton.class);
		bind(StatusController.class).in(Singleton.class);
		bind(UserController.class).in(Singleton.class);
		bind(UserListController.class).in(Singleton.class);
		bind(WhoController.class).in(Singleton.class);
		bind(TaskController.class).in(Singleton.class);
		bind(TaskListController.class).in(Singleton.class);
		bind(CredentialsController.class).in(Singleton.class);
		bind(CredentialsListController.class).in(Singleton.class);
		bind(CredentialsCallbackController.class).in(Singleton.class);
		bind(SnapshotController.class).in(Singleton.class);
		bind(SchedulerController.class).in(Singleton.class);
		bind(RedirectController.class).in(Singleton.class);
		bind(OpenGraphController.class).in(Singleton.class);
		bind(QuotaController.class).in(Singleton.class);
		bind(ExternalClientController.class).in(Singleton.class);
		bind(ProtectedResourceMetadataController.class).in(Singleton.class);
	}

	private <T> void bindIfConfigured(String prefix, Class<? extends T> type, Multibinder<T> binder) {
		if (isConfigured(prefix)) {
			binder.addBinding().to(type).in(Singleton.class);
		}
	}

	private boolean isConfigured(String key) {
		var node = config.get(key);
		return node.isLeaf()
			? node
					.asString()
					.filter(s -> !s.isEmpty())
					.isPresent()
			: node.exists();
	}

	private void bindConfiguration() {
		// Core
		bindString("web.hostname");
		bindString("api.hostname");

		// OpenSearch
		bindString("opensearch.host");
		bindString("opensearch.replay");
		bindString("opensearch.rebuild");
		bindString("opensearch.rebuild_parallelism");
		bindString("opensearch.snapshot.bucket");
		bindString("opensearch.snapshot_role_arn");
		bindString("aws.region");

		// Auth0 / JWT
		bindString("auth0.domain");
		bindString("auth0.audience");
		bindStringOrEmpty("auth0.external_audience");
		bindString("auth0.jwks_domain");
		bindString("auth0.m2m.domain");
		bindString("auth0.m2m.client_id");
		bindString("auth0.m2m.client_secret");

		// Integration API keys (only when their prefix is configured)
		for (String prefix : List.of(
			"fitbit",
			"foursquare",
			"goodreads",
			"google",
			"hexoskin",
			"lastfm",
			"mapmyfitness",
			"netatmo",
			"oura",
			"rescuetime",
			"strava",
			"trakt",
			"wakatime",
			"withings"
		)) {
			if (isConfigured(prefix)) {
				bindAllLeaves(config.get(prefix));
			}
		}
	}

	private void bindString(String key) {
		config
			.get(key)
			.asString()
			.ifPresent(value -> bindConstant().annotatedWith(Names.named(key)).to(value));
	}

	/**
	 * Like {@link #bindString(String)} but binds an empty string when the key is absent, so consumers can rely on
	 * the {@code @Named} injection succeeding and detect "not configured" via {@code .isEmpty()}.
	 */
	private void bindStringOrEmpty(String key) {
		bindConstant().annotatedWith(Names.named(key)).to(config.get(key).asString().orElse(""));
	}

	private void bindAllLeaves(Config node) {
		if (node.isLeaf()) {
			bindString(node.key().toString());
		} else {
			node
				.asNodeList()
				.ifPresent(children -> {
					for (Config child : children) {
						bindAllLeaves(child);
					}
				});
		}
	}
}
