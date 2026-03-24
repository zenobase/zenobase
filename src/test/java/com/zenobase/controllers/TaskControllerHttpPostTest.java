package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Before;
import org.junit.Test;

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
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		when(registry.find(task.getType())).thenReturn(manager);
		when(dispatcher.dispatch(any(UpdateTaskCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(task.getId(), newSettings())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testUpdateTaskEmptyBody() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(registry.find(task.getType())).thenReturn(manager);
		when(tasks.find(task.getId())).thenReturn(task.copy());
		try (Http1ClientResponse result = call(task.getId(), Nodes.newObject())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateTaskInvalidField() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(registry.find(task.getType())).thenReturn(manager);
		when(tasks.find(task.getId())).thenReturn(task.copy());
		try (Http1ClientResponse result = call(task.getId(), Nodes.newObject("name", "Foo"))) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateInvalidTaskType() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		try (Http1ClientResponse result = call(task.getId(), newSettings())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateTaskNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(task.getId(), newSettings())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateTaskUnauthorized() {
		when(tasks.find(task.getId())).thenReturn(task.copy());
		try (Http1ClientResponse result = call(task.getId(), newSettings())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateTaskForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		try (Http1ClientResponse result = call(task.getId(), newSettings())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	private static ObjectNode newSettings() {
		ObjectNode task = Nodes.newObject();
		Task.SETTINGS.setValue(task, Nodes.newObject("name", "Foo"));
		return task;
	}

	private Http1ClientResponse call(String taskId, ObjectNode body) {
		return client.post("/tasks/" + taskId).submit(body);
	}
}
