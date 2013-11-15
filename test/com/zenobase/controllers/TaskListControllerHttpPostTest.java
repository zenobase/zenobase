package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Test;
import play.mvc.Result;
import play.test.Helpers;

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
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(registry.find(type)).thenReturn(manager);
		when(manager.newTask(bucket.getId(), user.asIdentity(), form.getSettings())).thenReturn(task);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		Result result = call(form);
		assertThat(result).hasStatus(CREATED).hasContent(task.toJson());
		assertThat(Helpers.redirectLocation(result)).isEqualTo(com.zenobase.controllers.routes.TaskController.get(task.getId()).toString());
		assertThat(Helpers.header(COMMAND_ID, result)).isEqualTo(commandId);
	}

	@Test
	public void testNotAuthorized() {
		Result result = call(form);
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testInvalidBody() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(new CreateTaskForm(null, null));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testInvalidBucket() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(new CreateTaskForm(Generator.id(), type));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testNotOwner() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		Result result = call(form);
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testInvalidType() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		Result result = call(new CreateTaskForm(bucket.getId(), "foo"));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(CreateTaskForm form) {
		return callAction(com.zenobase.controllers.routes.ref.TaskListController.post(), fakeRequest().withJsonBody(form.toJson()));
	}
}
