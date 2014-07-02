package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;

public class TaskControllerHttpPostTest extends TaskControllerTestSupport {

	private final TaskManager manager = mock(TaskManager.class);
	private Task task;

	@Before
	public void setUp() {
		task = new Task("test", Generator.id(), user.asIdentity());
	}

	@Test
	public void testUpdateTaskSettings() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		when(registry.find(task.getType())).thenReturn(manager);
		when(dispatcher.dispatch(any(UpdateTaskCommand.class))).thenReturn(commandId);
		Result result = call(task.getId(), newSettings());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testUpdateTaskEmptyBody() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(registry.find(task.getType())).thenReturn(manager);
		when(tasks.find(task.getId())).thenReturn(task.copy());
		Result result = call(task.getId(), Nodes.newObject());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateTaskInvalidField() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(registry.find(task.getType())).thenReturn(manager);
		when(tasks.find(task.getId())).thenReturn(task.copy());
		Result result = call(task.getId(), Nodes.newObject("name", "Foo"));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateInvalidTaskType() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		Result result = call(task.getId(), newSettings());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateTaskNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(task.getId(), newSettings());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateTaskUnauthorized() {
		when(tasks.find(task.getId())).thenReturn(task.copy());
		Result result = call(task.getId(), newSettings());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateTaskForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		Result result = call(task.getId(), newSettings());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	private static ObjectNode newSettings() {
		ObjectNode task = Nodes.newObject();
		Task.SETTINGS.setValue(task, Nodes.newObject("name", "Foo"));
		return task;
	}

	private static Result call(String taskId, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.TaskController.update(taskId), fakeRequest().withJsonBody(body));
	}
}
