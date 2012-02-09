import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class Module extends AbstractModule {

	@Override
	protected void configure() {
		bind(NodeManager.class).in(Singleton.class);
		bind(BucketManager.class).in(Singleton.class);
		bind(CommandQueue.class).in(Singleton.class);
	}
}
