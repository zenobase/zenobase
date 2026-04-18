package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.zenobase.models.User;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import io.helidon.webserver.http.HttpRouting;

public abstract class BucketControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final TaskRepository tasks = mock(TaskRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("tester");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(BucketRepository.class).toInstance(buckets);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(TaskRepository.class).toInstance(tasks);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		BucketController controller = injector.getInstance(BucketController.class);
		builder.get("/buckets/{bucketId}", controller::get);
		builder.get("/buckets/{bucketId}/label", controller::getLabel);
		builder.put("/buckets/{bucketId}", controller::update);
		builder.delete("/buckets/{bucketId}", controller::delete);
	}
}
