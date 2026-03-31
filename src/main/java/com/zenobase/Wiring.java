package com.zenobase;

import java.util.LinkedHashSet;
import java.util.Set;

import io.helidon.config.Config;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import com.zenobase.actions.GatekeeperFilter;
import com.zenobase.actions.QuotaExceptionFilter;
import com.zenobase.actions.SentryFilter;
import com.zenobase.commands.*;
import com.zenobase.controllers.*;
import com.zenobase.mail.*;
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

class Wiring {

	// Services
	private final Bus bus;
	private final IndexManager indexManager;
	private final UserRepository userRepository;
	private final BucketRepository bucketRepository;
	private final EventRepository eventRepository;
	private final CommandDispatcher commandDispatcher;
	private final Scheduler scheduler;
	private final @Nullable CommandReplay commandReplay;
	private final @Nullable CommandRebuild commandRebuild;

	// Filters
	private final SentryFilter sentryFilter;
	private final GatekeeperFilter gatekeeperFilter;
	private final QuotaExceptionFilter quotaExceptionFilter;

	// Controllers
	private final StatusController statusController;
	private final WhoController whoController;
	private final PasswordResetController passwordResetController;
	private final QuotaController quotaController;
	private final UserListController userListController;
	private final UserController userController;
	private final AccountController accountController;
	private final BucketListController bucketListController;
	private final BucketController bucketController;
	private final EventListController eventListController;
	private final EventController eventController;
	private final TagController tagController;
	private final JournalController journalController;
	private final CredentialsListController credentialsListController;
	private final CredentialsController credentialsController;
	private final TaskListController taskListController;
	private final TaskController taskController;
	private final OAuthController oauthController;
	private final AuthorizationListController authorizationListController;
	private final AuthorizationController authorizationController;
	private final SnapshotController snapshotController;
	private final SchedulerController schedulerController;
	private final RedirectController redirectController;
	private final OpenGraphController openGraphController;

