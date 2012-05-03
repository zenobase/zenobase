package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class BucketControllerDeleteBucketTest extends BucketControllerTestSupport {

	private Bucket bucket = new Bucket();

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.setLabel("Obsolete Bucket");
		bucket.addPermission(user.asIdentity(), Permission.ALL);
	}

	@Test
	public void test() {
		String commandId = Generator.id();
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		when(queue.dispatch(any(DeleteBucketCommand.class))).thenReturn(commandId);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(OK).hasContent(BucketController.receipt(commandId));
	}

	@Test
	public void testAsSuperuser() {
		String commandId = Generator.id();
		Identity superuser = new Identity();
		when(auth.getPrincipal()).thenReturn(superuser);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		when(queue.dispatch(any(DeleteBucketCommand.class))).thenReturn(commandId);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(OK).hasContent(BucketController.receipt(commandId));
	}

	@Test
	public void testNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(queue);
	}

	@Test
	public void testUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(queue);
	}

	@Test
	public void testForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(queue);
	}

	private static Result call(String bucketId) {
		return callAction(com.zenobase.controllers.routes.ref.BucketController.delete(bucketId));
	}
}
