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
import com.zenobase.services.BucketQuery;
import com.zenobase.services.SearchOrder;

public class BucketListControllerFindByUserTest extends BucketListControllerTestSupport {

	private static final SearchOrder ORDER_BY = SearchOrder.valueOf("label", Bucket.SCHEMA);

	@Test
	public void test() {
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()), ORDER_BY, 0, 10)).thenReturn(list);
		Result result = call(user.getId(), ORDER_BY, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(BucketList.toJson(list, events));
	}

	@Test
	public void testLabelsOnly() {
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()), ORDER_BY, 0, 10)).thenReturn(list);
		Result result = call(user.getId(), ORDER_BY, 0, 10, true);
		assertThat(result).hasStatus(OK).hasContent(BucketList.toJsonLabelsOnly(list));
	}

	@Test
	public void testAsSuperuser() {
		Identity superuser = new Identity();
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()), ORDER_BY, 0, 10)).thenReturn(list);
		Result result = call(user.getId(), ORDER_BY, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(BucketList.toJson(list, events));
	}

	@Test
	public void testLimitTooLow() {
		Result result = call(user.getId(), ORDER_BY, 0, -1);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testLimitTooHigh() {
		Result result = call(user.getId(), ORDER_BY, 0, 1000);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooLow() {
		Result result = call(user.getId(), ORDER_BY, -1, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testOffsetTooHigh() {
		Result result = call(user.getId(), ORDER_BY, 10000, 0);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testMissingAuthorization() {
		Result result = call(user.getId(), ORDER_BY, 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		Result result = call(user.getId(), ORDER_BY, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call("@none", ORDER_BY, 0, 10);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testAsNotOwner() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		Result result = call(user.getId(), ORDER_BY, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String userId, SearchOrder order, int offset, int limit) {
		return call(userId, order, offset, limit, false);
	}

	private static Result call(String userId, SearchOrder order, int offset, int limit, boolean labelsOnly) {
		return callAction(com.zenobase.controllers.routes.ref.BucketListController.findByUser(userId, order.toString(), offset, limit, labelsOnly));
	}
}
