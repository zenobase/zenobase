package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.joda.time.DateTime;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.IncompleteCredentialsException;
import com.zenobase.tasks.MissingCredentialsException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class TaskControllerHttpGetTest extends TaskControllerTestSupport {

	private final Bucket bucket = new Bucket();
	private final String taskType = "test";
	private final Task task = new Task(taskType, bucket.getId(), user.asIdentity());

	@Test
	public void testGetTask() {
		task.setCompleted(DateTime.now());
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		Result result = call(task.getId());
		assertThat(result).hasStatus(OK).hasContent(task.toJson());
		verifyZeroInteractions(refresher);
	}

	@Test
	public void testGetStaleTask() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		Result result = call(task.getId());
		assertThat(result).hasStatus(OK).hasContent(task.toJson());
		verify(refresher).refresh(task);
	}

	@Test
	public void testGetStaleTaskInReadOnlyMode() {
		bus.setReadOnly(true);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		Result result = call(task.getId());
		assertThat(result).hasStatus(OK).hasContent(task.toJson());
		verifyZeroInteractions(refresher);
	}

	@Test
	public void testGetStaleTaskWithMissingCredentials() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		doThrow(new MissingCredentialsException("test")).when(refresher).refresh(any(Task.class));
		Result result = call(task.getId());
		assertThat(result).hasStatus(OK).hasContent(task.toJson());
		assertThat(result).hasHeader("X-Credentials", "test");
		verify(refresher).refresh(task);
	}

	@Test
	public void testGetStaleTaskWithIncompleteCredentials() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		OAuthCredentials credentials = new OAuthCredentials("test", user.asIdentity());
		credentials.setAuthorizationUrl("http://localhost/authorize");
		doThrow(new IncompleteCredentialsException(credentials)).when(refresher).refresh(any(Task.class));
		Result result = call(task.getId());
		assertThat(result).hasStatus(OK).hasContent(task.toJson());
		assertThat(result).hasHeader("Link", "<" + credentials.getAuthorizationUrl() + ">");
		verify(refresher).refresh(task);
	}

	@Test
	public void testGetStaleTaskFailedRefresh() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		doThrow(new RuntimeException()).when(refresher).refresh(any(Task.class));
		Result result = call(task.getId());
		assertThat(result).hasStatus(OK).hasContent(task.toJson());
		verify(refresher).refresh(task);
	}

	@Test
	public void testGetTaskNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(task.getId());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetTaskUnauthorized() {
		Result result = call(task.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetTaskForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(tasks.find(task.getId())).thenReturn(task);
		Result result = call(task.getId());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String taskId) {
		return callAction(com.zenobase.controllers.routes.ref.TaskController.get(taskId));
	}
}
