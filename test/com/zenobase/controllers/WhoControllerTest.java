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
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserManager;

public class WhoControllerTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final UserManager users = mock(UserManager.class);

	@Before
	public void setUp() {
		WhoController.auth = auth;
		WhoController.users = users;
	}

	@After
	public void tearDown() {
		WhoController.auth = null;
		WhoController.users = null;
	}

	@Test
	public void testUnknown() {
		when(auth.getPrincipal()).thenReturn(null);
		Result result = callAction(com.zenobase.controllers.routes.ref.WhoController.who(), fakeRequest());
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
	}

	@Test
	public void testGuest() {
		Identity principal = new Identity();
		when(auth.getPrincipal()).thenReturn(principal);
		when(users.find(principal)).thenReturn(null);
		Result result = callAction(com.zenobase.controllers.routes.ref.WhoController.who(), fakeRequest());
		assertThat(result).hasStatus(OK).hasContent(principal.toJson());
	}

	@Test
	public void testUser() {
		User user = new User(Generator.id(), "tester");
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = callAction(com.zenobase.controllers.routes.ref.WhoController.who(), fakeRequest());
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}
}
