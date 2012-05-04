package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;
import org.mockito.Matchers;
import play.mvc.Result;

import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserPasswordCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;

public class UserControllerUpdateUserTest extends UserControllerTestSupport {

	@Test
	public void testUpdateEmail() {
		String commandId = Generator.id();
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(user);
		when(queue.dispatch(Matchers.any(ChangeUserEmailCommand.class))).thenReturn(commandId);
		Result result = call(user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(OK).hasContent(UserController.receipt(commandId));
	}

	@Test
	public void testUserNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(null);
		Result result = call(user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(queue, mailer);
	}

	@Test
	public void testUpdateEmailUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(queue, mailer);
	}

	@Test
	public void testUpdateEmailForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(queue, mailer);
	}

	@Test
	public void testUpdatePassword() {
		user.setPassword("secret123");
		String commandId = Generator.id();
		when(users.find(user.getName())).thenReturn(user);
		when(queue.dispatch(Matchers.any(ChangeUserPasswordCommand.class))).thenReturn(commandId);
		PasswordResetKey key = new PasswordResetKey(user);
		Result result = call(user.getName(), new UpdateUserForm("newpassword",  key.getKey(), key.getExpirationToken()).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(auth).setPrincipal(user.asIdentity(), true);
	}

	private static Result call(String name, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.UserController.update(name), fakeRequest().withJsonBody(body));
	}
}
