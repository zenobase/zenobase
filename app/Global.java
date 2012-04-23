import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Play;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import com.zenobase.commands.ChangePasswordCommand;
import com.zenobase.commands.CommandHandler;
import com.zenobase.commands.CommandParser;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.DeleteEventCommand;
import com.zenobase.commands.DeleteUserCommand;
import com.zenobase.commands.RestoreBucketCommand;
import com.zenobase.commands.SuspendUserCommand;
import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.commands.UpdateUserCommand;
import com.zenobase.commands.VerifyUserCommand;
import com.zenobase.common.Scheduled;
import com.zenobase.common.ScheduledInterceptor;
import com.zenobase.controllers.AccountController;
import com.zenobase.controllers.BucketController;
import com.zenobase.controllers.BucketListController;
import com.zenobase.controllers.EventController;
import com.zenobase.controllers.PasswordResetMailer;
import com.zenobase.controllers.QueueController;
import com.zenobase.controllers.SecurityController;
import com.zenobase.controllers.UserController;
import com.zenobase.controllers.VerificationMailer;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandHandlerRegistry;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.CommandReplay;
import com.zenobase.services.CommandStore;
import com.zenobase.services.IndexManager;
import com.zenobase.services.Mailer;
import com.zenobase.services.PersistentCommandStore;
import com.zenobase.services.SmtpMailer;
import com.zenobase.services.UserManager;

public class Global extends GlobalSettings {

	private Injector injector;

	@Override
	public void onStart(Application application) {
		createInjector();
		replay();
	}

	private void createInjector() {
		injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {

				bindConfiguration();

				bind(IndexManager.class).in(Singleton.class);
				bind(BucketManager.class).in(Singleton.class);
				bind(CommandQueue.class).in(Singleton.class);
				bind(CommandStore.class).to(PersistentCommandStore.class);
				bind(UserManager.class).in(Singleton.class);
				bind(CommandParserRegistry.class).in(Singleton.class);
				bind(CommandHandlerRegistry.class).in(Singleton.class);
				bind(CommandReplay.class).in(Singleton.class);
				bind(Mailer.class).to(SmtpMailer.class);
				bind(VerificationMailer.class).in(Singleton.class);
				bind(PasswordResetMailer.class).in(Singleton.class);

				Multibinder<CommandParser> parsers = Multibinder.newSetBinder(binder(), CommandParser.class);
				parsers.addBinding().to(CreateBucketCommand.Parser.class);
				parsers.addBinding().to(DeleteBucketCommand.Parser.class);
				parsers.addBinding().to(RestoreBucketCommand.Parser.class);
				parsers.addBinding().to(UpdateBucketCommand.Parser.class);
				parsers.addBinding().to(CreateEventCommand.Parser.class);
				parsers.addBinding().to(DeleteEventCommand.Parser.class);
				parsers.addBinding().to(CreateUserCommand.Parser.class);
				parsers.addBinding().to(DeleteUserCommand.Parser.class);
				parsers.addBinding().to(UpdateUserCommand.Parser.class);
				parsers.addBinding().to(SuspendUserCommand.Parser.class);
				parsers.addBinding().to(ChangePasswordCommand.Parser.class);
				parsers.addBinding().to(CompoundCommand.Parser.class);

				Multibinder<CommandHandler<?>> handlers = Multibinder.newSetBinder(binder(), new TypeLiteral<CommandHandler<?>>() {});
				handlers.addBinding().to(CreateBucketCommand.Handler.class);
				handlers.addBinding().to(DeleteBucketCommand.Handler.class);
				handlers.addBinding().to(RestoreBucketCommand.Handler.class);
				handlers.addBinding().to(UpdateBucketCommand.Handler.class);
				handlers.addBinding().to(CreateEventCommand.Handler.class);
				handlers.addBinding().to(DeleteEventCommand.Handler.class);
				handlers.addBinding().to(CreateUserCommand.Handler.class);
				handlers.addBinding().to(DeleteUserCommand.Handler.class);
				handlers.addBinding().to(UpdateUserCommand.Handler.class);
				handlers.addBinding().to(SuspendUserCommand.Handler.class);
				handlers.addBinding().to(VerifyUserCommand.Handler.class);
				handlers.addBinding().to(ChangePasswordCommand.Handler.class);

				requestStaticInjection(QueueController.class);
				requestStaticInjection(BucketListController.class);
				requestStaticInjection(SecurityController.class);
				requestStaticInjection(BucketController.class);
				requestStaticInjection(EventController.class);
				requestStaticInjection(UserController.class);
				requestStaticInjection(AccountController.class);

				bindInterceptor(Matchers.any(), Matchers.annotatedWith(Scheduled.class), new ScheduledInterceptor());
			}

			private void bindConfiguration() {
				Configuration conf = Play.application().configuration().getConfig("zeno");
				for (String key : conf.keys()) {
					bind(String.class).annotatedWith(Names.named(key)).toInstance(conf.getString(key));
				}
			}
		});
	}

	private void replay() {
		UserManager users = injector.getInstance(UserManager.class);
		if (users.isEmpty()) {
			injector.getInstance(CommandReplay.class).replay();
		}
	}

	@Override
	public void onStop(Application application) {
		injector.getInstance(IndexManager.class).close();
	}
}
