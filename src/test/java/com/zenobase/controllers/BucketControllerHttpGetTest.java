package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Before;
import org.junit.Test;

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
		bucket.setWidgets(List.of(Nodes.newObject()));
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		try (Http1ClientResponse result = call(bucket.getId(), false)) {
			assertThat(result).hasStatus(200).hasContent(bucket.toJson());
		}
	}

	@Test
	public void testGetBucketLabel() {
		bucket.setLabel("Test Bucket");
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		try (Http1ClientResponse result = call(bucket.getId(), true)) {
			assertThat(result).hasStatus(200).asObjectNode().path("label").isEqualTo(bucket.getLabel());
		}
	}

	@Test
	public void testGetBucketWithDefaultDashboard() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		try (Http1ClientResponse result = call(bucket.getId(), false)) {
			BucketController.setDefaultDashboard(bucket);
			assertThat(result).hasStatus(200).hasContent(bucket.toJson());
		}
	}

	@Test
	public void testGetBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(bucket.getId(), false)) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testGetBucketUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		try (Http1ClientResponse result = call(bucket.getId(), false)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testGetBucketForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		try (Http1ClientResponse result = call(bucket.getId(), false)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String bucketId, boolean labelOnly) {
		if (labelOnly) {
			return client.get("/buckets/" + bucketId + "/label").request();
		}
		return client.get("/buckets/" + bucketId).request();
	}
}
