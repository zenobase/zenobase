import play.Application;
import play.GlobalSettings;
import services.BucketManager;
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
import commands.CreateBucketCommandHandler;
import commands.CreateEventCommandHandler;
import commands.CreateUserCommandHandler;
import commands.DeleteBucketCommandHandler;
import commands.DeleteEventCommandHandler;
import commands.DeleteUserCommandHandler;
import commands.RestoreBucketCommandHandler;
import commands.SuspendUserCommandHandler;
import commands.UpdateBucketCommandHandler;
import commands.UpdateUserCommandHandler;

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

				Multibinder<CommandHandler<?>> handlers = Multibinder.newSetBinder(binder(), new TypeLiteral<CommandHandler<?>>() {});
				handlers.addBinding().to(CreateBucketCommandHandler.class);
				handlers.addBinding().to(DeleteBucketCommandHandler.class);
				handlers.addBinding().to(RestoreBucketCommandHandler.class);
				handlers.addBinding().to(UpdateBucketCommandHandler.class);
				handlers.addBinding().to(CreateEventCommandHandler.class);
				handlers.addBinding().to(DeleteEventCommandHandler.class);
				handlers.addBinding().to(CreateUserCommandHandler.class);
				handlers.addBinding().to(DeleteUserCommandHandler.class);
				handlers.addBinding().to(UpdateUserCommandHandler.class);
				handlers.addBinding().to(SuspendUserCommandHandler.class);

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
