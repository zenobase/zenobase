package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Task;

public class TaskControllerHttpDeleteTest extends TaskControllerTestSupport {

	private final Bucket bucket = new Bucket();
	private final String taskType = "test";
	private final Task task = new Task(taskType, bucket.getId(), user.asIdentity());

	@Test
	public void testDeleteTask() {
		String commandId = Generator.id();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		Result result = call(task.getId());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testDeleteTaskNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(task.getId());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteTaskUnauthorized() {
		Result result = call(task.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteTaskBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task);
		when(dispatcher.dispatch(any(Command.class))).thenReturn("c");
		Result result = call(task.getId());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, "c").isEmpty();
	}

	@Test
	public void testDeleteTaskForbidden() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task);
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		Result result = call(task.getId());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteTaskAsSuperuser() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		Result result = call(task.getId());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	private static Result call(String taskId) {
		return callAction(com.zenobase.controllers.routes.ref.TaskController.delete(taskId));
	}
}
