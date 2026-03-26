package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;

public class TaskListControllerHttpPostTest extends TaskListControllerTestSupport {

	private final String type = "foo";
	private final Bucket bucket = new Bucket();
	private final CreateTaskForm form = new CreateTaskForm(bucket.getId(), type);

	@Test
	public void test() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
		TaskManager manager = mock(TaskManager.class);
		Task task = new Task(type, bucket.getId(), user.asIdentity());
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(registry.find(type)).thenReturn(manager);
		when(manager.newTask(eq(bucket.getId()), eq(user.asIdentity()), any())).thenReturn(task);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(form)) {
			assertThat(result).hasStatus(201).hasContent(task.toJson());
			assertThat(result).hasHeader("Location", "/tasks/" + task.getId());
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
		try (Http1ClientResponse result = call(new CreateTaskForm(null, null))) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testInvalidBucket() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(new CreateTaskForm(Generator.id(), type))) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testNotOwner() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(form)) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testInvalidType() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(new CreateTaskForm(bucket.getId(), "foo"))) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(CreateTaskForm form) {
		return client.post("/tasks/").submit(form.toJson());
	}
}
