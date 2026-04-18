package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.TaskQuery;
import com.zenobase.tasks.TaskList;

public class TaskListControllerFindByUserTest extends TaskListControllerTestSupport {

	@Test
	public void test() {
		TaskList list = new TaskList(DefaultPartialList.of());
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(tasks.find(new TaskQuery().principalEqualTo(user.asIdentity()), 0, 10)).thenReturn(list);
		try (Http1ClientResponse result = call(user.getId(), null, 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(TaskList.toJson(list));
		}
	}

	@Test
	public void testLimitTooLow() {
		try (Http1ClientResponse result = call(user.getId(), null, 0, -1)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testLimitTooHigh() {
		try (Http1ClientResponse result = call(user.getId(), null, 0, 1000)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooLow() {
		try (Http1ClientResponse result = call(user.getId(), null, -1, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooHigh() {
		try (Http1ClientResponse result = call(user.getId(), null, 10000, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testNotAuthorized() {
		try (Http1ClientResponse result = call(user.getId(), null, 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		try (Http1ClientResponse result = call(user.getId(), null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call("@jdoe", null, 0, 10)) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testNotOwner() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getId())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getId(), null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testSuperuser() {
		Identity superuser = new Identity();
		TaskList list = new TaskList(DefaultPartialList.of());
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.find(user.getId())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(tasks.find(new TaskQuery().principalEqualTo(user.asIdentity()).queryString("type:foo"), 0, 10)).thenReturn(
			list
		);
		try (Http1ClientResponse result = call(user.getId(), "type:foo", 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(TaskList.toJson(list));
		}
	}

	private Http1ClientResponse call(String userId, String q, int offset, int limit) {
		var request = client
			.get("/users/" + userId + "/tasks/")
			.queryParam("offset", String.valueOf(offset))
			.queryParam("limit", String.valueOf(limit));
		if (q != null) {
			request = request.queryParam("q", q);
		}
		return request.request();
	}
}
