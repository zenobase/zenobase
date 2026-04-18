package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.OptInCommand;
import com.zenobase.commands.OptOutCommand;
import com.zenobase.commands.SuspendUserCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class UserControllerHttpPostTest extends UserControllerTestSupport {

	@Test
	public void testUpdateEmail() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(ChangeUserEmailCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(user.getId(), new UpdateUserForm("jdoe@zenobase.com").toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testOptOut() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(OptOutCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(user.getId(), UpdateUserForm.withOptedOut(true).toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testOptIn() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(OptInCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(user.getId(), UpdateUserForm.withOptedOut(false).toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testUpdateUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (
			Http1ClientResponse result = call('@' + user.getName(), new UpdateUserForm("jdoe@zenobase.com").toJson())
		) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateEmailNotSignedIn() {
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId(), new UpdateUserForm("jdoe@zenobase.com").toJson())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateEmailDifferentUser() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId(), new UpdateUserForm("jdoe@zenobase.com").toJson())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateEmailWithInvalidAddress() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId(), new UpdateUserForm("").toJson())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateQuota() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(dispatcher.dispatch(any(ChangeQuotaCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(user.getId(), new UpdateUserForm(50000).toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testUpdateQuotaForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId(), new UpdateUserForm(50000).toJson())) {
			assertThat(result).hasStatus(403).isEmpty();
		}
	}

	@Test
	public void testUpdateSuspension() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(dispatcher.dispatch(any(SuspendUserCommand.class))).thenReturn(Generator.id());
		try (Http1ClientResponse result = call(user.getId(), new UpdateUserForm(true).toJson())) {
			assertThat(result).hasStatus(204);
			verify(dispatcher).dispatch(ArgumentMatchers.any(SuspendUserCommand.class));
		}
	}

	@Test
	public void testUpdateSuspensionForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId(), new UpdateUserForm(true).toJson())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateNothing() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId(), new UpdateUserForm(Nodes.newObject()).toJson())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(String userId, ObjectNode body) {
		return client.post("/users/" + userId).submit(body);
	}
}
