package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import io.helidon.http.HeaderNames;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

public class OAuthControllerClientCredentialsGrantTest extends OAuthControllerTestSupport {

	@Test
	public void test() {
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_CLIENT_CREDENTIALS))) {
			assertGranted(result);
			assertExpires(result, 31 * 24 * 60 * 60);
		}
	}

	@Test
	public void testWithJson() {
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_CLIENT_CREDENTIALS).toJson())) {
			assertGranted(result);
			assertExpires(result, 31 * 24 * 60 * 60);
		}
	}

	private Http1ClientResponse call(TokenForm form) {
		return client.post("/oauth/token")
				.header(HeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded")
				.submit(toFormString(form));
	}

	private Http1ClientResponse call(JsonNode node) {
		return client.post("/oauth/token").submit(node);
	}

	private static String toFormString(TokenForm form) {
		StringBuilder sb = new StringBuilder();
		for (java.util.Map.Entry<String, String> entry : form.toMap().entrySet()) {
			if (sb.length() > 0) sb.append("&");
			sb.append(entry.getKey()).append("=").append(entry.getValue());
		}
		return sb.toString();
	}
}
