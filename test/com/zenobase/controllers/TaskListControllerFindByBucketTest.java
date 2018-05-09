package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import play.mvc.Result;

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
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(tasks.find(new TaskQuery().bucketEqualTo(bucket.getId()), TaskQuery.orderByCreated(true), 0, 10)).thenReturn(list);
		Result result = call(bucket.getId(), 0, 10);
		assertThat(result).hasStatus(OK).hasContent(TaskList.toJson(list));
	}

	@Test
	public void testLimitTooLow() {
		Result result = call(bucket.getId(), 0, -1);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testLimitTooHigh() {
		Result result = call(bucket.getId(), 0, 1000);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooLow() {
		Result result = call(bucket.getId(), -1, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooHigh() {
		Result result = call(bucket.getId(), 10000, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testNotAuthorized() {
		Result result = call(bucket.getId(), 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(Generator.id(), 0, 10);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testNotOwner() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket.getId(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testSuperuser() {
		Identity superuser = new Identity();
		TaskList list = new TaskList(DefaultPartialList.of());
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(tasks.find(new TaskQuery().bucketEqualTo(bucket.getId()), TaskQuery.orderByCreated(true), 0, 10)).thenReturn(list);
		Result result = call(bucket.getId(), 0, 10);
		assertThat(result).hasStatus(OK).hasContent(TaskList.toJson(list));
	}

	private static Result call(String bucketId, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.TaskListController.findByBucket(bucketId, offset, limit));
	}
}
