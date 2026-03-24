package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;

public class OAuthControllerCallbackTest extends OAuthControllerTestSupport {

	@Test
	public void testRedirect() {
		try (Http1ClientResponse result = call("zzz", "a=b&c=d")) {
			assertThat(result).hasStatus(303).hasHeader("Location", "/#/credentials/zzz?a=b&c=d");
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
