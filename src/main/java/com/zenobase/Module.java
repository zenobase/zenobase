package com.zenobase;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.helidon.config.Config;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import com.zenobase.actions.SentryFilter;
import com.zenobase.commands.*;
import com.zenobase.controllers.*;
import com.zenobase.mail.ConsoleMailer;
import com.zenobase.mail.EmailValidator;
import com.zenobase.mail.Mailer;
import com.zenobase.mail.PasswordResetMailer;
import com.zenobase.mail.RegexEmailValidator;
import com.zenobase.mail.SesEmailValidator;
import com.zenobase.mail.SesMailer;
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

		if (isConfigured("aws.region")) {
			bind(SesV2Client.class).toInstance(SesV2Client.create());
			bind(Mailer.class).to(SesMailer.class).in(Singleton.class);
			bind(EmailValidator.class).to(SesEmailValidator.class).in(Singleton.class);
		} else {
			bind(Mailer.class).to(ConsoleMailer.class).in(Singleton.class);
			bind(EmailValidator.class).to(RegexEmailValidator.class).in(Singleton.class);
		}

		bind(VerificationMailer.class).in(Singleton.class);
		bind(PasswordResetMailer.class).in(Singleton.class);
		bind(AuthorizationContext.class).in(Singleton.class);
		bind(TaskRepository.class).in(Singleton.class);
		bind(TaskRefresher.class).in(Singleton.class);
		bind(CredentialsRepository.class).in(Singleton.class);
		bind(AuthorizationRepository.class).in(Singleton.class);
		bind(QuotaManager.class).in(Singleton.class);
		bind(Scheduler.class).asEagerSingleton();

		if (isConfigured("foursquare")) {
			bind(FoursquareVenues.class).in(Singleton.class);
		}
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
	}

	private void bindCredentialsManagers() {
		var credentials = Multibinder.newSetBinder(binder(), new TypeLiteral<CredentialsManager>() {});
		credentials.addBinding().to(DemoCredentialsManager.class);
		bindIfConfigured("beeminder", BeeminderCredentialsManager.class, credentials);
		bindIfConfigured("dropbox", DropboxCredentialsManager.class, credentials);
		bindIfConfigured("fitbark", FitBarkCredentialsManager.class, credentials);
		bindIfConfigured("fitbit", FitbitCredentialsManager.class, credentials);
		bindIfConfigured("foursquare", FoursquareCredentialsManager.class, credentials);
		bindIfConfigured("goodreads", GoodreadsCredentialsManager.class, credentials);
		bindIfConfigured("google", GoogleCredentialsManager.class, credentials);
		bindIfConfigured("hexoskin", HexoskinCredentialsManager.class, credentials);
		bindIfConfigured("ihealth", IHealthCredentialsManager.class, credentials);
		bindIfConfigured("lastfm", LastFmCredentialsManager.class, credentials);
		bindIfConfigured("mapmyfitness", MapMyFitnessCredentialsManager.class, credentials);
		bindIfConfigured("netatmo", NetatmoCredentialsManager.class, credentials);
		bindIfConfigured("oura", OuraCredentialsManager.class, credentials);
		bindIfConfigured("rescuetime", RescueTimeCredentialsManager.class, credentials);
		bindIfConfigured("runkeeper", RunkeeperCredentialsManager.class, credentials);
		bindIfConfigured("strava", StravaCredentialsManager.class, credentials);
		bindIfConfigured("trakt", TraktCredentialsManager.class, credentials);
		bindIfConfigured("wakatime", WakaTimeCredentialsManager.class, credentials);
		bindIfConfigured("withings", WithingsCredentialsManager.class, credentials);
		bind(CredentialsManagerRegistry.class).in(Singleton.class);
	}

	private void bindTaskManagers() {
		var tasks = Multibinder.newSetBinder(binder(), new TypeLiteral<TaskManager>() {});
		tasks.addBinding().to(DemoTaskManager.class);
		bindIfConfigured("beeminder", BeeminderTaskManager.class, tasks);
		bindIfConfigured("dropbox", ReporterTaskManager.class, tasks);
		bindIfConfigured("fitbark", FitBarkTaskManager.class, tasks);
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
		bindIfConfigured("ihealth", IHealthActivitiesTaskManager.class, tasks);
		bindIfConfigured("ihealth", IHealthCardioTaskManager.class, tasks);
		bindIfConfigured("ihealth", IHealthFoodTaskManager.class, tasks);
		bindIfConfigured("ihealth", IHealthGlucoseTaskManager.class, tasks);
		bindIfConfigured("ihealth", IHealthSleepTaskManager.class, tasks);
		bindIfConfigured("ihealth", IHealthStepsTaskManager.class, tasks);
		bindIfConfigured("ihealth", IHealthWeightTaskManager.class, tasks);
		bindIfConfigured("lastfm", LastFmTaskManager.class, tasks);
		bindIfConfigured("mapmyfitness", MapMyFitnessActivitiesTaskManager.class, tasks);
		bindIfConfigured("mapmyfitness", MapMyFitnessSleepTaskManager.class, tasks);
		bindIfConfigured("mapmyfitness", MapMyFitnessWeightTaskManager.class, tasks);
		bindIfConfigured("netatmo", NetatmoTaskManager.class, tasks);
		bindIfConfigured("oura", OuraSleepTaskManager.class, tasks);
		bindIfConfigured("oura", OuraStepsTaskManager.class, tasks);
		bindIfConfigured("oura", OuraReadinessTaskManager.class, tasks);
		bindIfConfigured("rescuetime", RescueTimeProductivityTaskManager.class, tasks);
		bindIfConfigured("runkeeper", RunkeeperActivitiesTaskManager.class, tasks);
		bindIfConfigured("runkeeper", RunkeeperWeightTaskManager.class, tasks);
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
		jobs.addBinding().to(AuthorizationExpirationJob.class);
		jobs.addBinding().to(BucketRefreshJob.class);
		jobs.addBinding().to(CredentialsCleanupJob.class);
		jobs.addBinding().to(SnapshotJob.class);
	}

	private void bindControllers() {
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
		var node = config.get(key);
		return node.isLeaf() ? node.asString().filter(s -> !s.isEmpty()).isPresent() : node.exists();
	}

	private void bindConfiguration() {
		// Core
		bindString("hostname");
		bindString("oauth.hostname");

		// OpenSearch
		bindString("opensearch.host");
		bindString("opensearch.replay");
		bindString("opensearch.rebuild");
		bindString("opensearch.rebuild_parallelism");
		bindString("opensearch.snapshot.bucket");
		bindString("opensearch.snapshot_role_arn");
		bindString("aws.region");

		// Mail
		bindString("mail.from");

		// Integration API keys (only when their prefix is configured)
		for (String prefix : List.of(
				"beeminder",
				"dropbox",
				"fitbark",
				"fitbit",
				"foursquare",
				"goodreads",
				"google",
				"hexoskin",
				"ihealth",
				"lastfm",
				"mapmyfitness",
				"netatmo",
				"oura",
				"rescuetime",
				"runkeeper",
				"strava",
				"trakt",
				"wakatime",
				"withings")) {
			if (isConfigured(prefix)) {
				bindAllLeaves(config.get(prefix));
			}
		}
	}

	private void bindString(String key) {
		config.get(key)
				.asString()
				.ifPresent(
						value -> bindConstant().annotatedWith(Names.named(key)).to(value));
	}

	private void bindAllLeaves(Config node) {
		if (node.isLeaf()) {
			bindString(node.key().toString());
		} else {
			node.asNodeList().ifPresent(children -> {
				for (Config child : children) {
					bindAllLeaves(child);
				}
			});
		}
	}
}