	Wiring(Config config) {
		// Bus
		bus = new LocalBus();
		bus.setReadOnly(true);

		// Core services
		var clientFactory = new OpenSearchClientFactory(
				configString(config, "opensearch.host"), configString(config, "aws.region"));
		indexManager = new IndexManager(
				clientFactory,
				configString(config, "opensearch.snapshot.bucket"),
				configString(config, "aws.region"),
				configString(config, "opensearch.snapshot_role_arn"));
		userRepository = new UserRepository(indexManager);
		bucketRepository = new BucketRepository(indexManager);
		eventRepository = new EventRepository(indexManager);
		var taskRepository = new TaskRepository(indexManager);
		var credentialsRepository = new CredentialsRepository(indexManager);
		var authorizationRepository = new AuthorizationRepository(indexManager);

		// Command parsers
		var parserRegistry = new CommandParserRegistry(Set.of(
				new CreateBucketCommand.Parser(),
				new DeleteBucketCommand.Parser(),
				new RestoreBucketCommand.Parser(),
				new UpdateBucketCommand.Parser(),
				new UpdateEventCommand.Parser(),
				new CreateEventCommand.Parser(),
				new DeleteEventCommand.Parser(),
				new CreateEventsCommand.Parser(),
				new DeleteEventsCommand.Parser(),
				new CreateUserCommand.Parser(),
				new DeleteUserCommand.Parser(),
				new ChangeUserEmailCommand.Parser(),
				new SuspendUserCommand.Parser(),
				new ChangeUserPasswordCommand.Parser(),
				new ChangeUserVerifiedCommand.Parser(),
				new ChangeQuotaCommand.Parser(),
				new SpendQuotaCommand.Parser(),
				new OptOutCommand.Parser(),
				new OptInCommand.Parser(),
				new CreateTaskCommand.Parser(),
				new UpdateTaskCommand.Parser(),
				new DeleteTaskCommand.Parser(),
				new CreateCredentialsCommand.Parser(),
				new UpdateCredentialsCommand.Parser(),
				new DeleteCredentialsCommand.Parser(),
				new CompoundCommand.Parser(),
				new CreateAuthorizationCommand.Parser(),
				new DeleteAuthorizationCommand.Parser()));

		// Command handlers
		var handlerRegistry = new CommandHandlerRegistry(Set.of(
				new CreateBucketCommand.Handler(bucketRepository),
				new DeleteBucketCommand.Handler(bucketRepository),
				new RestoreBucketCommand.Handler(bucketRepository),
				new UpdateBucketCommand.Handler(bucketRepository),
				new UpdateEventCommand.Handler(eventRepository),
				new CreateEventCommand.Handler(eventRepository),
				new DeleteEventCommand.Handler(eventRepository),
				new CreateEventsCommand.Handler(eventRepository),
				new DeleteEventsCommand.Handler(eventRepository),
				new CreateUserCommand.Handler(userRepository),
				new DeleteUserCommand.Handler(userRepository),
				new ChangeUserEmailCommand.Handler(userRepository),
				new SuspendUserCommand.Handler(userRepository),
				new ChangeUserPasswordCommand.Handler(userRepository),
				new ChangeUserVerifiedCommand.Handler(userRepository),
				new ChangeQuotaCommand.Handler(userRepository),
				new SpendQuotaCommand.Handler(),
				new OptOutCommand.Handler(userRepository),
				new OptInCommand.Handler(userRepository),
				new CreateTaskCommand.Handler(taskRepository),
				new UpdateTaskCommand.Handler(taskRepository),
				new DeleteTaskCommand.Handler(taskRepository),
				new CreateCredentialsCommand.Handler(credentialsRepository),
				new UpdateCredentialsCommand.Handler(credentialsRepository),
				new DeleteCredentialsCommand.Handler(credentialsRepository),
				new CreateAuthorizationCommand.Handler(authorizationRepository),
				new DeleteAuthorizationCommand.Handler(authorizationRepository)));

		// Command infrastructure
		var commandRepository = new CommandRepository(indexManager, parserRegistry);
		var quotaManager = new QuotaManager(userRepository, commandRepository);
		commandDispatcher = new CommandDispatcher(handlerRegistry, commandRepository, quotaManager);

		// Replay/rebuild
		if (isConfigured(config, "opensearch.replay")) {
			commandReplay =
					new CommandReplay(configString(config, "opensearch.replay"), parserRegistry, commandDispatcher);
			commandRebuild = null;
		} else if (isConfigured(config, "opensearch.rebuild")) {
			commandReplay = null;
			commandRebuild = new CommandRebuild(
					configString(config, "opensearch.rebuild"),
					Integer.parseInt(configString(config, "opensearch.rebuild_parallelism")),
					commandDispatcher,
					userRepository,
					authorizationRepository,
					credentialsRepository,
					bucketRepository,
					taskRepository);
		} else {
			commandReplay = null;
			commandRebuild = null;
		}

		// Mail
		Mailer mailer;
		EmailValidator emailValidator;
		if (isConfigured(config, "aws.region")) {
			var sesClient = SesV2Client.create();
			mailer = new SesMailer(sesClient, configString(config, "mail.from"));
			emailValidator = new SesEmailValidator(sesClient);
		} else {
			mailer = new ConsoleMailer(configString(config, "mail.from"));
			emailValidator = new RegexEmailValidator();
		}
		var hostname = configString(config, "hostname");
		var verificationMailer = new VerificationMailer(mailer, hostname);
		var passwordResetMailer = new PasswordResetMailer(mailer, hostname);

		// Credentials managers
		var oauthHostname = configString(config, "oauth.hostname");
		var credentialsManagers = new LinkedHashSet<CredentialsManager>();
		credentialsManagers.add(new DemoCredentialsManager());
		if (isConfigured(config, "beeminder")) {
			credentialsManagers.add(new BeeminderCredentialsManager(
					credentialsRepository,
					configString(config, "beeminder.api.key"),
					configString(config, "beeminder.api.secret"),
					oauthHostname));
		}
		DropboxCredentialsManager dropboxCredentials = null;
		if (isConfigured(config, "dropbox")) {
			dropboxCredentials = new DropboxCredentialsManager(
					credentialsRepository,
					configString(config, "dropbox.api.key"),
					configString(config, "dropbox.api.secret"),
					oauthHostname);
			credentialsManagers.add(dropboxCredentials);
		}
		FitBarkCredentialsManager fitbarkCredentials = null;
		if (isConfigured(config, "fitbark")) {
			fitbarkCredentials = new FitBarkCredentialsManager(
					credentialsRepository,
					configString(config, "fitbark.api.key"),
					configString(config, "fitbark.api.secret"),
					oauthHostname);
			credentialsManagers.add(fitbarkCredentials);
		}
		FitbitCredentialsManager fitbitCredentials = null;
		if (isConfigured(config, "fitbit")) {
			fitbitCredentials = new FitbitCredentialsManager(
					credentialsRepository,
					configString(config, "fitbit.api.key"),
					configString(config, "fitbit.api.secret"),
					oauthHostname);
			credentialsManagers.add(fitbitCredentials);
		}
		FoursquareCredentialsManager foursquareCredentials = null;
		if (isConfigured(config, "foursquare")) {
			foursquareCredentials = new FoursquareCredentialsManager(
					credentialsRepository,
					configString(config, "foursquare.api.key"),
					configString(config, "foursquare.api.secret"),
					oauthHostname);
			credentialsManagers.add(foursquareCredentials);
		}
		GoodreadsCredentialsManager goodreadsCredentials = null;
		if (isConfigured(config, "goodreads")) {
			goodreadsCredentials = new GoodreadsCredentialsManager(
					credentialsRepository,
					configString(config, "goodreads.api.key"),
					configString(config, "goodreads.api.secret"),
					oauthHostname);
			credentialsManagers.add(goodreadsCredentials);
		}
		GoogleCredentialsManager googleCredentials = null;
		if (isConfigured(config, "google")) {
			googleCredentials = new GoogleCredentialsManager(
					credentialsRepository,
					configString(config, "google.api.key"),
					configString(config, "google.api.secret"),
					oauthHostname);
			credentialsManagers.add(googleCredentials);
		}
		HexoskinCredentialsManager hexoskinCredentials = null;
		if (isConfigured(config, "hexoskin")) {
			hexoskinCredentials = new HexoskinCredentialsManager(
					credentialsRepository,
					configString(config, "hexoskin.api.key"),
					configString(config, "hexoskin.api.secret"),
					oauthHostname);
			credentialsManagers.add(hexoskinCredentials);
		}
		IHealthCredentialsManager ihealthCredentials = null;
		if (isConfigured(config, "ihealth")) {
			ihealthCredentials = new IHealthCredentialsManager(
					credentialsRepository,
					configString(config, "ihealth.api.key"),
					configString(config, "ihealth.api.secret"),
					configString(config, "ihealth.api.sc"),
					oauthHostname);
			credentialsManagers.add(ihealthCredentials);
		}
		LastFmCredentialsManager lastfmCredentials = null;
		if (isConfigured(config, "lastfm")) {
			lastfmCredentials = new LastFmCredentialsManager(
					credentialsRepository,
					configString(config, "lastfm.api.key"),
					configString(config, "lastfm.api.secret"),
					oauthHostname);
			credentialsManagers.add(lastfmCredentials);
		}
		MapMyFitnessCredentialsManager mapmyfitnessCredentials = null;
		if (isConfigured(config, "mapmyfitness")) {
			mapmyfitnessCredentials = new MapMyFitnessCredentialsManager(
					credentialsRepository,
					configString(config, "mapmyfitness.api.key"),
					configString(config, "mapmyfitness.api.secret"),
					oauthHostname);
			credentialsManagers.add(mapmyfitnessCredentials);
		}
		NetatmoCredentialsManager netatmoCredentials = null;
		if (isConfigured(config, "netatmo")) {
			netatmoCredentials = new NetatmoCredentialsManager(
					credentialsRepository,
					configString(config, "netatmo.api.key"),
					configString(config, "netatmo.api.secret"),
					oauthHostname);
			credentialsManagers.add(netatmoCredentials);
		}
		OuraCredentialsManager ouraCredentials = null;
		if (isConfigured(config, "oura")) {
			ouraCredentials = new OuraCredentialsManager(
					credentialsRepository,
					configString(config, "oura.api.key"),
					configString(config, "oura.api.secret"),
					oauthHostname);
			credentialsManagers.add(ouraCredentials);
		}
		RescueTimeCredentialsManager rescuetimeCredentials = null;
		if (isConfigured(config, "rescuetime")) {
			rescuetimeCredentials = new RescueTimeCredentialsManager(
					credentialsRepository,
					configString(config, "rescuetime.api.key"),
					configString(config, "rescuetime.api.secret"),
					oauthHostname);
			credentialsManagers.add(rescuetimeCredentials);
		}
		RunkeeperCredentialsManager runkeeperCredentials = null;
		if (isConfigured(config, "runkeeper")) {
			runkeeperCredentials = new RunkeeperCredentialsManager(
					credentialsRepository,
					configString(config, "runkeeper.api.key"),
					configString(config, "runkeeper.api.secret"),
					oauthHostname);
			credentialsManagers.add(runkeeperCredentials);
		}
		StravaCredentialsManager stravaCredentials = null;
		if (isConfigured(config, "strava")) {
			stravaCredentials = new StravaCredentialsManager(
					credentialsRepository,
					configString(config, "strava.api.key"),
					configString(config, "strava.api.secret"),
					oauthHostname);
			credentialsManagers.add(stravaCredentials);
		}
		TraktCredentialsManager traktCredentials = null;
		if (isConfigured(config, "trakt")) {
			traktCredentials = new TraktCredentialsManager(
					credentialsRepository,
					configString(config, "trakt.api.key"),
					configString(config, "trakt.api.secret"),
					oauthHostname);
			credentialsManagers.add(traktCredentials);
		}
		WakaTimeCredentialsManager wakatimeCredentials = null;
		if (isConfigured(config, "wakatime")) {
			wakatimeCredentials = new WakaTimeCredentialsManager(
					credentialsRepository,
					configString(config, "wakatime.api.key"),
					configString(config, "wakatime.api.secret"),
					oauthHostname);
			credentialsManagers.add(wakatimeCredentials);
		}
		WithingsCredentialsManager withingsCredentials = null;
		if (isConfigured(config, "withings")) {
			withingsCredentials = new WithingsCredentialsManager(
					credentialsRepository,
					configString(config, "withings.api.key"),
					configString(config, "withings.api.secret"),
					oauthHostname);
			credentialsManagers.add(withingsCredentials);
		}
		var credentialsManagerRegistry = new CredentialsManagerRegistry(credentialsManagers);

		// Task managers
		var taskManagers = new LinkedHashSet<TaskManager>();
		taskManagers.add(new DemoTaskManager());
		if (isConfigured(config, "beeminder")) {
			var beeminderCredentials = (BeeminderCredentialsManager) credentialsManagers.stream()
					.filter(m -> m instanceof BeeminderCredentialsManager)
					.findFirst()
					.orElseThrow();
			taskManagers.add(new BeeminderTaskManager(beeminderCredentials, eventRepository));
		}
		if (dropboxCredentials != null) {
			taskManagers.add(new ReporterTaskManager(dropboxCredentials));
		}
		if (fitbarkCredentials != null) {
			taskManagers.add(new FitBarkTaskManager(fitbarkCredentials));
		}
		if (fitbitCredentials != null) {
			taskManagers.add(new FitbitActivitiesTaskManager(fitbitCredentials));
			taskManagers.add(new FitbitBurnTaskManager(fitbitCredentials));
			taskManagers.add(new FitbitCardioTaskManager(fitbitCredentials));
			taskManagers.add(new FitbitStepsTaskManager(fitbitCredentials));
			taskManagers.add(new FitbitSleepTaskManager(fitbitCredentials));
			taskManagers.add(new FitbitWeightTaskManager(fitbitCredentials));
			taskManagers.add(new FitbitFoodTaskManager(fitbitCredentials));
		}
		if (foursquareCredentials != null) {
			taskManagers.add(new FoursquareTaskManager(foursquareCredentials));
		}
		if (goodreadsCredentials != null) {
			taskManagers.add(new GoodreadsTaskManager(goodreadsCredentials));
		}
		if (googleCredentials != null) {
			taskManagers.add(new SleepCloudTaskManager(googleCredentials));
			taskManagers.add(new GoogleFitActivitiesTaskManager(googleCredentials));
			taskManagers.add(new GoogleFitCardioTaskManager(googleCredentials));
			taskManagers.add(new GoogleFitFoodTaskManager(googleCredentials));
			taskManagers.add(new GoogleFitWeightTaskManager(googleCredentials));
		}
		if (hexoskinCredentials != null) {
			taskManagers.add(new HexoskinActivitiesTaskManager(hexoskinCredentials));
			taskManagers.add(new HexoskinSleepTaskManager(hexoskinCredentials));
		}
		if (ihealthCredentials != null) {
			taskManagers.add(
					new IHealthActivitiesTaskManager(ihealthCredentials, configString(config, "ihealth.api.sv.sport")));
			taskManagers.add(new IHealthCardioTaskManager(
					ihealthCredentials,
					configString(config, "ihealth.api.sv.bp"),
					configString(config, "ihealth.api.sv.spo2")));
			taskManagers.add(
					new IHealthFoodTaskManager(ihealthCredentials, configString(config, "ihealth.api.sv.food")));
			taskManagers.add(
					new IHealthGlucoseTaskManager(ihealthCredentials, configString(config, "ihealth.api.sv.glucose")));
			taskManagers.add(
					new IHealthSleepTaskManager(ihealthCredentials, configString(config, "ihealth.api.sv.sleep")));
			taskManagers.add(
					new IHealthStepsTaskManager(ihealthCredentials, configString(config, "ihealth.api.sv.activity")));
			taskManagers.add(
					new IHealthWeightTaskManager(ihealthCredentials, configString(config, "ihealth.api.sv.weight")));
		}
		if (lastfmCredentials != null) {
			taskManagers.add(new LastFmTaskManager(lastfmCredentials));
		}
		if (mapmyfitnessCredentials != null) {
			taskManagers.add(new MapMyFitnessActivitiesTaskManager(mapmyfitnessCredentials));
			taskManagers.add(new MapMyFitnessSleepTaskManager(mapmyfitnessCredentials));
			taskManagers.add(new MapMyFitnessWeightTaskManager(mapmyfitnessCredentials));
		}
		if (netatmoCredentials != null) {
			taskManagers.add(new NetatmoTaskManager(netatmoCredentials));
		}
		if (ouraCredentials != null) {
			taskManagers.add(new OuraSleepTaskManager(ouraCredentials));
			taskManagers.add(new OuraStepsTaskManager(ouraCredentials));
			taskManagers.add(new OuraReadinessTaskManager(ouraCredentials));
		}
		if (rescuetimeCredentials != null) {
			taskManagers.add(new RescueTimeProductivityTaskManager(rescuetimeCredentials));
		}
		if (runkeeperCredentials != null) {
			taskManagers.add(new RunkeeperActivitiesTaskManager(runkeeperCredentials));
			taskManagers.add(new RunkeeperWeightTaskManager(runkeeperCredentials));
		}
		if (stravaCredentials != null) {
			taskManagers.add(new StravaTaskManager(stravaCredentials));
		}
		if (traktCredentials != null) {
			taskManagers.add(new TraktTaskManager(traktCredentials));
		}
		if (wakatimeCredentials != null) {
			taskManagers.add(new WakaTimeTaskManager(wakatimeCredentials));
		}
		if (withingsCredentials != null) {
			taskManagers.add(new WithingsCardioTaskManager(withingsCredentials));
			taskManagers.add(new WithingsStepsTaskManager(withingsCredentials));
			taskManagers.add(new WithingsWeightTaskManager(withingsCredentials));
			taskManagers.add(new WithingsSleepTaskManager(withingsCredentials));
			taskManagers.add(new WithingsTemperatureTaskManager(withingsCredentials));
		}
		var taskManagerRegistry = new TaskManagerRegistry(taskManagers);

		// Task refresher
		var taskRefresher = new TaskRefresher(taskManagerRegistry, bucketRepository, commandDispatcher);

		// Jobs
		var jobs = Set.of(
				new AuthorizationExpirationJob(authorizationRepository, commandDispatcher),
				new BucketRefreshJob(bucketRepository, userRepository, taskRepository, taskRefresher),
				new CredentialsCleanupJob(credentialsRepository, commandDispatcher),
				new SnapshotJob(indexManager));
		scheduler = new Scheduler(bus, jobs);

		// Auth
		var authorizationContext = new AuthorizationContext(authorizationRepository);

		// Filters
		sentryFilter = new SentryFilter(authorizationContext);
		gatekeeperFilter = new GatekeeperFilter(bus, userRepository, authorizationContext);
		quotaExceptionFilter = new QuotaExceptionFilter();

		// Controllers
		statusController = new StatusController(authorizationContext, userRepository, bus);
		whoController = new WhoController(authorizationContext, userRepository);
		passwordResetController =
				new PasswordResetController(authorizationContext, userRepository, passwordResetMailer);
		quotaController = new QuotaController(authorizationContext, userRepository, quotaManager, commandDispatcher);
		userListController = new UserListController(authorizationContext, userRepository);
		userController = new UserController(
				authorizationContext,
				userRepository,
				authorizationRepository,
				commandDispatcher,
				verificationMailer,
				emailValidator);
		accountController = new AccountController(
				authorizationContext,
				userRepository,
				bucketRepository,
				taskRepository,
				credentialsRepository,
				authorizationRepository,
				commandDispatcher,
				emailValidator,
				verificationMailer);
		bucketListController = new BucketListController(
				authorizationContext, commandDispatcher, bucketRepository, eventRepository, userRepository);
		bucketController = new BucketController(
				authorizationContext,
				commandDispatcher,
				bucketRepository,
				userRepository,
				authorizationRepository,
				taskRepository);
		eventListController = new EventListController(
				authorizationContext, bucketRepository, eventRepository, userRepository, commandDispatcher);
		eventController =
				new EventController(authorizationContext, bucketRepository, eventRepository, commandDispatcher);
		tagController = new TagController(authorizationContext, bucketRepository, eventRepository);
		journalController =
				new JournalController(authorizationContext, commandDispatcher, commandRepository, userRepository);
		credentialsListController = new CredentialsListController(
				authorizationContext,
				commandDispatcher,
				credentialsManagerRegistry,
				credentialsRepository,
				userRepository);
		credentialsController = new CredentialsController(
				authorizationContext,
				commandDispatcher,
				credentialsManagerRegistry,
				credentialsRepository,
				userRepository);
		taskListController = new TaskListController(
				authorizationContext,
				commandDispatcher,
				taskManagerRegistry,
				taskRepository,
				bucketRepository,
				userRepository);
		taskController = new TaskController(
				authorizationContext,
				commandDispatcher,
				taskManagerRegistry,
				taskRepository,
				bucketRepository,
				userRepository,
				taskRefresher,
				bus);
		oauthController =
				new OAuthController(authorizationContext, authorizationRepository, commandDispatcher, userRepository);
		authorizationListController =
				new AuthorizationListController(authorizationContext, authorizationRepository, userRepository);
		authorizationController = new AuthorizationController(
				authorizationContext, commandDispatcher, authorizationRepository, userRepository);
		snapshotController = new SnapshotController(authorizationContext, userRepository, indexManager);
		schedulerController = new SchedulerController(authorizationContext, userRepository, scheduler);
		redirectController = new RedirectController(authorizationContext);
		openGraphController = new OpenGraphController(authorizationContext);
	}

