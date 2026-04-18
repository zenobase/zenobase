package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.zenobase.commands.Command;
import com.zenobase.commands.TestCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class JournalControllerHttpPostTest extends JournalControllerTestSupport {

	private final Command command = new TestCommand(user.asIdentity(), "testing");

	@Test
	public void testUndo() {
		ArgumentCaptor<TestCommand> commandArg = ArgumentCaptor.forClass(TestCommand.class);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(commands.find(command.getId())).thenReturn(command);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(command.getId());
		try (Http1ClientResponse result = call(new UndoForm(command.getId()).toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, command.getId()).isEmpty();
			assertThat(commandArg.getValue().getTag()).isEqualTo("gnitset");
		}
	}

	@Test
	public void testUndoAsSuperuser() {
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(commands.find(command.getId())).thenReturn(command);
		when(dispatcher.dispatch(any(TestCommand.class))).thenReturn(command.getId());
		when(users.isSuperuser(superuser)).thenReturn(true);
		try (Http1ClientResponse result = call(new UndoForm(command.getId()).toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, command.getId()).isEmpty();
		}
	}

	@Test
	public void testUndoUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		try (Http1ClientResponse result = call(new UndoForm(command.getId()).toJson())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUndoFormNotValid() {
		when(auth.current(any())).thenReturn(null);
		try (Http1ClientResponse result = call(Nodes.newObject())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUndoNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(commands.find(command.getId())).thenReturn(null);
		try (Http1ClientResponse result = call(new UndoForm(Generator.id()).toJson())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUndoForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(commands.find(command.getId())).thenReturn(command);
		try (Http1ClientResponse result = call(new UndoForm(command.getId()).toJson())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(ObjectNode body) {
		return client.post("/journal/").submit(body);
	}
}
