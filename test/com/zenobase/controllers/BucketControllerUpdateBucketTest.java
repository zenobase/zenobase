package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class BucketControllerUpdateBucketTest extends BucketControllerTestSupport {

	private Bucket from, to;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		from = new Bucket();
		from.setLabel("Test Bucket");
		from.addPermission(user.asIdentity(), Permission.ALL);
		to = from.copy();
		to.setLabel("Real Bucket");
	}

	@Test
	public void testSuccess() {
		String commandId = Generator.id();
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(from.getId())).thenReturn(from.copy());
		when(queue.dispatch(any(UpdateBucketCommand.class))).thenReturn(commandId);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(OK).hasContent(BucketController.receipt(commandId));
	}

	@Test
	public void testNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(from.getId())).thenReturn(null);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(queue);
	}

	@Test
	public void testUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(queue);
	}

	@Test
	public void testForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(queue);
	}

	private static Result call(String bucketId, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.BucketController.update(bucketId), fakeRequest().withJsonBody(body));
	}
}
