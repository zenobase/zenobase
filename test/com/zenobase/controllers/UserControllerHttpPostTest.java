package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;
import org.mockito.Matchers;
import play.mvc.Result;

import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserPasswordCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.models.User;

public class UserControllerHttpPostTest extends UserControllerTestSupport {

	@Test
	public void testUpdateEmail() {
		String commandId = Generator.id();
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(user);
		when(dispatcher.dispatch(any(ChangeUserEmailCommand.class))).thenReturn(commandId);
		Result result = call(user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(OK).hasContent(UserController.receipt(commandId));
	}

	@Test
	public void testUpdateUserNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		Result result = call(user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher, mailer);
	}

	@Test
	public void testUpdateEmailNotSignedIn() {
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher, mailer);
	}

	@Test
	public void testUpdateEmailDifferentUser() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher, mailer);
	}

	@Test
	public void testUpdateEmailWithInvalidAddress() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName(), new UpdateUserForm("jdoe").toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher, mailer);
	}

	@Test
	public void testUpdatePassword() {
		user.setPassword("secret123");
		String commandId = Generator.id();
		when(users.find(user.getName())).thenReturn(user);
		when(dispatcher.dispatch(Matchers.any(ChangeUserPasswordCommand.class))).thenReturn(commandId);
		PasswordResetKey key = new PasswordResetKey(user);
		Result result = call(user.getName(), new UpdateUserForm("newpassword",  key.getKey(), key.getExpirationToken()).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(auth).setPrincipal(user.asIdentity(), true);
	}

	@Test
	public void testUpdatePasswordWithInvalidPassword() {
		user.setPassword("secret123");
		when(users.find(user.getName())).thenReturn(user);
		PasswordResetKey key = new PasswordResetKey(user);
		Result result = call(user.getName(), new UpdateUserForm("123", key.getKey(), key.getExpirationToken()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth, dispatcher);
	}

	@Test
	public void testUpdatePasswordWithoutKey() {
		user.setPassword("secret123");
		when(users.find(user.getName())).thenReturn(user);
		PasswordResetKey key = new PasswordResetKey(user);
		Result result = call(user.getName(), new UpdateUserForm("newpassword", null, key.getExpirationToken()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth, dispatcher);
	}

	@Test
	public void testUpdatePasswordWithoutExpiresToken() {
		user.setPassword("secret123");
		when(users.find(user.getName())).thenReturn(user);
		PasswordResetKey key = new PasswordResetKey(user);
		Result result = call(user.getName(), new UpdateUserForm("newpassword",  key.getKey(), null).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth, dispatcher);
	}

	@Test
	public void testUpdatePasswordWithInvalidKey() {
		user.setPassword("secret123");
		when(users.find(user.getName())).thenReturn(user);
		User other = user.copy();
		other.setPassword("123secret");
		PasswordResetKey key = new PasswordResetKey(other);
		Result result = call(user.getName(), new UpdateUserForm("newpassword",  key.getKey(), key.getExpirationToken()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth, dispatcher);
	}

	@Test
	public void testUpdateVerified() {
		user.setEmail("jdoe@zenobase.com");
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(user);
		EmailVerificationKey key = new EmailVerificationKey(user.getName(), user.getEmail());
		Result result = call(user.getName(), new UpdateUserForm(Boolean.TRUE, key.getKey()).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(dispatcher).dispatch(Matchers.any(ChangeUserVerifiedCommand.class));
	}

	@Test
	public void testUpdateVerifiedWhenAlreadyVerified() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(true);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(user);
		EmailVerificationKey key = new EmailVerificationKey(user.getName(), user.getEmail());
		Result result = call(user.getName(), new UpdateUserForm(Boolean.TRUE, key.getKey()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateVerifiedWithoutKey() {
		user.setEmail("jdoe@zenobase.com");
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName(), new UpdateUserForm(Boolean.TRUE, null).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateVerifiedWithInvalidKey() {
		user.setEmail("jdoe@zenobase.com");
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(user);
		EmailVerificationKey key = new EmailVerificationKey(user.getName(), "jdoe@zenobase.org");
		Result result = call(user.getName(), new UpdateUserForm(Boolean.TRUE, key.getKey()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateNothing() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName(), new UpdateUserForm(Nodes.newObject()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(String name, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.UserController.update(name), fakeRequest().withJsonBody(body));
	}
}
