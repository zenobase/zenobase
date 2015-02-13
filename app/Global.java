import javax.inject.Inject;

import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Play;
import play.api.PlayException;
import play.api.mvc.Handler;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import com.zenobase.actions.Canonical;
import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserPasswordCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.commands.CommandHandler;
import com.zenobase.commands.CommandHandlerRegistry;
import com.zenobase.commands.CommandParser;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.commands.CreateCredentialsCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.DeleteCredentialsCommand;
import com.zenobase.commands.DeleteEventCommand;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.DeleteUserCommand;
import com.zenobase.commands.RestoreBucketCommand;
import com.zenobase.commands.SpendQuotaCommand;
import com.zenobase.commands.SuspendUserCommand;
import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Globals;
import com.zenobase.controllers.AccountController;
import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.controllers.AuthorizationController;
import com.zenobase.controllers.AuthorizationListController;
import com.zenobase.controllers.BucketController;
import com.zenobase.controllers.BucketListController;
import com.zenobase.controllers.ControllerSupport;
import com.zenobase.controllers.CredentialsController;
import com.zenobase.controllers.CredentialsListController;
import com.zenobase.controllers.EventController;
import com.zenobase.controllers.EventListController;
import com.zenobase.controllers.JournalController;
import com.zenobase.controllers.OAuthController;
import com.zenobase.controllers.PasswordResetController;
import com.zenobase.controllers.PaymentController;
import com.zenobase.controllers.StatusController;
import com.zenobase.controllers.TagController;
import com.zenobase.controllers.TaskController;
import com.zenobase.controllers.TaskListController;
import com.zenobase.controllers.TimezoneController;
import com.zenobase.controllers.UserController;
import com.zenobase.controllers.UserListController;
import com.zenobase.controllers.WhoController;
import com.zenobase.json.Nodes;
import com.zenobase.mail.Mailer;
import com.zenobase.mail.PasswordResetMailer;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.oauth.CustomX509TrustManager;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.ClusterNodeFactory;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CommandReplay;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.EventRepository;
import com.zenobase.services.HazelcastBus;
import com.zenobase.services.IndexManager;
import com.zenobase.services.LocalNodeFactory;
import com.zenobase.services.NodeFactory;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.QuotaManager;
import com.zenobase.services.Scheduler;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.TestNodeFactory;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.CredentialsManager;
import com.zenobase.tasks.CredentialsManagerRegistry;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskManagerRegistry;
import com.zenobase.tasks.TaskRefresher;
import com.zenobase.tasks.automatic.AutomaticCredentialsManager;
import com.zenobase.tasks.automatic.AutomaticTaskManager;
import com.zenobase.tasks.beddit.BedditCredentialsManager;
import com.zenobase.tasks.beddit.BedditTaskManager;
import com.zenobase.tasks.beeminder.BeeminderCredentialsManager;
import com.zenobase.tasks.beeminder.BeeminderTaskManager;
import com.zenobase.tasks.bodymedia.BodyMediaBurnTaskManager;
import com.zenobase.tasks.bodymedia.BodyMediaCredentialsManager;
import com.zenobase.tasks.bodymedia.BodyMediaSleepTaskManager;
import com.zenobase.tasks.bodymedia.BodyMediaStepsTaskManager;
import com.zenobase.tasks.demo.DemoCredentialsManager;
import com.zenobase.tasks.demo.DemoTaskManager;
import com.zenobase.tasks.dropbox.DropboxCredentialsManager;
import com.zenobase.tasks.fitbit.FitbitActivitiesTaskManager;
import com.zenobase.tasks.fitbit.FitbitCredentialsManager;
import com.zenobase.tasks.fitbit.FitbitFoodTaskManager;
import com.zenobase.tasks.fitbit.FitbitSleepTaskManager;
import com.zenobase.tasks.fitbit.FitbitStepsTaskManager;
import com.zenobase.tasks.fitbit.FitbitWeightTaskManager;
import com.zenobase.tasks.forecast.ForecastTaskManager;
import com.zenobase.tasks.forecast.Forecaster;
import com.zenobase.tasks.foursquare.FoursquareCredentialsManager;
import com.zenobase.tasks.foursquare.FoursquareTaskManager;
import com.zenobase.tasks.foursquare.FoursquareVenues;
import com.zenobase.tasks.google.GoogleCredentialsManager;
import com.zenobase.tasks.google.GoogleFitActivitiesTaskManager;
import com.zenobase.tasks.google.GoogleFitCardioTaskManager;
import com.zenobase.tasks.google.GoogleFitFoodTaskManager;
import com.zenobase.tasks.google.GoogleFitLocateTaskManager;
import com.zenobase.tasks.google.GoogleFitWeightTaskManager;
import com.zenobase.tasks.hexoskin.HexoskinActivitiesTaskManager;
import com.zenobase.tasks.hexoskin.HexoskinCredentialsManager;
import com.zenobase.tasks.hexoskin.HexoskinSleepTaskManager;
import com.zenobase.tasks.ihealth.IHealthActivitiesTaskManager;
import com.zenobase.tasks.ihealth.IHealthCardioTaskManager;
import com.zenobase.tasks.ihealth.IHealthCredentialsManager;
import com.zenobase.tasks.ihealth.IHealthFoodTaskManager;
import com.zenobase.tasks.ihealth.IHealthGlucoseTaskManager;
import com.zenobase.tasks.ihealth.IHealthSleepTaskManager;
import com.zenobase.tasks.ihealth.IHealthStepsTaskManager;
import com.zenobase.tasks.ihealth.IHealthWeightTaskManager;
import com.zenobase.tasks.jawbone.JawboneCredentialsManager;
import com.zenobase.tasks.jawbone.JawboneFoodTaskManager;
import com.zenobase.tasks.jawbone.JawboneSleepTaskManager;
import com.zenobase.tasks.jawbone.JawboneStepsTaskManager;
import com.zenobase.tasks.jawbone.JawboneWeightTaskManager;
import com.zenobase.tasks.lastfm.LastFmCredentialsManager;
import com.zenobase.tasks.lastfm.LastFmTaskManager;
import com.zenobase.tasks.mapmyfitness.MapMyFitnessActivitiesTaskManager;
import com.zenobase.tasks.mapmyfitness.MapMyFitnessCredentialsManager;
import com.zenobase.tasks.mapmyfitness.MapMyFitnessSleepTaskManager;
import com.zenobase.tasks.mapmyfitness.MapMyFitnessWeightTaskManager;
import com.zenobase.tasks.misfit.MisfitActivitiesTaskManager;
import com.zenobase.tasks.misfit.MisfitCredentialsManager;
import com.zenobase.tasks.misfit.MisfitSleepTaskManager;
import com.zenobase.tasks.misfit.MisfitStepsTaskManager;
import com.zenobase.tasks.moves.MovesActivitiesTaskManager;
import com.zenobase.tasks.moves.MovesCredentialsManager;
import com.zenobase.tasks.moves.MovesLocateTaskManager;
import com.zenobase.tasks.moves.MovesPlacesTaskManager;
import com.zenobase.tasks.moves.MovesStepsTaskManager;
import com.zenobase.tasks.netatmo.NetatmoCredentialsManager;
import com.zenobase.tasks.netatmo.NetatmoTaskManager;
import com.zenobase.tasks.reporter.ReporterTaskManager;
import com.zenobase.tasks.rescuetime.RescueTimeCredentialsManager;
import com.zenobase.tasks.rescuetime.RescueTimeProductivityTaskManager;
import com.zenobase.tasks.runkeeper.RunkeeperActivitiesTaskManager;
import com.zenobase.tasks.runkeeper.RunkeeperCredentialsManager;
import com.zenobase.tasks.runkeeper.RunkeeperWeightTaskManager;
import com.zenobase.tasks.sleepcloud.SleepCloudTaskManager;
import com.zenobase.tasks.strava.StravaCredentialsManager;
import com.zenobase.tasks.strava.StravaTaskManager;
import com.zenobase.tasks.trackthisforme.TrackthisformeCredentialsManager;
import com.zenobase.tasks.trackthisforme.TrackthisformeTaskManager;
import com.zenobase.tasks.withings.WithingsCardioTaskManager;
import com.zenobase.tasks.withings.WithingsCredentialsManager;
import com.zenobase.tasks.withings.WithingsSleepTaskManager;
import com.zenobase.tasks.withings.WithingsStepsTaskManager;
import com.zenobase.tasks.withings.WithingsWeightTaskManager;

