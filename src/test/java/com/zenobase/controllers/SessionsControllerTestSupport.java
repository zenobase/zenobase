package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.zenobase.auth.UserDirectory;
import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import io.helidon.webserver.http.HttpRouting;

public abstract class SessionsControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserDirectory directory = mock(UserDirectory.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final User user = newUser("tester");

	private static User newUser(String name) {
		User user = new User(name);
		user.setExternalId("auth0|" + name);
		return user;
	}

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserDirectory.class).toInstance(directory);
				bind(UserRepository.class).toInstance(users);
				bind(SessionsController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		SessionsController controller = injector.getInstance(SessionsController.class);
		builder.get("/sessions/", controller::findAll);
		builder.get("/users/{userId}/sessions/", controller::findByUser);
		builder.delete("/users/{userId}/sessions/", controller::deleteAll);
		builder.delete("/users/{userId}/sessions/{sessionId}", controller::deleteOne);
	}
}
