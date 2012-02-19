import play.Application;
import play.GlobalSettings;
import secure.UserManager;
import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import controllers.BucketController;
import controllers.DashboardController;
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
				bind(NodeManager.class).in(Singleton.class);
				bind(BucketManager.class).in(Singleton.class);
				bind(CommandQueue.class).in(Singleton.class);
				bind(UserManager.class).in(Singleton.class);
				requestStaticInjection(QueueController.class);
				requestStaticInjection(DashboardController.class);
				requestStaticInjection(SecurityController.class);
				requestStaticInjection(BucketController.class);
				requestStaticInjection(EventController.class);
				requestStaticInjection(UserController.class);
			}
		});
	}

	@Override
	public void onStop(Application application) {
		injector.getInstance(NodeManager.class).close();
	}
}
