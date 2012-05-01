package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.UserManager;

public class UserControllerFinderTest {

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
				bind(VerificationMailer.class).toInstance(mock(VerificationMailer.class)); // unused
				bind(CommandQueue.class).toInstance(mock(CommandQueue.class)); // unused
				requestStaticInjection(UserController.class);
			}
		});
	}

	@Test
	public void testFindExistingUser() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = callAction(com.zenobase.controllers.routes.ref.UserController.find(user.getId(), 0, 1), fakeRequest());
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
	}
}
