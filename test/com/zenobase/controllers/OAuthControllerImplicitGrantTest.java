package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;

public class OAuthControllerImplicitGrantTest extends OAuthControllerTestSupport {

	private User user = new User("me");
	private User client = new User("zeno");
	private String redirectUri = "https://zenobase.com/callback";
	private String scope = Generator.id();

	@Before
	@Override
	public void setUp() {
		super.setUp();
		client.setVerified(true);
		client.setSuspended(false);
		client.setEmail("info@zenobase.com");
	}

	@Test
	public void test() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(client.asIdentity())).thenReturn(client);
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c1");
		Result result = call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, client.asIdentity(), redirectUri, scope));
		assertGranted(result);
		Authorization auth = arg.getValue().getAuthorization();
		assertThat(auth.getId()).isNotNull();
		assertThat(auth.getPrincipal()).isEqualTo(user.asIdentity());
		assertThat(auth.getClient()).isEqualTo(client.asIdentity());
		assertThat(auth.getScope()).isEqualTo(scope);
	}

	@Test
	public void testUnauthorized() {
		Result result = call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, client.asIdentity(), redirectUri, scope));
		assertThat(result).hasStatus(UNAUTHORIZED).isEmpty();
	}

	@Test
	public void testMissingClient() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(client.asIdentity())).thenReturn(client);
		Result result = call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, null, redirectUri, scope));
		assertDenied(result, OAuthController.INVALID_REQUEST);
	}

	@Test
	public void testMissingRedirectUri() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(client.asIdentity())).thenReturn(client);
		Result result = call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, client.asIdentity(), null, scope));
		assertDenied(result, OAuthController.INVALID_REQUEST);
	}

	@Test
	public void testBadRedirectUri() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(client.asIdentity())).thenReturn(client);
		Result result = call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, client.asIdentity(), "http://foo.test/", scope));
		assertDenied(result, OAuthController.INVALID_REDIRECT_URI);
	}

	@Test
	public void testMissingScope() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(client.asIdentity())).thenReturn(client);
		Result result = call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, client.asIdentity(), redirectUri, null));
		assertDenied(result, OAuthController.INVALID_SCOPE);
	}

	@Test
	public void testBadResponseType() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(client.asIdentity())).thenReturn(client);
		Result result = call(new AuthorizeForm("foo", client.asIdentity(), redirectUri, scope));
		assertDenied(result, OAuthController.UNSUPPORTED_RESPONSE_TYPE);
	}

	@Test
	public void testUnknownClient() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, client.asIdentity(), redirectUri, scope));
		assertDenied(result, OAuthController.UNAUTHORIZED_CLIENT);
	}

	@Test
	public void testSuspendedClient() {
		client.setSuspended(true);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(client.asIdentity())).thenReturn(client);
		Result result = call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, client.asIdentity(), redirectUri, scope));
		assertDenied(result, OAuthController.UNAUTHORIZED_CLIENT);
	}

	@Test
	public void testUnverifiedClient() {
		client.setVerified(false);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(client.asIdentity())).thenReturn(client);
		Result result = call(new AuthorizeForm(OAuthController.RESPONSE_TYPE_TOKEN, client.asIdentity(), redirectUri, scope));
		assertDenied(result, OAuthController.UNAUTHORIZED_CLIENT);
	}

	private Result call(AuthorizeForm form) {
		return callAction(com.zenobase.controllers.routes.ref.OAuthController.authorize(), fakeRequest().withJsonBody(form.toJson()));
	}
}
