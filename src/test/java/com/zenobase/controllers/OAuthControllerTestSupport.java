package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;

import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserRepository;

public abstract class OAuthControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(AuthorizationRepository.class).toInstance(authorizations);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(OAuthController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		OAuthController controller = injector.getInstance(OAuthController.class);
		builder.post("/oauth/authorize", controller::authorize);
		builder.post("/oauth/token", controller::token);
		builder.get("/oauth/callback/{id}", controller::callback);
	}

	protected static void assertGranted(Http1ClientResponse result) {
		assertThat(result).hasStatus(200).asObjectNode()
			.path("access_token").isNotNull();
	}

	protected static void assertExpires(Http1ClientResponse result, int seconds) {
		assertThat(result).asObjectNode()
			.path("expires_in").isEqualTo(seconds);
	}

	protected static void assertDenied(Http1ClientResponse result, String expectedError) {
		assertThat(result).hasStatus(400).asObjectNode()
			.path("error").isEqualTo(expectedError);
	}
}
