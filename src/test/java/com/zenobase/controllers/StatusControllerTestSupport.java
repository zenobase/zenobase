package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import io.helidon.webserver.http.HttpRouting;

public abstract class StatusControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final Bus bus = mock(LocalBus.class);
	protected final User user = new User("tester");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).toInstance(bus);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(StatusController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		StatusController controller = injector.getInstance(StatusController.class);
		builder.get("/status", controller::get);
		builder.post("/status", controller::post);
	}
}
