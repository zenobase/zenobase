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
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;

public class EventControllerHttpGetTest extends EventControllerTestSupport {

	private final Event event = new Event();

	@Before
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
		event.setValue(Event.AUTHOR, user.asIdentity());
	}

	@Test
	public void testGetEvent() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(OK).hasContent(event.toJson());
	}

	@Test
	public void testGetEventBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetEventNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetEventUnauthorized() {
		when(auth.current()).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetEventForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(Bucket bucket, Event event) {
		return callAction(com.zenobase.controllers.routes.ref.EventController.get(bucket.getId(), event.getId()));
	}
}
