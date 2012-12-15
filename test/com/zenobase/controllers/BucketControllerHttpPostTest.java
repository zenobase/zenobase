package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.oauth.Authorization;

public class BucketControllerHttpPostTest extends BucketControllerTestSupport {

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
	public void testUpdateBucket() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.findBucket(from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateBucketCommand.class))).thenReturn(commandId);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(OK).hasContent(BucketController.content(null, commandId));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConflict() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.findBucket(from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateBucketCommand.class))).thenThrow(VersionConflictEngineException.class);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(CONFLICT);
	}

	@Test
	public void testUpdateBucketInvalidLabel() {
		to.setLabel("");
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.findBucket(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketAddOwner() {
		to.addPermission(new Identity(), Permission.ALL);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.findBucket(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketUnauthorized() {
		when(buckets.findBucket(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(buckets.findBucket(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(String bucketId, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.BucketController.update(bucketId), fakeRequest().withJsonBody(body));
	}
}
