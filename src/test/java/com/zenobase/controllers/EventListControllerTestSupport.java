package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.Bucket;
import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.EventRepository;
import com.zenobase.services.LocalBus;
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
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(BucketRepository.class).toInstance(buckets);
				bind(EventRepository.class).toInstance(events);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(EventListController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		EventListController controller = injector.getInstance(EventListController.class);
		builder.get("/buckets/{bucketId}/", controller::find);
		builder.post("/buckets/{bucketId}/", controller::post);
		builder.get("/events/", controller::countAll);
		builder.get("/users/{userId}/events/", controller::countByUser);
	}
}
