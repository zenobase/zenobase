package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

public class OAuthControllerCallbackTest extends OAuthControllerTestSupport {

	@Test
	public void testRedirect() {
		try (Http1ClientResponse result = call("0123456789", "a=b&c=d")) {
			assertThat(result).hasStatus(303).hasHeader("Location", "/#/credentials/0123456789?a=b&c=d");
		}
	}

	@Test
	public void testRedirectSentinel() {
		try (Http1ClientResponse result = call("-", "a=b")) {
			assertThat(result).hasStatus(303).hasHeader("Location", "/#/credentials/-?a=b");
		}
	}

	@Test
	public void testRejectsInvalidId() {
		try (Http1ClientResponse result = call("zzz", "a=b")) {
			assertThat(result).hasStatus(404);
		}
	}

	private Http1ClientResponse call(String taskId, String params) {
		var request = client.get("/oauth/callback/" + taskId);
		for (String param : params.split("&")) {
			String[] kv = param.split("=", 2);
			request = request.queryParam(kv[0], kv.length > 1 ? kv[1] : "");
		}
		return request.followRedirects(false).request();
	}
}
