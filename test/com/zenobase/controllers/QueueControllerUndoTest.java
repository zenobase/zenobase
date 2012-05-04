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

public class QueueControllerUndoTest extends QueueControllerTestSupport {

	private final Command command = new TestCommand(principal, "testing");

	@Test
	public void testUndo() {
		ArgumentCaptor<TestCommand> commandArg = ArgumentCaptor.forClass(TestCommand.class);
		when(auth.getPrincipal()).thenReturn(principal);
		when(commands.find(command.getId())).thenReturn(command);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(command.getId());
		Result result = call(new UndoForm(command.getId()).toJson());
		assertThat(result).hasStatus(CREATED).hasContent(QueueController.receipt(command.getId()));
		assertThat(commandArg.getValue().getTag()).isEqualTo("gnitset");
	}

	@Test
	public void testUndoAsSuperuser() {
		Identity superuser = new Identity();
		when(auth.getPrincipal()).thenReturn(superuser);
		when(commands.find(command.getId())).thenReturn(command);
		when(dispatcher.dispatch(any(TestCommand.class))).thenReturn(command.getId());
		when(users.isSuperuser(superuser)).thenReturn(true);
		Result result = call(new UndoForm(command.getId()).toJson());
		assertThat(result).hasStatus(CREATED).hasContent(QueueController.receipt(command.getId()));
	}

	@Test
	public void testUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		Result result = call(new UndoForm(command.getId()).toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testFormNotValid() {
		when(auth.getPrincipal()).thenReturn(null);
		Result result = call(Nodes.newObject());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testCommandNotFound() {
		when(auth.getPrincipal()).thenReturn(principal);
		when(commands.find(command.getId())).thenReturn(null);
		Result result = call(new UndoForm(Generator.id()).toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(commands.find(command.getId())).thenReturn(command);
		Result result = call(new UndoForm(command.getId()).toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	private Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.QueueController.post(), fakeRequest().withJsonBody(body));
	}
}
