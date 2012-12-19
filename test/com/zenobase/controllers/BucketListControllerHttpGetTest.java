package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;
import com.google.common.collect.ImmutableList;

import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class BucketListControllerHttpGetTest extends BucketListControllerTestSupport {

	@Test
	public void testGetBucketList() {
		BucketList list = new BucketList(ImmutableList.<Bucket>of(), 0, events);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.findBuckets(user.asIdentity(), 0, 10)).thenReturn(list);
		Result result = call("roles.principal:" + user.getId(), 0, 10);
		assertThat(result).hasStatus(OK).hasContent(list.toJson());
	}

	@Test
	public void testGetBucketBadQuery() {
		when(auth.current()).thenReturn(null);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call("foo:bar", 0, 10);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testGetBucketListNotSignedIn() {
		when(auth.current()).thenReturn(null);
		Result result = call("roles.principal:" + user.getId(), 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetBucketListForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		Result result = call("roles.principal:" + user.getId(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testGetCompleteBucketListSignedInAsAdmin() {
		BucketList list = new BucketList(ImmutableList.<Bucket>of(), 0, events);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(buckets.findAll(0, 10)).thenReturn(list);
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(list.toJson());
	}

	@Test
	public void testDownloadCompleteBucketListAsAdmin() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(null, 0, Integer.MAX_VALUE);
		assertThat(result).hasStatus(OK).hasContentType("text/plain");
	}

	@Test
	public void testGetCompleteBucketLisForbidden() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String identity, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.BucketListController.find(identity, offset, limit));
	}
}
