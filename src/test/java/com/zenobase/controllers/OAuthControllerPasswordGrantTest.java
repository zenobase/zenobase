package com.zenobase.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.helidon.http.HeaderNames;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;

public class OAuthControllerPasswordGrantTest extends OAuthControllerTestSupport {

	private User user = new User("me");
	private String password = "secret123";

	@Before
	public void setUp() {
		user.setVerified(true);
		user.setSuspended(false);
		user.setPassword(password);
	}

	@Test
	public void test() {
		when(users.find(user.getName())).thenReturn(user);
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c");
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), password))) {
			assertGranted(result);
			assertExpires(result, 31 * 24 * 60 * 60);
			Authorization auth = arg.getValue().getAuthorization();
			assertThat(auth.getId()).isNotNull();
			assertThat(auth.getPrincipal()).isEqualTo(user.asIdentity());
			assertThat(auth.getClient()).isNull();
			assertThat(auth.getScope()).isNull();
		}
	}

	@Test
	public void testWithJson() {
		when(users.find(user.getName())).thenReturn(user);
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c");
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), password).toJson())) {
			assertGranted(result);
			assertExpires(result, 31 * 24 * 60 * 60);
			Authorization auth = arg.getValue().getAuthorization();
			assertThat(auth.getId()).isNotNull();
			assertThat(auth.getPrincipal()).isEqualTo(user.asIdentity());
			assertThat(auth.getClient()).isNull();
			assertThat(auth.getScope()).isNull();
		}
	}

	@Test
	public void testMissingUsername() {
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, null, password))) {
			assertDenied(result, OAuthController.INVALID_REQUEST);
		}
	}

	@Test
	public void testMissingPassword() {
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), null))) {
			assertDenied(result, OAuthController.INVALID_REQUEST);
		}
	}

	@Test
	public void testUnknownUser() {
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), password))) {
			assertDenied(result, OAuthController.ACCESS_DENIED);
		}
	}

	@Test
	public void testSuspendedUser() {
		user.setSuspended(true);
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), password))) {
			assertDenied(result, OAuthController.ACCESS_DENIED);
		}
	}

	@Test
	public void testBadPassword() {
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), "forgotten"))) {
			assertDenied(result, OAuthController.ACCESS_DENIED);
		}
	}

	@Test
	public void testBadGrantType() {
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = call(new TokenForm("foo", user.getName(), password))) {
			assertDenied(result, OAuthController.UNSUPPORTED_GRANT_TYPE);
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
