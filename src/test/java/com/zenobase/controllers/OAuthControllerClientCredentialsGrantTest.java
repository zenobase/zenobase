package com.zenobase.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.helidon.http.HeaderNames;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.oauth.Authorization;

public class OAuthControllerClientCredentialsGrantTest extends OAuthControllerTestSupport {

	@Test
	public void test() {
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c");
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_CLIENT_CREDENTIALS, null, null))) {
			assertGranted(result);
			Authorization auth = arg.getValue().getAuthorization();
			assertThat(auth.getId()).isNotNull();
			assertThat(auth.getPrincipal()).isNotNull();
			assertThat(auth.getClient()).isNull();
			assertThat(auth.getScope()).isNull();
		}
	}

	@Test
	public void testWithJson() {
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c");
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_CLIENT_CREDENTIALS, null, null).toJson())) {
			assertGranted(result);
			Authorization auth = arg.getValue().getAuthorization();
			assertThat(auth.getId()).isNotNull();
			assertThat(auth.getPrincipal()).isNotNull();
			assertThat(auth.getClient()).isNull();
			assertThat(auth.getScope()).isNull();
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
