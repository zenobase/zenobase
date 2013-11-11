package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class BucketListControllerFindByUserTest extends BucketListControllerTestSupport {

	@Test
	public void test() {
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(user.asIdentity(), 0, 10)).thenReturn(list);
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(OK).hasContent(BucketList.toJson(list, events));
	}

	@Test
	public void testAsSuperuser() {
		Identity superuser = new Identity();
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(buckets.find(user.asIdentity(), 0, 10)).thenReturn(list);
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(OK).hasContent(BucketList.toJson(list, events));
	}

	@Test
	public void testLimitTooLow() {
		Result result = call(user.getId(), 0, -1);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testLimitTooHigh() {
		Result result = call(user.getId(), 0, 1000);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooLow() {
		Result result = call(user.getId(), -1, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooHigh() {
		Result result = call(user.getId(), 10000, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testMissingAuthorization() {
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call("@none", 0, 10);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testAsNotOwner() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String userId, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.BucketListController.findByUser(userId, offset, limit));
	}
}
