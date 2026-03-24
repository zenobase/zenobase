package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Credentials;

public class CredentialsControllerHttpDeleteTest extends CredentialsControllerTestSupport {

	private final String type = "test";
	private final Credentials credentials = new Credentials(type, principal);

	@Test
	public void test() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(principal));
		when(repository.find(credentials.getId())).thenReturn(credentials.copy());
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(credentials.getId())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testUnauthorized() {
		try (Http1ClientResponse result = call(credentials.getId())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		try (Http1ClientResponse result = call(credentials.getId())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(repository.find(credentials.getId())).thenReturn(credentials);
		try (Http1ClientResponse result = call(credentials.getId())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testAsSuperuser() {
		Identity superuser = new Identity();
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(repository.find(credentials.getId())).thenReturn(credentials.copy());
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(credentials.getId())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	private Http1ClientResponse call(String credentialsId) {
		return client.delete("/credentials/" + credentialsId).request();
	}
}
