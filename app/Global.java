import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.api.PlayException;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserPasswordCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.commands.CommandHandler;
import com.zenobase.commands.CommandHandlerRegistry;
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
import com.zenobase.controllers.AccountController;
import com.zenobase.controllers.BucketController;
import com.zenobase.controllers.BucketListController;
import com.zenobase.controllers.EventController;
import com.zenobase.controllers.EventListController;
import com.zenobase.controllers.PasswordResetController;
import com.zenobase.controllers.QueueController;
import com.zenobase.controllers.Canonical;
import com.zenobase.controllers.SecurityContext;
import com.zenobase.controllers.SecurityController;
import com.zenobase.controllers.StatusController;
import com.zenobase.controllers.UserController;
import com.zenobase.controllers.UserListController;
import com.zenobase.controllers.WhoController;
import com.zenobase.mail.Mailer;
import com.zenobase.mail.PasswordResetMailer;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CommandReplay;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.IndexManager;
import com.zenobase.services.UserRepository;

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

				bind(Boolean.class).annotatedWith(Names.named("es.clientOnly")).toInstance(Play.isProd());

				bindConfiguration();

				bind(IndexManager.class).in(Singleton.class);
				bind(BucketRepository.class).in(Singleton.class);
				bind(CommandDispatcher.class).in(Singleton.class);
				bind(CommandRepository.class).in(Singleton.class);
				bind(UserRepository.class).in(Singleton.class);
				bind(CommandParserRegistry.class).in(Singleton.class);
				bind(CommandHandlerRegistry.class).in(Singleton.class);
				bind(CommandReplay.class).in(Singleton.class);
				bind(Mailer.class).in(Singleton.class);
				bind(VerificationMailer.class).in(Singleton.class);
				bind(PasswordResetMailer.class).in(Singleton.class);
				bind(SecurityContext.class).in(Singleton.class);

				Multibinder<CommandParser> parsers = Multibinder.newSetBinder(binder(), CommandParser.class);
				parsers.addBinding().to(CreateBucketCommand.Parser.class);
				parsers.addBinding().to(DeleteBucketCommand.Parser.class);
				parsers.addBinding().to(RestoreBucketCommand.Parser.class);
				parsers.addBinding().to(UpdateBucketCommand.Parser.class);
				parsers.addBinding().to(CreateEventCommand.Parser.class);
				parsers.addBinding().to(DeleteEventCommand.Parser.class);
				parsers.addBinding().to(CreateUserCommand.Parser.class);
				parsers.addBinding().to(DeleteUserCommand.Parser.class);
				parsers.addBinding().to(ChangeUserEmailCommand.Parser.class);
				parsers.addBinding().to(SuspendUserCommand.Parser.class);
				parsers.addBinding().to(ChangeUserPasswordCommand.Parser.class);
				parsers.addBinding().to(ChangeUserVerifiedCommand.Parser.class);
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
				handlers.addBinding().to(ChangeUserEmailCommand.Handler.class);
				handlers.addBinding().to(SuspendUserCommand.Handler.class);
				handlers.addBinding().to(ChangeUserVerifiedCommand.Handler.class);
				handlers.addBinding().to(ChangeUserPasswordCommand.Handler.class);

				requestStaticInjection(AccountController.class);
				requestStaticInjection(BucketController.class);
				requestStaticInjection(BucketListController.class);
				requestStaticInjection(EventController.class);
				requestStaticInjection(EventListController.class);
				requestStaticInjection(PasswordResetController.class);
				requestStaticInjection(QueueController.class);
				requestStaticInjection(SecurityController.class);
				requestStaticInjection(StatusController.class);
				requestStaticInjection(UserController.class);
				requestStaticInjection(UserListController.class);
				requestStaticInjection(WhoController.class);

				requestStaticInjection(Canonical.class);
			}

			private void bindConfiguration() {
				Configuration conf = Play.application().configuration();
				for (String key : conf.keys()) {
					try {
						String value = conf.getString(key);
						bind(String.class).annotatedWith(Names.named(key)).toInstance(value);
					} catch (PlayException e) {
						Logger.warn("Can't bind property from " + e.description());
					}
				}
			}
		});
	}

	private void replay() {
		UserRepository users = injector.getInstance(UserRepository.class);
		if (users.isEmpty()) {
			injector.getInstance(CommandReplay.class).replay();
		}
	}

	@Override
	public void onStop(Application application) {
		injector.getInstance(IndexManager.class).close();
	}
}
