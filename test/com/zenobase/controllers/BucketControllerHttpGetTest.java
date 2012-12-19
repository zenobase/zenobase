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
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;

public class BucketControllerHttpGetTest extends BucketControllerTestSupport {

	private final Bucket bucket = new Bucket();

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testGetBucketWithDashboard() {
		bucket.setWidgets(ImmutableList.of(Nodes.newObject()));
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(OK).hasContent(bucket.toJson());
	}

	@Test
	public void testGetBucketWithDefaultDashboard() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		BucketController.setDefaultDashboard(bucket);
		assertThat(result).hasStatus(OK).hasContent(bucket.toJson());
	}

	@Test
	public void testGetBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetBucketUnauthorized() {
		when(auth.current()).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetBucketForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String bucketId) {
		return callAction(com.zenobase.controllers.routes.ref.BucketController.get(bucketId), fakeRequest());
	}
}
