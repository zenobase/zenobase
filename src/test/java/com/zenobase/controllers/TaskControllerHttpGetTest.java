package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.IncompleteCredentialsException;
import com.zenobase.tasks.MissingCredentialsException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class TaskControllerHttpGetTest extends TaskControllerTestSupport {

	private final Bucket bucket = new Bucket();
	private final String taskType = "test";
	private final Task task = new Task(taskType, bucket.getId(), user.asIdentity());

	@Test
	public void testGetTask() {
		task.setCompleted(DateTime.now());
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(200).hasContent(task.toJson());
			verifyNoInteractions(refresher);
		}
	}

	@Test
	public void testGetStaleTask() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(200).hasContent(task.toJson());
			verify(refresher).refresh(task);
		}
	}

	@Test
	public void testGetStaleTaskInReadOnlyMode() {
		bus.setReadOnly(true);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(200).hasContent(task.toJson());
			verifyNoInteractions(refresher);
		}
	}

	@Test
	public void testGetStaleTaskOnArchivedBucket() {
		bucket.setArchived(true);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(200).hasContent(task.toJson());
			verifyNoInteractions(refresher);
		}
	}

	@Test
	public void testGetStaleTaskWithMissingCredentials() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		doThrow(new MissingCredentialsException("test"))
			.when(refresher)
			.refresh(any(Task.class));
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(200).hasContent(task.toJson());
			assertThat(result).hasHeader("X-Credentials", "test");
			verify(refresher).refresh(task);
		}
	}

	@Test
	public void testGetStaleTaskWithIncompleteCredentials() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		OAuthCredentials credentials = new OAuthCredentials("test", user.asIdentity());
		credentials.setAuthorizationUrl("http://localhost/authorize");
		doThrow(new IncompleteCredentialsException(credentials))
			.when(refresher)
			.refresh(any(Task.class));
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(200).hasContent(task.toJson());
			assertThat(result).hasHeader("Link", "<" + credentials.getAuthorizationUrl() + ">");
			verify(refresher).refresh(task);
		}
	}

	@Test
	public void testGetStaleTaskFailedRefresh() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(task.getId())).thenReturn(task.copy());
		doThrow(new RuntimeException())
			.when(refresher)
			.refresh(any(Task.class));
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(500);
		}
	}

	@Test
	public void testGetTaskNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testGetTaskUnauthorized() {
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testGetTaskForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(tasks.find(task.getId())).thenReturn(task);
		try (Http1ClientResponse result = call(task.getId())) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String taskId) {
		return client.get("/tasks/" + taskId).request();
	}
}
