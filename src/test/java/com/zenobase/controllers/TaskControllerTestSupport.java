package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.zenobase.models.User;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import com.zenobase.tasks.TaskManagerRegistry;
import com.zenobase.tasks.TaskRefresher;
import io.helidon.webserver.http.HttpRouting;

public abstract class TaskControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final TaskManagerRegistry registry = mock(TaskManagerRegistry.class);
	protected final TaskRepository tasks = mock(TaskRepository.class);
	protected final TaskRefresher refresher = mock(TaskRefresher.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final Bus bus = new LocalBus();
	protected final User user = new User("tester");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).toInstance(bus);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(TaskManagerRegistry.class).toInstance(registry);
				bind(TaskRepository.class).toInstance(tasks);
				bind(TaskRefresher.class).toInstance(refresher);
				bind(BucketRepository.class).toInstance(buckets);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(TaskController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		TaskController controller = injector.getInstance(TaskController.class);
		builder.get("/tasks/{taskId}", controller::get);
		builder.post("/tasks/{taskId}", controller::update);
		builder.delete("/tasks/{taskId}", controller::delete);
	}
}
