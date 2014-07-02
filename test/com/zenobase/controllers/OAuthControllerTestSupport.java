package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;

import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public abstract class OAuthControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(AuthorizationRepository.class).toInstance(authorizations);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(OAuthController.class).in(Singleton.class);
			}
		});
	}

	protected static void assertGranted(Result result) {
		assertThat(result).hasStatus(Http.Status.OK).asObjectNode()
			.path("access_token").isNotNull();
	}

	protected static void assertExpires(Result result, int seconds) {
		assertThat(result).asObjectNode()
			.path("expires_in").isEqualTo(seconds);
	}

	protected static void assertDenied(Result result, String expectedError) {
		assertThat(result).hasStatus(Http.Status.BAD_REQUEST).asObjectNode()
			.path("error").isEqualTo(expectedError);
	}
}
