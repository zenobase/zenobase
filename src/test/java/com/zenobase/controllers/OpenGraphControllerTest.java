package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;

import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

import com.zenobase.testing.NodeAssert;

public class OpenGraphControllerTest extends ControllerTestSupport {

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new OpenGraphController(mock(AuthorizationContext.class));
		builder.get("/og", controller::get);
	}

	@Test
	public void test() {
		try (Http1ClientResponse result = call("ogp.me")) {
			NodeAssert node = assertThat(result).hasStatus(200).asObjectNode();
			node.path("url").isEqualTo("http://ogp.me");
			node.path("title").isEqualTo("Open Graph protocol");
		}
	}

	@Test
	public void testInvalidUrl() {
		try (Http1ClientResponse result = call("")) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testInvalidHost() {
		try (Http1ClientResponse result = call("http://invalid/")) {
			assertThat(result).hasStatus(400);
		}
	}

	private Http1ClientResponse call(String url) {
		return client.get("/og").queryParam("url", url).request();
	}
}
