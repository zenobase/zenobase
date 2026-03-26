package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;

import com.zenobase.commands.Command;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsManager;
import com.zenobase.tasks.OAuthCredentials;

public class CredentialsControllerHttpPostTest extends CredentialsControllerTestSupport {

	private final CredentialsManager manager = mock(CredentialsManager.class);
	private final Command command = new Command(new Command.Type("test", 1), principal);
	private OAuthCredentials from;
	private ObjectNode update = Nodes.newObject();

	@BeforeEach
	public void setUp() {
		from = new OAuthCredentials("test", principal);
		from.setAuthorizationUrl("http://localhost/");
		Credentials.CREDENTIALS.setValue(update, Nodes.newObject("foo", "bar"));
	}

	@Test
	public void test() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		when(manager.authorize(from, Credentials.CREDENTIALS.getValue(update))).thenReturn(command);
		when(dispatcher.dispatch(command)).thenReturn(command.getId());
		try (Http1ClientResponse result = call(from.getId(), update)) {
			assertThat(result)
					.hasStatus(204)
					.hasHeader(COMMAND_ID, command.getId())
					.isEmpty();
		}
	}

	@Test
	public void testUnauthorized() {
		try (Http1ClientResponse result = call(from.getId(), update)) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		try (Http1ClientResponse result = call(from.getId(), update)) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(repository.find(from.getId())).thenReturn(from.copy());
		try (Http1ClientResponse result = call(from.getId(), update)) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testInvalidType() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		try (Http1ClientResponse result = call(from.getId(), update)) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testEmptyBody() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		try (Http1ClientResponse result = call(from.getId(), Nodes.newObject())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testMissingCredentials() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		try (Http1ClientResponse result = call(from.getId(), Nodes.newObject("foo", "bar"))) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testAlreadyAuthorized() {
		from.setAuthorizationUrl(null);
		when(auth.current(any())).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		try (Http1ClientResponse result = call(from.getId(), update)) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testInvalidToken() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		when(manager.authorize(from, update)).thenReturn(null);
		try (Http1ClientResponse result = call(from.getId(), update)) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConflict() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		when(manager.authorize(from, Credentials.CREDENTIALS.getValue(update))).thenReturn(command);
		when(dispatcher.dispatch(command))
				.thenThrow(new OpenSearchException(ErrorResponse.of(r -> r.status(409)
						.error(e2 ->
								e2.type("version_conflict_engine_exception").reason("version conflict")))));
		try (Http1ClientResponse result = call(from.getId(), update)) {
			assertThat(result).hasStatus(409);
		}
	}

	private Http1ClientResponse call(String credentialsId, ObjectNode body) {
		return client.post("/credentials/" + credentialsId).submit(body);
	}
}
