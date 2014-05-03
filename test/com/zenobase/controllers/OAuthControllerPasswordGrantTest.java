package com.zenobase.controllers;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;

public class OAuthControllerPasswordGrantTest extends OAuthControllerTestSupport {

	private User user = new User("me");
	private String password = "secret123";

	@Before
	@Override
	public void setUp() {
		super.setUp();
		user.setVerified(true);
		user.setSuspended(false);
		user.setPassword(password);
	}

	@Test
	public void test() {
		when(users.find(user.getName())).thenReturn(user);
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c");
		Result result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), password));
		assertGranted(result);
		assertExpires(result, 31 * 24 * 60 * 60);
		Authorization auth = arg.getValue().getAuthorization();
		assertThat(auth.getId()).isNotNull();
		assertThat(auth.getPrincipal()).isEqualTo(user.asIdentity());
		assertThat(auth.getClient()).isNull();
		assertThat(auth.getScope()).isNull();
	}

	@Test
	public void testWithJson() {
		when(users.find(user.getName())).thenReturn(user);
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c");
		Result result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), password).toJson());
		assertGranted(result);
		assertExpires(result, 31 * 24 * 60 * 60);
		Authorization auth = arg.getValue().getAuthorization();
		assertThat(auth.getId()).isNotNull();
		assertThat(auth.getPrincipal()).isEqualTo(user.asIdentity());
		assertThat(auth.getClient()).isNull();
		assertThat(auth.getScope()).isNull();
	}

	@Test
	public void testMissingUsername() {
		Result result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, null, password));
		assertDenied(result, OAuthController.INVALID_REQUEST);
	}

	@Test
	public void testMissingPassword() {
		Result result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), null));
		assertDenied(result, OAuthController.INVALID_REQUEST);
	}

	@Test
	public void testUnknownUser() {
		Result result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), password));
		assertDenied(result, OAuthController.ACCESS_DENIED);
	}

	@Test
	public void testSuspendedUser() {
		user.setSuspended(true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), password));
		assertDenied(result, OAuthController.ACCESS_DENIED);
	}

	@Test
	public void testBadPassword() {
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(new TokenForm(OAuthController.GRANT_TYPE_PASSWORD, user.getName(), "forgotten"));
		assertDenied(result, OAuthController.ACCESS_DENIED);
	}

	@Test
	public void testBadGrantType() {
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(new TokenForm("foo", user.getName(), password));
		assertDenied(result, OAuthController.UNSUPPORTED_GRANT_TYPE);
	}

	private Result call(TokenForm form) {
		return callAction(com.zenobase.controllers.routes.ref.OAuthController.token(), fakeRequest()
			.withFormUrlEncodedBody(form.toMap()));
	}

	private Result call(JsonNode node) {
		return callAction(com.zenobase.controllers.routes.ref.OAuthController.token(), fakeRequest()
			.withJsonBody(node));
	}
}
