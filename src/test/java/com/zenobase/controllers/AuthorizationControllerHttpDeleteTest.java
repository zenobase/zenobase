package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;

import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationControllerHttpDeleteTest extends AuthorizationControllerTestSupport {

	private Authorization authorization = new Authorization(user.asIdentity(), null, Generator.id());

	@Test
	public void test() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(authorizations.find(authorization.getId())).thenReturn(authorization.copy());
		when(dispatcher.dispatch(any(DeleteAuthorizationCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(authorization.getId())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testAsSuperuser() {
		Identity superuser = new Identity();
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(authorizations.find(authorization.getId())).thenReturn(authorization.copy());
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(dispatcher.dispatch(any(DeleteAuthorizationCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(authorization.getId())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(authorization.getId())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUnauthorized() {
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		try (Http1ClientResponse result = call(authorization.getId())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		try (Http1ClientResponse result = call(authorization.getId())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(String id) {
		return client.delete("/authorizations/" + id).request();
	}
}
