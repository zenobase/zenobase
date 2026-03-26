package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.TaskQuery;
import com.zenobase.tasks.TaskList;

public class TaskListControllerFindAllTest extends TaskListControllerTestSupport {

	@Test
	public void test() {
		TaskList list = new TaskList(DefaultPartialList.of());
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(tasks.find(new TaskQuery().queryString("type:foo"), 0, 10)).thenReturn(list);
		try (Http1ClientResponse result = call("type:foo", 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(TaskList.toJson(list));
		}
	}

	@Test
	public void testLimitTooLow() {
		try (Http1ClientResponse result = call(null, 0, -1)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testLimitTooHigh() {
		try (Http1ClientResponse result = call(null, 0, 1000)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooLow() {
		try (Http1ClientResponse result = call(null, -1, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooHigh() {
		try (Http1ClientResponse result = call(null, 10000, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testNotAuthorized() {
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), Generator.id()));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String q, int offset, int limit) {
		var request = client.get("/tasks/")
				.queryParam("offset", String.valueOf(offset))
				.queryParam("limit", String.valueOf(limit));
		if (q != null) {
			request = request.queryParam("q", q);
		}
		return request.request();
	}
}
