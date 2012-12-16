package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;

import com.zenobase.commands.Command;
import com.zenobase.commands.TestCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class QueueControllerHttpPostTest extends QueueControllerTestSupport {

	private final Command command = new TestCommand(principal, "testing");

	@Test
	public void testUndo() {
		ArgumentCaptor<TestCommand> commandArg = ArgumentCaptor.forClass(TestCommand.class);
		when(auth.current()).thenReturn(new Authorization(principal));
		when(commands.find(command.getId())).thenReturn(command);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(command.getId());
		Result result = call(new UndoForm(command.getId()).toJson());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, command.getId()).isEmpty();
		assertThat(commandArg.getValue().getTag()).isEqualTo("gnitset");
	}

	@Test
	public void testUndoAsSuperuser() {
		Identity superuser = new Identity();
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(commands.find(command.getId())).thenReturn(command);
		when(dispatcher.dispatch(any(TestCommand.class))).thenReturn(command.getId());
		when(users.isSuperuser(superuser)).thenReturn(true);
		Result result = call(new UndoForm(command.getId()).toJson());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, command.getId()).isEmpty();
	}

	@Test
	public void testUndoUnauthorized() {
		when(auth.current()).thenReturn(null);
		Result result = call(new UndoForm(command.getId()).toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUndoFormNotValid() {
		when(auth.current()).thenReturn(null);
		Result result = call(Nodes.newObject());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUndoNotFound() {
		when(auth.current()).thenReturn(new Authorization(principal));
		when(commands.find(command.getId())).thenReturn(null);
		Result result = call(new UndoForm(Generator.id()).toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUndoForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(commands.find(command.getId())).thenReturn(command);
		Result result = call(new UndoForm(command.getId()).toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	private Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.QueueController.post(), fakeRequest().withJsonBody(body));
	}
}
