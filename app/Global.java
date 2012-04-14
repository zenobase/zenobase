import play.Application;
import play.GlobalSettings;
import services.BucketManager;
import services.CommandHandlerRegistry;
import services.CommandQueue;
import services.IndexManager;
import services.UserManager;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import commands.CommandHandler;
import commands.CommandParser;
import commands.CommandParserRegistry;
import commands.CompoundCommand;
import commands.CreateBucketCommand;
import commands.CreateEventCommand;
import commands.CreateUserCommand;
import commands.DeleteBucketCommand;
import commands.DeleteEventCommand;
import commands.DeleteUserCommand;
import commands.RestoreBucketCommand;
import commands.SuspendUserCommand;
import commands.UpdateBucketCommand;
import commands.UpdateUserCommand;

import controllers.AccountController;
import controllers.BucketController;
import controllers.BucketListController;
import controllers.EventController;
import controllers.QueueController;
import controllers.SecurityController;
import controllers.UserController;

public class Global extends GlobalSettings {

	private Injector injector;

	@Override
	public void onStart(Application application) {
		injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {

				bind(IndexManager.class).in(Singleton.class);
				bind(BucketManager.class).in(Singleton.class);
				bind(CommandQueue.class).in(Singleton.class);
				bind(UserManager.class).in(Singleton.class);
				bind(CommandParserRegistry.class).in(Singleton.class);
				bind(CommandHandlerRegistry.class).in(Singleton.class);

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

				requestStaticInjection(QueueController.class);
				requestStaticInjection(BucketListController.class);
				requestStaticInjection(SecurityController.class);
				requestStaticInjection(BucketController.class);
				requestStaticInjection(EventController.class);
				requestStaticInjection(UserController.class);
				requestStaticInjection(AccountController.class);
			}
		});
	}

	@Override
	public void onStop(Application application) {
		injector.getInstance(IndexManager.class).close();
	}
}
