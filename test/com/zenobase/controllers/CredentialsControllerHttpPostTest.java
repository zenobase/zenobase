package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.rest.RestStatus;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

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

	@Before
	public void setUp() {
		from = new OAuthCredentials("test", principal);
		from.setAuthorizationUrl("http://localhost/");
		Credentials.CREDENTIALS.setValue(update, Nodes.newObject("foo", "bar"));
	}

	@Test
	public void test() {
		when(auth.current()).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		when(manager.authorize(from, Credentials.CREDENTIALS.getValue(update))).thenReturn(command);
		when(dispatcher.dispatch(command)).thenReturn(command.getId());
		Result result = call(from.getId(), update);
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, command.getId()).isEmpty();
	}

	@Test
	public void testUnauthorized() {
		Result result = call(from.getId(), update);
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testNotFound() {
		when(auth.current()).thenReturn(new Authorization(principal));
		Result result = call(from.getId(), update);
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(repository.find(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), update);
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testInvalidType() {
		when(auth.current()).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), update);
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testEmptyBody() {
		when(auth.current()).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		Result result = call(from.getId(), Nodes.newObject());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testMissingCredentials() {
		when(auth.current()).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		Result result = call(from.getId(), Nodes.newObject("foo", "bar"));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testAlreadyAuthorized() {
		from.setAuthorizationUrl(null);
		when(auth.current()).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		Result result = call(from.getId(), update);
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testInvalidToken() {
		when(auth.current()).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		when(manager.authorize(from, update)).thenReturn(null);
		Result result = call(from.getId(), update);
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConflict() {
		when(auth.current()).thenReturn(new Authorization(principal));
		when(repository.find(from.getId())).thenReturn(from.copy());
		when(registry.find(from.getType())).thenReturn(manager);
		when(manager.authorize(from, Credentials.CREDENTIALS.getValue(update))).thenReturn(command);
		when(dispatcher.dispatch(command)).thenThrow(new OpenSearchStatusException("version conflict", RestStatus.CONFLICT));
		Result result = call(from.getId(), update);
		assertThat(result).hasStatus(CONFLICT);
	}

	private static Result call(String credentialsId, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.CredentialsController.update(credentialsId), fakeRequest().withJsonBody(body));
	}
}
