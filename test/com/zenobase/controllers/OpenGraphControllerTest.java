package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static play.test.Helpers.*;

import com.google.inject.AbstractModule;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeApplication;

import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserRepository;
import com.zenobase.testing.NodeAssert;

public class OpenGraphControllerTest extends ControllerTestSupport {

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(UserRepository.class).toInstance(mock(UserRepository.class));
				bind(AuthorizationContext.class).toInstance(mock(AuthorizationContext.class));
			}
		});
	}

	@Test
	public void test() {
		Result result = call("ogp.me");
		NodeAssert node = assertThat(result).hasStatus(OK).asObjectNode();
		node.path("url").isEqualTo("http://ogp.me");
		node.path("title").isEqualTo("Open Graph protocol");
	}

	@Test
	public void testInvalidUrl() {
		Result result = call("");
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testInvalidHost() {
		Result result = call("http://invalid/");
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	private static Result call(String url) {
		return callAction(com.zenobase.controllers.routes.ref.OpenGraphController.get(url), fakeRequest());
	}
}
