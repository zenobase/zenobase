package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserManager;

public class SecurityControllerTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final UserManager users = mock(UserManager.class);
	private final User user = new User(Generator.id(), "tester");
	private final String password = "secret123";

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(UserManager.class).toInstance(users);
				requestStaticInjection(SecurityController.class);
			}
		});
		user.setPassword(password);
	}

	@Test
	public void testSignIn() {
		SignInForm form = new SignInForm(user.getName(), password, true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(OK).hasContent(new UserInfo(user).toJson());
		verify(auth).setPrincipal(user.asIdentity(), form.isRemember());
	}

	@Test
	public void testSignInMissingUsername() {
		SignInForm form = new SignInForm(null, password, true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth);
	}

	@Test
	public void testSignInMissingPassword() {
		SignInForm form = new SignInForm(user.getName(), null, true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth);
	}

	@Test
	public void testSignInUnknownUser() {
		SignInForm form = new SignInForm(user.getName(), password, true);
		when(users.find(user.getName())).thenReturn(null);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(auth);
	}

	@Test
	public void testSignInSuspendedUser() {
		user.setSuspended(true);
		SignInForm form = new SignInForm(user.getName(), password, true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(auth);
	}

	@Test
	public void testSignInBadPassword() {
		user.setSuspended(true);
		SignInForm form = new SignInForm(user.getName(), "123secret", true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(auth);
	}

	@Test
	public void testSignOut() {
		Result result = call();
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
		verify(auth).unsetPrincipal();
	}

	private static Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.SecurityController.signIn(), fakeRequest().withJsonBody(body));
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.SecurityController.signOut());
	}
}
