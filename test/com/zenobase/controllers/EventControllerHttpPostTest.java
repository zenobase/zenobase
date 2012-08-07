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

import com.zenobase.commands.UpdateEventCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class EventControllerHttpPostTest extends EventControllerTestSupport {

	private Bucket bucket = new Bucket();
	private Event from, to;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addPermission(user.asIdentity(), Permission.ALL);
		from = new Event();
		from.setValue(Event.TAG, "foo");
		to = from.copy();
		to.setValue(Event.TAG, "bar");
	}

	@Test
	public void testUpdateEvent() {
		String commandId = Generator.id();
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateEventCommand.class))).thenReturn(commandId);
		Result result = call(bucket.getId(), from.getId(), to.toJson());
		assertThat(result).hasStatus(OK).hasContent(BucketController.receipt(commandId));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConflict() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateEventCommand.class))).thenThrow(VersionConflictEngineException.class);
		Result result = call(bucket.getId(), from.getId(), to.toJson());
		assertThat(result).hasStatus(CONFLICT);
	}

	@Test
	public void testUpdateBucketNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		Result result = call(bucket.getId(), from.getId(), to.toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateEventNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket.getId(), from.getId(), to.toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketUnauthorized() {
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket.getId(), from.getId(), to.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket.getId(), from.getId(), to.toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(String bucketId, String eventId, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.EventController.update(bucketId, eventId), fakeRequest().withJsonBody(body));
	}
}
