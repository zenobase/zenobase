package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.Identity;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;

public class UserControllerHttpGetTest extends UserControllerTestSupport {

	@Test
	public void testGetUser() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(OK).hasContent(new UserProfile(user).toJson());
	}

	@Test
	public void testGetUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(user.getName());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetUserUnauthorized() {
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetUserForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String name) {
		return callAction(com.zenobase.controllers.routes.ref.UserController.get(name));
	}
}
