package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;
import play.test.Helpers;

import com.zenobase.commands.CreateUserCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;

public class AccountControllerOpenAccountTest extends AccountControllerTestSupport {

	@Test
	public void testSignUp() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		String commandId = Generator.id();
		ArgumentCaptor<CreateUserCommand> commandArg = ArgumentCaptor.forClass(CreateUserCommand.class);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(commandId);
		Result result = call(form.toJson());
		User actual = commandArg.getValue().getUser();
		assertThat(result).hasStatus(CREATED).hasContent(new UserProfile(actual).toJson());
		assertThat(actual.getName()).isEqualTo(user.getName());
		assertThat(actual.getEmail()).isEqualTo(user.getEmail());
		ArgumentCaptor<User> userArg = ArgumentCaptor.forClass(User.class);
		verify(mailer).send(userArg.capture());
		assertThat(userArg.getValue()).isEqualTo(actual);
		assertThat(Helpers.redirectLocation(result)).isEqualTo(com.zenobase.controllers.routes.UserController.get(user.getName()).toString());
		assertThat(Helpers.header(COMMAND_ID, result)).isEqualTo(commandId);
	}

	@Test
	public void testSignUpExistingUser() {
		String commandId = Generator.id();
		when(users.exists(user.getName())).thenReturn(true);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(any(CreateUserCommand.class))).thenReturn(commandId);
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		Result result = call(form.toJson());
		assertThat(result).hasStatus(CONFLICT);
		verifyZeroInteractions(dispatcher, mailer);
	}

	@Test
	public void testSignUpGuest() {
		testSignUpWithBadName("guest");
	}

	@Test
	public void testSignUpZeno() {
		testSignUpWithBadName("iamzenobase");
	}

	@Test
	public void testSignUpAdmin() {
		testSignUpWithBadName("admin");
	}

	private void testSignUpWithBadName(String username) {
		when(users.exists(username)).thenReturn(false);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		SignUpForm form = new SignUpForm(username, password, user.getEmail());
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher, mailer);
	}

	@Test
	public void testSignUpWithInvalidData() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		SignUpForm form = new SignUpForm(user.getName(), password, "x");
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher, mailer);
	}

	@Test
	public void testSignUpUnauthorized() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.current()).thenReturn(null);
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		Result result = call(form.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher, mailer);
	}

	private Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.AccountController.open(), fakeRequest().withJsonBody(body));
	}
}
