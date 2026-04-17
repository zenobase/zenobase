package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import com.zenobase.tasks.CredentialsManagerRegistry;

public abstract class CredentialsListControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final CredentialsManagerRegistry registry = mock(CredentialsManagerRegistry.class);
	protected final CredentialsRepository repository = mock(CredentialsRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final User user = new User("tester");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(CredentialsManagerRegistry.class).toInstance(registry);
				bind(CredentialsRepository.class).toInstance(repository);
				bind(UserRepository.class).toInstance(users);
				bind(CredentialsListController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		CredentialsListController controller = injector.getInstance(CredentialsListController.class);
		builder.get("/credentials/", controller::findAll);
		builder.get("/users/{userId}/credentials/", controller::findByUser);
		builder.post("/credentials/", controller::post);
	}
}
