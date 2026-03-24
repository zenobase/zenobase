package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.helidon.webserver.http.HttpRouting;

import com.zenobase.models.User;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import com.zenobase.services.QuotaManager;
import com.zenobase.services.UserRepository;

abstract class QuotaControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final QuotaManager quotas = mock(QuotaManager.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final User user = new User("tester");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(UserRepository.class).toInstance(users);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(QuotaManager.class).toInstance(quotas);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(QuotaController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		QuotaController controller = injector.getInstance(QuotaController.class);
		builder.get("/users/{userId}/quota", controller::get);
		builder.post("/users/{userId}/quota", controller::post);
	}
}
