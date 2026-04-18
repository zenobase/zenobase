package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Task;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

public class TaskControllerHttpDeleteTest extends TaskControllerTestSupport {

	private final Bucket bucket = new Bucket();
	private final String taskType = "test";
	private final Task task = new Task(taskType, bucket.getId(), user.asIdentity());

	@Test
	public void testDeleteTask() {
		String commandId = Generator.id();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testDeleteTaskNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDeleteTaskUnauthorized() {
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDeleteTaskBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task);
		when(dispatcher.dispatch(any(Command.class))).thenReturn("c");
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, "c").isEmpty();
		}
	}

	@Test
	public void testDeleteTaskForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task);
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDeleteTaskAsSuperuser() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	private Http1ClientResponse call(String taskId) {
		return client.delete("/tasks/" + taskId).request();
	}
}
