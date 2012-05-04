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

public class BucketListControllerGetTest extends BucketListControllerTestSupport {

	@Test
	public void testGetMyBuckets() {
		BucketList list = new BucketList(ImmutableList.<Bucket>of(), 0, buckets);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBuckets(user.asIdentity(), 0, 10)).thenReturn(list);
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(OK).hasContent(list.toJson());
	}

	@Test
	public void testNotLoggedIn() {
		when(auth.getPrincipal()).thenReturn(null);
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetYourBuckets() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		Result result = call(user.getId(), 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testGetAllBucketsAsAdmin() {
		BucketList list = new BucketList(ImmutableList.<Bucket>of(), 0, buckets);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(buckets.findBuckets(0, 10)).thenReturn(list);
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(list.toJson());
	}

	@Test
	public void testDownloadBucketsAsAdmin() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(null, 0, Integer.MAX_VALUE);
		assertThat(result).hasStatus(OK).hasContentType("text/plain");
	}

	@Test
	public void testGetAllBuckets() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		Result result = call(null, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String identity, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.BucketListController.find(identity, offset, limit));
	}
}
