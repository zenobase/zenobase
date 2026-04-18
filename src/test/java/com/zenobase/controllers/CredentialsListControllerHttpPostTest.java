package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsManager;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

public class CredentialsListControllerHttpPostTest extends CredentialsListControllerTestSupport {

	private final String type = "foo";
	private final CreateCredentialsForm form = new CreateCredentialsForm(type);

	@Test
	public void test() {
		CredentialsManager manager = mock(CredentialsManager.class);
		Credentials credentials = new Credentials(type, user.asIdentity());
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(registry.exists(type)).thenReturn(true);
		when(registry.find(type)).thenReturn(manager);
		when(manager.newCredentials(user.asIdentity())).thenReturn(credentials);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(form)) {
			assertThat(result).hasStatus(201);
			assertThat(result).hasHeader("Location", "/credentials/" + credentials.getId());
			assertThat(result).hasHeader(COMMAND_ID, commandId);
		}
	}

	@Test
	public void testNotAuthorized() {
		try (Http1ClientResponse result = call(form)) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testInvalidBody() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(new CreateCredentialsForm(Nodes.newObject()))) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testInvalidType() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(new CreateCredentialsForm("bar"))) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDuplicate() {
		CredentialsManager manager = mock(CredentialsManager.class);
		Credentials credentials = new Credentials(type, user.asIdentity());
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(registry.exists(type)).thenReturn(true);
		when(registry.find(type)).thenReturn(manager);
		when(repository.find(user.asIdentity(), type)).thenReturn(credentials);
		try (Http1ClientResponse result = call(form)) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(CreateCredentialsForm form) {
		return client.post("/credentials/").submit(form.toJson());
	}
}
