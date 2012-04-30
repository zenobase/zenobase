package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserProfile;
import com.zenobase.services.UserManager;

public class UserControllerTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final UserManager users = mock(UserManager.class);
	private final User user = new User(Generator.id(), "tester");

	@Before
	public void setUp() {
		UserController.auth = auth;
		UserController.users = users;
	}

	@After
	public void tearDown() {
		UserController.auth = null;
		UserController.users = null;
	}

	@Test
	public void testGetExistingUser() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(user);
		Result result = callAction(com.zenobase.controllers.routes.ref.UserController.get(user.getName()), fakeRequest());
		assertThat(result).hasStatus(OK).hasContent(new UserProfile(user).toJson());
	}

	@Test
	public void testGetMissingUser() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(null);
		Result result = callAction(com.zenobase.controllers.routes.ref.UserController.get(user.getName()), fakeRequest());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetUserUnauthorized() {
		when(users.find(user.getName())).thenReturn(user);
		Result result = callAction(com.zenobase.controllers.routes.ref.UserController.get(user.getName()), fakeRequest());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetUserForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(users.find(user.getName())).thenReturn(user);
		Result result = callAction(com.zenobase.controllers.routes.ref.UserController.get(user.getName()), fakeRequest());
		assertThat(result).hasStatus(FORBIDDEN);
	}
}
