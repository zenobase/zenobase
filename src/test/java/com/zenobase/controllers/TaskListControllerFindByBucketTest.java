package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.TaskQuery;
import com.zenobase.tasks.TaskList;

public class TaskListControllerFindByBucketTest extends TaskListControllerTestSupport {

	private final Bucket bucket = new Bucket();

	@Test
	public void test() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
		TaskList list = new TaskList(DefaultPartialList.of());
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(tasks.find(new TaskQuery().bucketEqualTo(bucket.getId()), TaskQuery.orderByCreated(true), 0, 10))
				.thenReturn(list);
		try (Http1ClientResponse result = call(bucket.getId(), 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(TaskList.toJson(list));
		}
	}

	@Test
	public void testLimitTooLow() {
		try (Http1ClientResponse result = call(bucket.getId(), 0, -1)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testLimitTooHigh() {
		try (Http1ClientResponse result = call(bucket.getId(), 0, 1000)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooLow() {
		try (Http1ClientResponse result = call(bucket.getId(), -1, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooHigh() {
		try (Http1ClientResponse result = call(bucket.getId(), 10000, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testNotAuthorized() {
		try (Http1ClientResponse result = call(bucket.getId(), 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(Generator.id(), 0, 10)) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testNotOwner() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket.getId(), 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testSuperuser() {
		Identity superuser = new Identity();
		TaskList list = new TaskList(DefaultPartialList.of());
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(tasks.find(new TaskQuery().bucketEqualTo(bucket.getId()), TaskQuery.orderByCreated(true), 0, 10))
				.thenReturn(list);
		try (Http1ClientResponse result = call(bucket.getId(), 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(TaskList.toJson(list));
		}
	}

	private Http1ClientResponse call(String bucketId, int offset, int limit) {
		return client.get("/buckets/" + bucketId + "/tasks/")
				.queryParam("offset", String.valueOf(offset))
				.queryParam("limit", String.valueOf(limit))
				.request();
	}
}
