package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;

import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.common.collect.ImmutableList;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class BucketControllerGetBucketTest extends BucketControllerTestSupport {

	private Bucket bucket = new Bucket();

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addPermission(user.asIdentity(), Permission.ALL);
	}

	@Test
	public void testBucketWithDashboard() {
		bucket.setWidgets(ImmutableList.of(Nodes.newObject()));
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(OK).hasContent(bucket.toJson());
	}

	@Test
	public void testBucketWithDefaultDashboard() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		BucketController.setDefaultDashboard(bucket);
		assertThat(result).hasStatus(OK).hasContent(bucket.toJson());
	}

	@Test
	public void testNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String bucketId) {
		return callAction(com.zenobase.controllers.routes.ref.BucketController.get(bucketId), fakeRequest());
	}
}
