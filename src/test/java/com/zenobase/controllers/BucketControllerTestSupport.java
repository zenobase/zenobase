package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;

public abstract class BucketControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	protected final TaskRepository tasks = mock(TaskRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("tester");

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new BucketController(auth, dispatcher, buckets, users, authorizations, tasks);
		builder.get("/buckets/{bucketId}", controller::get);
		builder.get("/buckets/{bucketId}/label", controller::getLabel);
		builder.put("/buckets/{bucketId}", controller::update);
		builder.delete("/buckets/{bucketId}", controller::delete);
	}
}