public class Global extends GlobalSettings {

	@Inject
	private Canonical canonical;
	private Injector injector;

	@Override
	public void onStart(Application application) {
		CustomX509TrustManager.setDefault();
		Json.setObjectMapper(Nodes.MAPPER);
		createInjector();
		replay();
	}

	private void createInjector() {
		injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {

				bindConfiguration();

				bind(Bus.class).to(HazelcastBus.class).in(Singleton.class);
				bind(NodeFactory.class).to(getNodeFactory()).in(Singleton.class);
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
				bind(Canonical.class).in(Singleton.class);
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
				if (isConfigured("forecast")) {
					bind(Forecaster.class).in(Singleton.class);
				}

				Multibinder<CommandParser> parsers = Multibinder.newSetBinder(binder(), CommandParser.class);
				parsers.addBinding().to(CreateBucketCommand.Parser.class);
				parsers.addBinding().to(DeleteBucketCommand.Parser.class);
				parsers.addBinding().to(RestoreBucketCommand.Parser.class);
				parsers.addBinding().to(UpdateBucketCommand.Parser.class);
				parsers.addBinding().to(UpdateEventCommand.Parser.class);
				parsers.addBinding().to(CreateEventCommand.Parser.class);
				parsers.addBinding().to(DeleteEventCommand.Parser.class);
				parsers.addBinding().to(CreateUserCommand.Parser.class);
				parsers.addBinding().to(DeleteUserCommand.Parser.class);
				parsers.addBinding().to(ChangeUserEmailCommand.Parser.class);
				parsers.addBinding().to(SuspendUserCommand.Parser.class);
				parsers.addBinding().to(ChangeUserPasswordCommand.Parser.class);
				parsers.addBinding().to(ChangeUserVerifiedCommand.Parser.class);
				parsers.addBinding().to(ChangeQuotaCommand.Parser.class);
				parsers.addBinding().to(SpendQuotaCommand.Parser.class);
				parsers.addBinding().to(CreateTaskCommand.Parser.class);
				parsers.addBinding().to(UpdateTaskCommand.Parser.class);
				parsers.addBinding().to(DeleteTaskCommand.Parser.class);
				parsers.addBinding().to(CreateCredentialsCommand.Parser.class);
				parsers.addBinding().to(UpdateCredentialsCommand.Parser.class);
				parsers.addBinding().to(DeleteCredentialsCommand.Parser.class);
				parsers.addBinding().to(CompoundCommand.Parser.class);
				parsers.addBinding().to(CreateAuthorizationCommand.Parser.class);
				parsers.addBinding().to(DeleteAuthorizationCommand.Parser.class);

				Multibinder<CommandHandler<?>> handlers = Multibinder.newSetBinder(binder(), new TypeLiteral<CommandHandler<?>>() {});
				handlers.addBinding().to(CreateBucketCommand.Handler.class);
				handlers.addBinding().to(DeleteBucketCommand.Handler.class);
				handlers.addBinding().to(RestoreBucketCommand.Handler.class);
				handlers.addBinding().to(UpdateBucketCommand.Handler.class);
				handlers.addBinding().to(UpdateEventCommand.Handler.class);
				handlers.addBinding().to(CreateEventCommand.Handler.class);
				handlers.addBinding().to(DeleteEventCommand.Handler.class);
				handlers.addBinding().to(CreateUserCommand.Handler.class);
				handlers.addBinding().to(DeleteUserCommand.Handler.class);
				handlers.addBinding().to(ChangeUserEmailCommand.Handler.class);
				handlers.addBinding().to(SuspendUserCommand.Handler.class);
				handlers.addBinding().to(ChangeUserPasswordCommand.Handler.class);
				handlers.addBinding().to(ChangeUserVerifiedCommand.Handler.class);
				handlers.addBinding().to(ChangeQuotaCommand.Handler.class);
				handlers.addBinding().to(SpendQuotaCommand.Handler.class);
				handlers.addBinding().to(CreateTaskCommand.Handler.class);
				handlers.addBinding().to(UpdateTaskCommand.Handler.class);
				handlers.addBinding().to(DeleteTaskCommand.Handler.class);
				handlers.addBinding().to(CreateCredentialsCommand.Handler.class);
				handlers.addBinding().to(UpdateCredentialsCommand.Handler.class);
				handlers.addBinding().to(DeleteCredentialsCommand.Handler.class);
				handlers.addBinding().to(CreateAuthorizationCommand.Handler.class);
				handlers.addBinding().to(DeleteAuthorizationCommand.Handler.class);

				Multibinder<CredentialsManager> credentials = Multibinder.newSetBinder(binder(), new TypeLiteral<CredentialsManager>() {});
				credentials.addBinding().to(DemoCredentialsManager.class);
				bindIfConfigured("fitbit", FitbitCredentialsManager.class, credentials);
				bindIfConfigured("foursquare", FoursquareCredentialsManager.class, credentials);
				bindIfConfigured("withings", WithingsCredentialsManager.class, credentials);
				bindIfConfigured("bodymedia", BodyMediaCredentialsManager.class, credentials);
				bindIfConfigured("netatmo", NetatmoCredentialsManager.class, credentials);
				bindIfConfigured("runkeeper", RunkeeperCredentialsManager.class, credentials);
				bindIfConfigured("strava", StravaCredentialsManager.class, credentials);
				bindIfConfigured("mapmyfitness", MapMyFitnessCredentialsManager.class, credentials);
				bindIfConfigured("moves", MovesCredentialsManager.class, credentials);
				bindIfConfigured("dropbox", DropboxCredentialsManager.class, credentials);
				bindIfConfigured("lastfm", LastFmCredentialsManager.class, credentials);
				bindIfConfigured("rescuetime", RescueTimeCredentialsManager.class, credentials);
				bindIfConfigured("google", GoogleCredentialsManager.class, credentials);
				bindIfConfigured("automatic", AutomaticCredentialsManager.class, credentials);
				bindIfConfigured("jawbone", JawboneCredentialsManager.class, credentials);
				bindIfConfigured("misfit", MisfitCredentialsManager.class, credentials);
				bindIfConfigured("trackthisforme", TrackthisformeCredentialsManager.class, credentials);
				bindIfConfigured("ihealth", IHealthCredentialsManager.class, credentials);
				bindIfConfigured("beeminder", BeeminderCredentialsManager.class, credentials);
				bindIfConfigured("hexoskin", HexoskinCredentialsManager.class, credentials);
				bindIfConfigured("beddit", BedditCredentialsManager.class, credentials);
				bind(CredentialsManagerRegistry.class).in(Singleton.class);

				Multibinder<TaskManager> tasks = Multibinder.newSetBinder(binder(), new TypeLiteral<TaskManager>() {});
				tasks.addBinding().to(DemoTaskManager.class);
				bindIfConfigured("fitbit", FitbitActivitiesTaskManager.class, tasks);
				bindIfConfigured("fitbit", FitbitStepsTaskManager.class, tasks);
				bindIfConfigured("fitbit", FitbitSleepTaskManager.class, tasks);
				bindIfConfigured("fitbit", FitbitWeightTaskManager.class, tasks);
				bindIfConfigured("fitbit", FitbitFoodTaskManager.class, tasks);
				bindIfConfigured("foursquare", FoursquareTaskManager.class, tasks);
				bindIfConfigured("withings", WithingsCardioTaskManager.class, tasks);
				bindIfConfigured("withings", WithingsStepsTaskManager.class, tasks);
				bindIfConfigured("withings", WithingsWeightTaskManager.class, tasks);
				bindIfConfigured("withings", WithingsSleepTaskManager.class, tasks);
				bindIfConfigured("bodymedia", BodyMediaBurnTaskManager.class, tasks);
				bindIfConfigured("bodymedia", BodyMediaStepsTaskManager.class, tasks);
				bindIfConfigured("bodymedia", BodyMediaSleepTaskManager.class, tasks);
				bindIfConfigured("netatmo", NetatmoTaskManager.class, tasks);
				bindIfConfigured("runkeeper", RunkeeperActivitiesTaskManager.class, tasks);
				bindIfConfigured("runkeeper", RunkeeperWeightTaskManager.class, tasks);
				bindIfConfigured("strava", StravaTaskManager.class, tasks);
				bindIfConfigured("mapmyfitness", MapMyFitnessActivitiesTaskManager.class, tasks);
				bindIfConfigured("mapmyfitness", MapMyFitnessSleepTaskManager.class, tasks);
				bindIfConfigured("mapmyfitness", MapMyFitnessWeightTaskManager.class, tasks);
				bindIfConfigured("moves", MovesActivitiesTaskManager.class, tasks);
				bindIfConfigured("moves", MovesLocateTaskManager.class, tasks);
				bindIfConfigured("moves", MovesPlacesTaskManager.class, tasks);
				bindIfConfigured("moves", MovesStepsTaskManager.class, tasks);
				bindIfConfigured("dropbox", ReporterTaskManager.class, tasks);
				bindIfConfigured("lastfm", LastFmTaskManager.class, tasks);
				bindIfConfigured("forecast", ForecastTaskManager.class, tasks);
				bindIfConfigured("rescuetime", RescueTimeProductivityTaskManager.class, tasks);
				bindIfConfigured("google", SleepCloudTaskManager.class, tasks);
				bindIfConfigured("google", GoogleFitActivitiesTaskManager.class, tasks);
				bindIfConfigured("google", GoogleFitCardioTaskManager.class, tasks);
				bindIfConfigured("google", GoogleFitFoodTaskManager.class, tasks);
				bindIfConfigured("google", GoogleFitLocateTaskManager.class, tasks);
				bindIfConfigured("google", GoogleFitWeightTaskManager.class, tasks);
				bindIfConfigured("automatic", AutomaticTaskManager.class, tasks);
				bindIfConfigured("jawbone", JawboneStepsTaskManager.class, tasks);
				bindIfConfigured("jawbone", JawboneSleepTaskManager.class, tasks);
				bindIfConfigured("jawbone", JawboneFoodTaskManager.class, tasks);
				bindIfConfigured("jawbone", JawboneWeightTaskManager.class, tasks);
				bindIfConfigured("misfit", MisfitActivitiesTaskManager.class, tasks);
				bindIfConfigured("misfit", MisfitStepsTaskManager.class, tasks);
				bindIfConfigured("misfit", MisfitSleepTaskManager.class, tasks);
				bindIfConfigured("ihealth", IHealthActivitiesTaskManager.class, tasks);
				bindIfConfigured("ihealth", IHealthCardioTaskManager.class, tasks);
				bindIfConfigured("ihealth", IHealthFoodTaskManager.class, tasks);
				bindIfConfigured("ihealth", IHealthGlucoseTaskManager.class, tasks);
				bindIfConfigured("ihealth", IHealthSleepTaskManager.class, tasks);
				bindIfConfigured("ihealth", IHealthStepsTaskManager.class, tasks);
				bindIfConfigured("ihealth", IHealthWeightTaskManager.class, tasks);
				bindIfConfigured("trackthisforme", TrackthisformeTaskManager.class, tasks);
				bindIfConfigured("beeminder", BeeminderTaskManager.class, tasks);
				bindIfConfigured("hexoskin", HexoskinActivitiesTaskManager.class, tasks);
				bindIfConfigured("hexoskin", HexoskinSleepTaskManager.class, tasks);
				bindIfConfigured("beddit", BedditTaskManager.class, tasks);
				bind(TaskManagerRegistry.class).in(Singleton.class);

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
				bind(TimezoneController.class).in(Singleton.class);
				bind(PaymentController.class).in(Singleton.class);

				requestInjection(Global.this);
			}

			private <T> void bindIfConfigured(String prefix, Class<? extends T> type, Multibinder<T> binder) {
				if (isConfigured(prefix)) {
					binder.addBinding().to(type).in(Singleton.class);
				}
			}

			private boolean isConfigured(String key) {
				return Play.application().configuration().getConfig(key) != null;
			}

			private Class<? extends NodeFactory> getNodeFactory() {
				if (Play.isTest()) {
					return TestNodeFactory.class;
				} else if (Play.isDev()) {
					return LocalNodeFactory.class;
				} else {
					return ClusterNodeFactory.class;
				}
			}

			private void bindConfiguration() {
				Configuration conf = Play.application().configuration();
				for (String key : conf.keys()) {
					try {
						String value = conf.getString(key);
						bindConstant().annotatedWith(Names.named(key)).to(value);
					} catch (PlayException e) {

					}
				}
			}
		});

		Globals.put(Injector.class, injector);
	}

	@Override
	public <A> A getControllerInstance(Class<A> controllerClass) {
		return injector.getInstance(controllerClass);
	}

	private void replay() {
		UserRepository users = injector.getInstance(UserRepository.class);
		if (users.isEmpty()) {
			injector.getInstance(CommandReplay.class).replay();
		}
	}

	/*@Override
	@SuppressWarnings("unchecked")
	public <T extends EssentialFilter> Class<T>[] filters() {
		return new Class[] { SecurityHeadersFilter.class, GzipFilter.class };
	}*/

	@Override
	public Handler onRouteRequest(RequestHeader request) {
		return canonical.test(request) ? super.onRouteRequest(request) : canonical.redirect(request);
	}

	@Override
	public Promise<Result> onError(RequestHeader request, Throwable t) {
		return isProgrammatic(request)
			? Promise.<Result>pure(ControllerSupport.internalServerError(Throwables.getRootCause(t).getMessage()))
			: super.onError(request, t);
	}

	@Override
	public Promise<Result> onHandlerNotFound(RequestHeader request) {
		return isProgrammatic(request)
			? Promise.<Result>pure(ControllerSupport.notFound())
			: super.onHandlerNotFound(request);
	}

	@Override
	public Promise<Result> onBadRequest(RequestHeader request, String error) {
		return isProgrammatic(request)
			? Promise.<Result>pure(ControllerSupport.badRequest(error))
			: super.onBadRequest(request, error);
	}

	private boolean isProgrammatic(RequestHeader request) {
		return request.accepts("application/json");
	}

	@Override
	public void onStop(Application application) {
		injector.getInstance(Scheduler.class).close();
		injector.getInstance(IndexManager.class).close();
		injector.getInstance(Bus.class).close();
	}
}
