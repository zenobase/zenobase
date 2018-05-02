package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationControllerHttpGetTest extends AuthorizationControllerTestSupport {

	private Authorization authorization = new Authorization(user.asIdentity(), null, Generator.id());

	@Test
	public void test() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		Result result = call(authorization.getId());
		assertThat(result).hasStatus(OK).hasContent(authorization.toJson());
	}

	@Test
	public void testNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(user.getName());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testUnauthorized() {
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		Result result = call(authorization.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		Result result = call(authorization.getId());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String id) {
		return callAction(com.zenobase.controllers.routes.ref.AuthorizationController.get(id));
	}
}
