package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;

import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;

import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public abstract class OAuthControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new OAuthController(auth, authorizations, dispatcher, users);
		builder.post("/oauth/authorize", controller::authorize);
		builder.post("/oauth/token", controller::token);
		builder.get("/oauth/callback/{id}", controller::callback);
	}

	protected static void assertGranted(Http1ClientResponse result) {
		assertThat(result).hasStatus(200).asObjectNode().path("access_token").isNotNull();
	}

	protected static void assertExpires(Http1ClientResponse result, int seconds) {
		assertThat(result).asObjectNode().path("expires_in").isEqualTo(seconds);
	}

	protected static void assertDenied(Http1ClientResponse result, String expectedError) {
		assertThat(result).hasStatus(400).asObjectNode().path("error").isEqualTo(expectedError);
	}
}
