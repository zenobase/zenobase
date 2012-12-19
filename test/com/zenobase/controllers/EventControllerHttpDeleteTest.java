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
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;

public class EventControllerHttpDeleteTest extends EventControllerTestSupport {

	private final Event event = new Event();
	private final Identity friend = new Identity();

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		bucket.addRole(friend, Role.CONTRIBUTOR);
		event.setValue(Event.AUTHOR, user.asIdentity());
	}

	@Test
	public void testDeleteEvent() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		String commandId = Generator.id();
		when(dispatcher.dispatch(any(DeleteEventCommand.class))).thenReturn(commandId);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testDeleteEventBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteEventNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteEventUnauthorized() {
		when(auth.current()).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteEventForbidden() {
		when(auth.current()).thenReturn(new Authorization(friend));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(Bucket bucket, Event event) {
		return callAction(com.zenobase.controllers.routes.ref.EventController.delete(bucket.getId(), event.getId()));
	}
}
