package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.zenobase.commands.CreateUserCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.models.UserProfile;
import com.zenobase.oauth.Authorization;

public class AccountControllerOpenAccountTest extends AccountControllerTestSupport {

	@Test
	public void testSignUp() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		String commandId = Generator.id();
		ArgumentCaptor<CreateUserCommand> commandArg = ArgumentCaptor.forClass(CreateUserCommand.class);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(commandId);
		try (Http1ClientResponse result = call(form.toJson())) {
			User actual = commandArg.getValue().getUser();
			assertThat(result).hasStatus(201).hasContent(new UserProfile(actual).toJson());
			assertThat(actual.getName()).isEqualTo(user.getName());
			assertThat(actual.getEmail()).isEqualTo(user.getEmail());
			ArgumentCaptor<User> userArg = ArgumentCaptor.forClass(User.class);
			verify(mailer).send(userArg.capture());
			assertThat(userArg.getValue()).isEqualTo(actual);
			assertThat(result).hasHeader("Location", "/users/" + user.getName());
			assertThat(result).hasHeader(COMMAND_ID, commandId);
		}
	}

	@Test
	public void testSignUpExistingUser() {
		String commandId = Generator.id();
		when(users.exists(user.getName())).thenReturn(true);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(any(CreateUserCommand.class))).thenReturn(commandId);
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		try (Http1ClientResponse result = call(form.toJson())) {
			assertThat(result).hasStatus(409);
			verifyNoInteractions(dispatcher, mailer);
		}
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
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		SignUpForm form = new SignUpForm(username, password, user.getEmail());
		try (Http1ClientResponse result = call(form.toJson())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher, mailer);
		}
	}

	@Test
	public void testSignUpWithInvalidData() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		SignUpForm form = new SignUpForm(user.getName(), password, "x");
		try (Http1ClientResponse result = call(form.toJson())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher, mailer);
		}
	}

	@Test
	public void testSignUpUnauthorized() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.current(any())).thenReturn(null);
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		try (Http1ClientResponse result = call(form.toJson())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher, mailer);
		}
	}

	private Http1ClientResponse call(ObjectNode body) {
		return client.post("/users/").submit(body);
	}
}
