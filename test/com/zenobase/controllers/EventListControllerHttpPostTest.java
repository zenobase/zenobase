package com.zenobase.controllers;

import static com.zenobase.testing.EventAssert.assertThat;
import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;
import play.test.Helpers;

import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class EventListControllerHttpPostTest extends EventListControllerTestSupport {

	private final ObjectNode body = Nodes.newObject();

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addPermission(user.asIdentity(), Permission.ALL);
	}

	@Test
	public void testCreateEvent() {
		String commandId = Generator.id();
		ArgumentCaptor<CreateEventCommand> commandArg = ArgumentCaptor.forClass(CreateEventCommand.class);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(commandId);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(CREATED).hasContent(EventListController.content(null, commandId));
		assertThat(commandArg.getValue().getEvent())
			.hasField(Event.ID)
			.hasField(Event.TIMESTAMP)
			.hasValue(Event.AUTHOR, user.asIdentity());
	}

	@Test
	public void testCreateEvents() {
		String commandId = Generator.id();
		ArgumentCaptor<CompoundCommand> commandArg = ArgumentCaptor.forClass(CompoundCommand.class);
		ArrayNode eventsNode = body.putArray(EventListController.EVENTS.getName());
		Event.TAG.setValue(eventsNode.addObject(), "a");
		Event.TAG.setValue(eventsNode.addObject(), "b");
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(commandId);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(OK).hasContent(EventListController.content(null, commandId));
		assertThat(commandArg.getValue().getCommands().size()).isEqualTo(2);
	}

	@Test
	public void testCreateEventWithData() {
		String commandId = Generator.id();
		DateTime now = new DateTime(DateTimeZone.UTC);
		String tag = "test";
		ArgumentCaptor<CreateEventCommand> commandArg = ArgumentCaptor.forClass(CreateEventCommand.class);
		Event.TIMESTAMP.setValue(body, now);
		Event.TAG.setValue(body, tag);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(commandId);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(CREATED).hasContent(EventListController.content(null, commandId));
		assertThat(commandArg.getValue().getEvent())
			.hasField(Event.ID)
			.hasValue(Event.TIMESTAMP, now)
			.hasValue(Event.AUTHOR, user.asIdentity())
			.hasValue(Event.TAG, tag);
	}

	@Test
	public void testCreateEventBucketNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testCreateEventUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testCreateEventForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(Bucket bucket, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.EventListController.post(bucket.getId()), Helpers.fakeRequest().withJsonBody(body));
	}
}
