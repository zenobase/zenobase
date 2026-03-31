package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.EventRepository;
import com.zenobase.services.UserRepository;

public abstract class BucketListControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final EventRepository events = mock(EventRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("tester");

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new BucketListController(auth, dispatcher, buckets, events, users);
		builder.get("/buckets/", controller::findAll);
		builder.post("/buckets/", controller::post);
		builder.get("/users/{userId}/buckets/", controller::findByUser);
	}
}
