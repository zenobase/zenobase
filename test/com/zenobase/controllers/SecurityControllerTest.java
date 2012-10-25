package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Cookie;
import play.mvc.Result;
import play.test.Helpers;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.UserRepository;

public class SecurityControllerTest {

	private final SecurityContext auth = new SecurityContext("secret");
	private final UserRepository users = mock(UserRepository.class);
	private final User user = new User("tester");
	private final String password = "secret123";

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				requestStaticInjection(SecurityController.class);
			}
		});
		user.setPassword(password);
	}

	@Test
	public void testSignIn() {
		fakeApplication();
		SignInForm form = new SignInForm(user.getName(), password, true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		ObjectNode expected = new UserInfo(user).toJson();
		expected.put("hash", auth.sign(user.getId()));
		assertThat(result).hasStatus(OK).hasContent(expected);
		assertThat(cookie(result)).as("auth cookie").isNotNull();
		assertThat(auth.getPrincipal(cookie(result))).isEqualTo(user.asIdentity());
	}

	@Test
	public void testSignInWithAuthHeader() {
		String header = "zeno id=\"" + user.getId() + "\", hash=\"" + auth.sign(user.getId()) + "\"";
		assertThat(auth.getPrincipal(header)).isEqualTo(user.asIdentity());
	}

	@Test
	public void testSignInMissingUsername() {
		SignInForm form = new SignInForm(null, password, true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		assertThat(cookie(result)).as("auth cookie").isNull();
	}

	@Test
	public void testSignInMissingPassword() {
		SignInForm form = new SignInForm(user.getName(), null, true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		assertThat(cookie(result)).as("auth cookie").isNull();
	}

	@Test
	public void testSignInUnknownUser() {
		SignInForm form = new SignInForm(user.getName(), password, true);
		when(users.find(user.getName())).thenReturn(null);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		assertThat(cookie(result)).as("auth cookie").isNull();
	}

	@Test
	public void testSignInSuspendedUser() {
		user.setSuspended(true);
		SignInForm form = new SignInForm(user.getName(), password, true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		assertThat(cookie(result)).as("auth cookie").isNull();
	}

	@Test
	public void testSignInBadPassword() {
		user.setSuspended(true);
		SignInForm form = new SignInForm(user.getName(), "123secret", true);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(form.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		assertThat(cookie(result)).as("auth cookie").isNull();
	}

	@Test
	public void testSignOut() {
		Result result = call();
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
		assertThat(cookie(result)).as("auth cookie").isNotNull();
		assertThat(cookie(result).value()).as("auth cookie value").isEmpty();
	}

	private static Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.SecurityController.signIn(), fakeRequest().withJsonBody(body));
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.SecurityController.signOut());
	}

	private static Cookie cookie(Result result) {
		return Helpers.cookie(SecurityContext.TOKEN_NAME, result);
	}
}
