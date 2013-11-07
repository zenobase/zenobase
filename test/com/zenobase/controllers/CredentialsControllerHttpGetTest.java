package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Credentials;

public class CredentialsControllerHttpGetTest extends CredentialsControllerTestSupport {

	private final String type = "test";
	private final Credentials credentials = new Credentials(type, principal);

	@Test
	public void test() {
		when(auth.current()).thenReturn(new Authorization(principal));
		when(repository.find(credentials.getId())).thenReturn(credentials);
		Result result = call(credentials.getId());
		assertThat(result).hasStatus(OK).hasContent(credentials.toJson());
	}

	@Test
	public void testUnauthorized() {
		Result result = call(credentials.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testNotFound() {
		when(auth.current()).thenReturn(new Authorization(principal));
		Result result = call(credentials.getId());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(repository.find(credentials.getId())).thenReturn(credentials);
		Result result = call(credentials.getId());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String credentialsId) {
		return callAction(com.zenobase.controllers.routes.ref.CredentialsController.get(credentialsId));
	}
}
