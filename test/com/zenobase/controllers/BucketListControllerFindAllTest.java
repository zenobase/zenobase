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

public class BucketListControllerFindAllTest extends BucketListControllerTestSupport {

	@Test
	public void test() {
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(buckets.find(new BucketQuery().queryString("foo"), BucketQuery.DEFAULT_ORDER, 0, 10)).thenReturn(list);
		Result result = call("foo", 0, 10);
		assertThat(result).hasStatus(OK).hasContent(BucketList.toJson(list, events));
	}

	@Test
	public void testDownload() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(null, 0, Integer.MAX_VALUE);
		assertThat(result).hasStatus(OK).hasContentType("text/plain");
	}

	@Test
	public void testWithoutAuthorization() {
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testWithScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testWithNonSuperuser() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String q, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.BucketListController.findAll(q, offset, limit));
	}
}
