package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.commands.DeleteEventCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class EventControllerHttpDeleteTest extends EventControllerTestSupport {

	private final Event event = new Event();
	private final Identity friend = new Identity();

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addPermission(user.asIdentity(), Permission.ALL);
		bucket.addPermission(friend, Permission.CONTRIBUTE);
		event.setValue(Event.AUTHOR, user.asIdentity());
	}

	@Test
	public void testDeleteEvent() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(event);
		String commandId = Generator.id();
		when(dispatcher.dispatch(any(DeleteEventCommand.class))).thenReturn(commandId);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(OK).hasContent(EventController.content(null, commandId));
	}

	@Test
	public void testDeleteEventBucketNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteEventNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteEventUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteEventForbidden() {
		when(auth.getPrincipal()).thenReturn(friend);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(Bucket bucket, Event event) {
		return callAction(com.zenobase.controllers.routes.ref.EventController.delete(bucket.getId(), event.getId()));
	}
}
