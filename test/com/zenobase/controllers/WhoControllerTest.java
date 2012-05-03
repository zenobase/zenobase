package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;

import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserManager;

public class WhoControllerTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final UserManager users = mock(UserManager.class);
	private final User user = new User(Generator.id(), "tester");

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(UserManager.class).toInstance(users);
				requestStaticInjection(WhoController.class);
			}
		});
	}

	@Test
	public void testUnknown() {
		when(auth.getPrincipal()).thenReturn(null);
		Result result = call();
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
	}

	@Test
	public void testGuest() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(null);
		Result result = call();
		assertThat(result).hasStatus(OK).hasContent(user.asIdentity().toJson());
	}

	@Test
	public void testUser() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call();
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.WhoController.who(), fakeRequest());
	}
}
