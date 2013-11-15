package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;

import com.zenobase.testing.NodeAssert;

public class OpenGraphControllerTest extends ControllerTestSupport {

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {
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
