package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;

import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserRepository;

public class RedirectControllerTest extends ControllerTestSupport {

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(mock(AuthorizationContext.class));
				bind(UserRepository.class).toInstance(mock(UserRepository.class));
			}
		});
	}

	@Test
	public void testUser() {
		String url = "https://zenobase.com/";
		Result result = call(url);
		assertThat(result).hasStatus(Http.Status.FOUND).hasHeader("Location", url).isEmpty();
	}

	private static Result call(String url) {
		return Helpers.callAction(com.zenobase.controllers.routes.ref.RedirectController.get(url));
	}
}
