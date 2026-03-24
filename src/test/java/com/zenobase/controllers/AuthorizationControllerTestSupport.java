package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserRepository;

public abstract class AuthorizationControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("tester");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(AuthorizationRepository.class).toInstance(authorizations);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(AuthorizationController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		AuthorizationController controller = injector.getInstance(AuthorizationController.class);
		builder.get("/authorizations/{authId}", controller::get);
		builder.delete("/authorizations/{authId}", controller::delete);
	}
}
