package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationQuery;

public class OAuthControllerImplicitGrantTest extends OAuthControllerTestSupport {

	private User user = new User("me");
	private User oauthClient = new User("zeno");
	private String redirectUri = "https://zenobase.com/callback";
	private String scope = Generator.id();

	@BeforeEach
	public void setUp() {
		oauthClient.setVerified(true);
		oauthClient.setSuspended(false);
		oauthClient.setEmail("info@zenobase.com");
	}

	@Test
	public void test() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(oauthClient.asIdentity())).thenReturn(oauthClient);
		AuthorizationQuery query = new AuthorizationQuery()
				.principalEqualTo(user.asIdentity())
				.clientEqualTo(oauthClient.asIdentity())
				.scopeEqualTo(scope);
		when(authorizations.find(eq(query), anyInt(), anyInt())).thenReturn(DefaultPartialList.of());
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c1");
		try (Http1ClientResponse result = call(
				new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, oauthClient.asIdentity(), redirectUri, scope))) {
			assertGranted(result);
			Authorization auth = arg.getValue().getAuthorization();
			assertThat(auth.getId()).isNotNull();
			assertThat(auth.getPrincipal()).isEqualTo(user.asIdentity());
			assertThat(auth.getClient()).isEqualTo(oauthClient.asIdentity());
			assertThat(auth.getScope()).isEqualTo(scope);
		}
	}

	@Test
	public void testReusesTokenOnSecondRequest() {
		Authorization authorization = new Authorization(user.asIdentity(), oauthClient.asIdentity(), scope);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(oauthClient.asIdentity())).thenReturn(oauthClient);
		AuthorizationQuery query = new AuthorizationQuery()
				.principalEqualTo(user.asIdentity())
				.clientEqualTo(oauthClient.asIdentity())
				.scopeEqualTo(scope);
		when(authorizations.find(eq(query), anyInt(), anyInt())).thenReturn(DefaultPartialList.of(authorization));
		try (Http1ClientResponse result = call(
				new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, oauthClient.asIdentity(), redirectUri, scope))) {
			assertThat(result)
					.hasStatus(200)
					.asObjectNode()
					.path("access_token")
					.isEqualTo(authorization.getId());
		}
	}

	@Test
	public void testUnauthorized() {
		try (Http1ClientResponse result = call(
				new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, oauthClient.asIdentity(), redirectUri, scope))) {
			assertThat(result).hasStatus(401).isEmpty();
		}
	}

	@Test
	public void testMissingClient() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(oauthClient.asIdentity())).thenReturn(oauthClient);
		try (Http1ClientResponse result =
				call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, null, redirectUri, scope))) {
			assertDenied(result, OAuthController.INVALID_REQUEST);
		}
	}

	@Test
	public void testMissingRedirectUri() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(oauthClient.asIdentity())).thenReturn(oauthClient);
		try (Http1ClientResponse result =
				call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, oauthClient.asIdentity(), null, scope))) {
			assertDenied(result, OAuthController.INVALID_REQUEST);
		}
	}

	@Test
	public void testBadRedirectUri() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(oauthClient.asIdentity())).thenReturn(oauthClient);
		try (Http1ClientResponse result = call(new AuthorizeForm(
				OAuthController.RESPONSE_TYPE_TOKEN, oauthClient.asIdentity(), "http://foo.test/", scope))) {
			assertDenied(result, OAuthController.INVALID_REDIRECT_URI);
		}
	}

	@Test
	public void testMissingScope() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(oauthClient.asIdentity())).thenReturn(oauthClient);
		try (Http1ClientResponse result = call(
				new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, oauthClient.asIdentity(), redirectUri, null))) {
			assertDenied(result, OAuthController.INVALID_SCOPE);
		}
	}

	@Test
	public void testBadResponseType() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(oauthClient.asIdentity())).thenReturn(oauthClient);
		try (Http1ClientResponse result =
				call(new AuthorizeForm("foo", oauthClient.asIdentity(), redirectUri, scope))) {
			assertDenied(result, OAuthController.UNSUPPORTED_RESPONSE_TYPE);
		}
	}

	@Test
	public void testUnknownClient() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(
				new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, oauthClient.asIdentity(), redirectUri, scope))) {
			assertDenied(result, OAuthController.UNAUTHORIZED_CLIENT);
		}
	}

	@Test
	public void testSuspendedClient() {
		oauthClient.setSuspended(true);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(oauthClient.asIdentity())).thenReturn(oauthClient);
		try (Http1ClientResponse result = call(
				new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, oauthClient.asIdentity(), redirectUri, scope))) {
			assertDenied(result, OAuthController.UNAUTHORIZED_CLIENT);
		}
	}

	@Test
	public void testUnverifiedClient() {
		oauthClient.setVerified(false);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(oauthClient.asIdentity())).thenReturn(oauthClient);
		try (Http1ClientResponse result = call(
				new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, oauthClient.asIdentity(), redirectUri, scope))) {
			assertDenied(result, OAuthController.UNAUTHORIZED_CLIENT);
		}
	}

	private Http1ClientResponse call(AuthorizeForm form) {
		return client.post("/oauth/authorize").submit(form.toJson());
	}
}
