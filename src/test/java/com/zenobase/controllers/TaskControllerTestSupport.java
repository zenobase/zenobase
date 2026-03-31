package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.TaskManagerRegistry;
import com.zenobase.tasks.TaskRefresher;

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
	protected void routing(HttpRouting.Builder builder) {
		var controller = new TaskController(auth, dispatcher, registry, tasks, buckets, users, refresher, bus);
		builder.get("/tasks/{taskId}", controller::get);
		builder.post("/tasks/{taskId}", controller::update);
		builder.delete("/tasks/{taskId}", controller::delete);
	}
}
