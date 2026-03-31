package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;

import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

public class RedirectControllerTest extends ControllerTestSupport {

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new RedirectController(mock(AuthorizationContext.class));
		builder.get("/to", controller::get);
	}

	@Test
	public void testUser() {
		String url = "https://zenobase.com/";
		try (Http1ClientResponse result =
				client.get("/to").queryParam("url", url).followRedirects(false).request()) {
			assertThat(result).hasStatus(302).hasHeader("Location", url).isEmpty();
		}
	}
}
