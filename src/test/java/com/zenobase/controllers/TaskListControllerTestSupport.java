package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.TaskManagerRegistry;

public abstract class TaskListControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final TaskManagerRegistry registry = mock(TaskManagerRegistry.class);
	protected final TaskRepository tasks = mock(TaskRepository.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final User user = new User("tester");

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new TaskListController(auth, dispatcher, registry, tasks, buckets, users);
		builder.get("/tasks/", controller::findAll);
		builder.get("/buckets/{bucketId}/tasks/", controller::findByBucket);
		builder.get("/users/{userId}/tasks/", controller::findByUser);
		builder.post("/tasks/", controller::post);
	}
}