	Bus bus() {
		return bus;
	}

	IndexManager indexManager() {
		return indexManager;
	}

	UserRepository userRepository() {
		return userRepository;
	}

	Scheduler scheduler() {
		return scheduler;
	}

	@Nullable
	CommandReplay commandReplay() {
		return commandReplay;
	}

	@Nullable
	CommandRebuild commandRebuild() {
		return commandRebuild;
	}

	SentryFilter sentryFilter() {
		return sentryFilter;
	}

	GatekeeperFilter gatekeeperFilter() {
		return gatekeeperFilter;
	}

	QuotaExceptionFilter quotaExceptionFilter() {
		return quotaExceptionFilter;
	}

	StatusController statusController() {
		return statusController;
	}

	WhoController whoController() {
		return whoController;
	}

	PasswordResetController passwordResetController() {
		return passwordResetController;
	}

	QuotaController quotaController() {
		return quotaController;
	}

	UserListController userListController() {
		return userListController;
	}

	UserController userController() {
		return userController;
	}

	AccountController accountController() {
		return accountController;
	}

	BucketListController bucketListController() {
		return bucketListController;
	}

	BucketController bucketController() {
		return bucketController;
	}

	EventListController eventListController() {
		return eventListController;
	}

	EventController eventController() {
		return eventController;
	}

	TagController tagController() {
		return tagController;
	}

	JournalController journalController() {
		return journalController;
	}

	CredentialsListController credentialsListController() {
		return credentialsListController;
	}

	CredentialsController credentialsController() {
		return credentialsController;
	}

	TaskListController taskListController() {
		return taskListController;
	}

	TaskController taskController() {
		return taskController;
	}

	OAuthController oauthController() {
		return oauthController;
	}

	AuthorizationListController authorizationListController() {
		return authorizationListController;
	}

	AuthorizationController authorizationController() {
		return authorizationController;
	}

	SnapshotController snapshotController() {
		return snapshotController;
	}

	SchedulerController schedulerController() {
		return schedulerController;
	}

	RedirectController redirectController() {
		return redirectController;
	}

	OpenGraphController openGraphController() {
		return openGraphController;
	}

	private static String configString(Config config, String key) {
		return config.get(key).asString().orElse("");
	}

	private static boolean isConfigured(Config config, String key) {
		var node = config.get(key);
		return node.isLeaf() ? node.asString().filter(s -> !s.isEmpty()).isPresent() : node.exists();
	}
}
