package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.Bucket;
import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.EventRepository;
import com.zenobase.services.UserRepository;

public abstract class EventListControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final EventRepository events = mock(EventRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("tester");
	protected final Bucket bucket = new Bucket();

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new EventListController(auth, buckets, events, users, dispatcher);
		builder.get("/buckets/{bucketId}/", controller::find);
		builder.post("/buckets/{bucketId}/", controller::post);
		builder.get("/events/", controller::countAll);
		builder.get("/users/{userId}/events/", controller::countByUser);
	}
}
