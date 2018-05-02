package com.zenobase.controllers;

import static com.zenobase.testing.CallbackAnswer.doCallback;
import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.mockito.Matchers;
import play.mvc.Result;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.OptOutCommand;
import com.zenobase.common.Callback;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationQuery;

public class UserControllerHttpPostTest extends UserControllerTestSupport {

	@Test
	public void testUpdateEmail() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(ChangeUserEmailCommand.class))).thenReturn(commandId);
		Result result = call(user.getId(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
		verifyZeroInteractions(payments);
	}

	@Test
	public void testOptOut() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(OptOutCommand.class))).thenReturn(commandId);
		Result result = call(user.getId(), UpdateUserForm.withOptedOut(true).toJson());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
		verifyZeroInteractions(mailer, payments);
	}

	@Test
	public void testOptIn() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(OptOutCommand.class))).thenReturn(commandId);
		Result result = call(user.getId(), UpdateUserForm.withOptedOut(false).toJson());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
		verifyZeroInteractions(mailer, payments);
	}

	@Test
	public void testUpdateUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call('@' + user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher, mailer, payments);
	}

	@Test
	public void testUpdateEmailNotSignedIn() {
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher, mailer, payments);
	}

	@Test
	public void testUpdateEmailDifferentUser() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), new UpdateUserForm("jdoe@zenobase.com").toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher, mailer, payments);
	}

	@Test
	public void testUpdateEmailWithInvalidAddress() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), new UpdateUserForm("jdoe").toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher, mailer, payments);
	}

	@Test
	public void testUpdatePassword() {
		user.setPassword("secret123");
		String commandId = Generator.id();
		when(users.find(user.asIdentity())).thenReturn(user);
		AuthorizationQuery query = new AuthorizationQuery().principalEqualTo(user.asIdentity()).clientIsNull();
		doCallback(new Authorization(new Identity())).when(authorizations).find(eq(query), any(Callback.class));
		when(dispatcher.dispatch(Matchers.any(CompoundCommand.class))).thenReturn(commandId);
		PasswordResetKey key = new PasswordResetKey(user);
		Result result = call(user.getId(), new UpdateUserForm("newpassword",  key.getKey(), key.getExpirationToken()).toJson());
		assertThat(result).hasStatus(OK).hasHeader(COMMAND_ID, commandId).asObjectNode().path("access_code").isNotNull();
		verifyZeroInteractions(payments);
	}

	@Test
	public void testUpdatePasswordWithInvalidPassword() {
		user.setPassword("secret123");
		when(users.find(user.asIdentity())).thenReturn(user);
		PasswordResetKey key = new PasswordResetKey(user);
		Result result = call(user.getId(), new UpdateUserForm("123", key.getKey(), key.getExpirationToken()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth, dispatcher, payments);
	}

	@Test
	public void testUpdatePasswordWithoutKey() {
		user.setPassword("secret123");
		when(users.find(user.asIdentity())).thenReturn(user);
		PasswordResetKey key = new PasswordResetKey(user);
		Result result = call(user.getId(), new UpdateUserForm("newpassword", null, key.getExpirationToken()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth, dispatcher, payments);
	}

	@Test
	public void testUpdatePasswordWithoutExpiresToken() {
		user.setPassword("secret123");
		when(users.find(user.asIdentity())).thenReturn(user);
		PasswordResetKey key = new PasswordResetKey(user);
		Result result = call(user.getId(), new UpdateUserForm("newpassword",  key.getKey(), null).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth, dispatcher, payments);
	}

	@Test
	public void testUpdatePasswordWithInvalidKey() {
		user.setPassword("secret123");
		when(users.find(user.asIdentity())).thenReturn(user);
		User other = user.copy();
		other.setPassword("123secret");
		PasswordResetKey key = new PasswordResetKey(other);
		Result result = call(user.getId(), new UpdateUserForm("newpassword",  key.getKey(), key.getExpirationToken()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(auth, dispatcher, payments);
	}

	@Test
	public void testUpdateVerified() {
		user.setEmail("jdoe@zenobase.com");
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		EmailVerificationKey key = new EmailVerificationKey(user.getName(), user.getEmail());
		Result result = call(user.getId(), new UpdateUserForm(Boolean.TRUE, key.getKey()).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(dispatcher).dispatch(Matchers.any(ChangeUserVerifiedCommand.class));
		verify(payments).update(user.getName(), "jdoe@zenobase.com");
	}

	@Test
	public void testUpdateVerifiedWhenAlreadyVerified() {
		user.setEmail("jdoe@zenobase.com");
		user.setVerified(true);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		EmailVerificationKey key = new EmailVerificationKey(user.getName(), user.getEmail());
		Result result = call(user.getId(), new UpdateUserForm(Boolean.TRUE, key.getKey()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher, payments);
	}

	@Test
	public void testUpdateVerifiedWithoutKey() {
		user.setEmail("jdoe@zenobase.com");
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), new UpdateUserForm(Boolean.TRUE, null).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher, payments);
	}

	@Test
	public void testUpdateVerifiedWithInvalidKey() {
		user.setEmail("jdoe@zenobase.com");
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		EmailVerificationKey key = new EmailVerificationKey(user.getName(), "jdoe@zenobase.org");
		Result result = call(user.getId(), new UpdateUserForm(Boolean.TRUE, key.getKey()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher, payments);
	}

	@Test
	public void testUpdateQuota() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(dispatcher.dispatch(any(ChangeQuotaCommand.class))).thenReturn(commandId);
		Result result = call(user.getId(), new UpdateUserForm(50000).toJson());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testUpdateQuotaForbidden() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), new UpdateUserForm(50000).toJson());
		assertThat(result).hasStatus(FORBIDDEN).isEmpty();
	}

	@Test
	public void testUpdateSuspension() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(user.getId(), new UpdateUserForm(true).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(dispatcher).dispatch(Matchers.any(ChangeUserVerifiedCommand.class));
	}

	@Test
	public void testUpdateSuspensionForbidden() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), new UpdateUserForm(true).toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher, payments);
	}

	@Test
	public void testUpdateNothing() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(user.getId(), new UpdateUserForm(Nodes.newObject()).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher, payments);
	}

	private static Result call(String userId, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.UserController.update(userId), fakeRequest().withJsonBody(body));
	}
}
