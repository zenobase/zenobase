package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;

public class BucketControllerHttpGetTest extends BucketControllerTestSupport {

	private final Bucket bucket = new Bucket();

	@Before
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testGetBucketWithDashboard() {
		bucket.setWidgets(ImmutableList.of(Nodes.newObject()));
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId(), false);
		assertThat(result).hasStatus(OK).hasContent(bucket.toJson());
	}

	@Test
	public void testGetBucketLabel() {
		bucket.setLabel("Test Bucket");
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId(), true);
		assertThat(result).hasStatus(OK).asObjectNode().path("label").isEqualTo(bucket.getLabel());
	}

	@Test
	public void testGetBucketWithDefaultDashboard() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId(), false);
		BucketController.setDefaultDashboard(bucket);
		assertThat(result).hasStatus(OK).hasContent(bucket.toJson());
	}

	@Test
	public void testGetBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(bucket.getId(), false);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetBucketUnauthorized() {
		when(auth.current()).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId(), false);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetBucketForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId(), false);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String bucketId, boolean labelOnly) {
		return callAction(com.zenobase.controllers.routes.ref.BucketController.get(bucketId, labelOnly), fakeRequest());
	}
}
