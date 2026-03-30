package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AccountControllerCloseAccountTest extends AccountControllerTestSupport {

	@Test
	public void testCloseAccount() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
			verify(dispatcher).dispatch(any(Command.class));
		}
	}

	@Test
	public void testCloseAccountNotSignedIn() {
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testCloseAccountNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testCloseAccountForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testCloseAccountSignedInAsSuperuser() {
		Identity superuser = new Identity();
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.find(user.getName())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
			verify(dispatcher).dispatch(any(Command.class));
		}
	}

	private Http1ClientResponse call(String username) {
		return client.delete("/users/@" + username).request();
	}
}
