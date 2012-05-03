package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;

import com.zenobase.commands.CreateUserCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;

public class AccountControllerOpenAccountTest extends AccountControllerTestSupport {

	@Test
	public void testSignUp() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		Result result = call(form.toJson());
		assertThat(result).hasStatus(CREATED).hasContent(new UserInfo(user).toJson());
		ArgumentCaptor<CreateUserCommand> commandArg = ArgumentCaptor.forClass(CreateUserCommand.class);
		verify(queue).dispatch(commandArg.capture());
		User actual = commandArg.getValue().getUser();
		assertThat(actual.getName()).isEqualTo(user.getName());
		assertThat(actual.getEmail()).isEqualTo(user.getEmail());
		ArgumentCaptor<User> userArg = ArgumentCaptor.forClass(User.class);
		verify(mailer).send(userArg.capture());
		assertThat(userArg.getValue()).isEqualTo(actual);
	}

	@Test
	public void testSignUpExistingUser() {
		String commandId = Generator.id();
		when(users.exists(user.getName())).thenReturn(true);
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		when(queue.dispatch(any(CreateUserCommand.class))).thenReturn(commandId);
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		Result result = call(form.toJson());
		assertThat(result).hasStatus(CONFLICT);
		verifyZeroInteractions(queue, mailer);
	}

	@Test
	public void testSignUpGuest() {
		String username = "guest";
		when(users.exists(username)).thenReturn(false);
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		SignUpForm form = new SignUpForm(username, password, user.getEmail());
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(queue, mailer);
	}

	@Test
	public void testSignUpWithInvalidData() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		SignUpForm form = new SignUpForm(user.getName(), password, "x");
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(queue, mailer);
	}

	private Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.AccountController.open(), fakeRequest().withJsonBody(body));
	}
}
