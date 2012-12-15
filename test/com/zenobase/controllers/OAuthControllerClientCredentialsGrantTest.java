package com.zenobase.controllers;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.test.Helpers.*;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.oauth.Authorization;

public class OAuthControllerClientCredentialsGrantTest extends OAuthControllerTestSupport {

	@Test
	public void test() {
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c");
		Result result = call(new TokenForm(OAuthController.GRANT_TYPE_CLIENT_CREDENTIALS, null, null));
		assertGranted(result);
		Authorization auth = arg.getValue().getAuthorization();
		assertThat(auth.getId()).isNotNull();
		assertThat(auth.getPrincipal()).isNotNull();
		assertThat(auth.getClient()).isNull();
		assertThat(auth.getScope()).isNull();
	}

	@Test
	public void testWithJson() {
		ArgumentCaptor<CreateAuthorizationCommand> arg = ArgumentCaptor.forClass(CreateAuthorizationCommand.class);
		when(dispatcher.dispatch(arg.capture())).thenReturn("c");
		Result result = call(new TokenForm(OAuthController.GRANT_TYPE_CLIENT_CREDENTIALS, null, null).toJson());
		assertGranted(result);
		Authorization auth = arg.getValue().getAuthorization();
		assertThat(auth.getId()).isNotNull();
		assertThat(auth.getPrincipal()).isNotNull();
		assertThat(auth.getClient()).isNull();
		assertThat(auth.getScope()).isNull();
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
