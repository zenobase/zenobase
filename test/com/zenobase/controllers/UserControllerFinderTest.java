package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserManager;

public class UserControllerFinderTest {

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
	public void testFindExistingUser() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = callAction(com.zenobase.controllers.routes.ref.UserController.find(user.getId(), 0, 1), fakeRequest());
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}
}
