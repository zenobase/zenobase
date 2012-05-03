package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;

import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class EventControllerGetEventTest extends EventControllerTestSupport {

	private final Event event = new Event();

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addPermission(user.asIdentity(), Permission.ALL);
		event.setValue(Event.AUTHOR, user.asIdentity());
	}

	@Test
	public void testGetEvent() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(OK).hasContent(event.toJson());
	}

	@Test
	public void testMissingBucket() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testMissingEvent() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(Bucket bucket, Event event) {
		return callAction(com.zenobase.controllers.routes.ref.EventController.get(bucket.getId(), event.getId()));
	}
}
