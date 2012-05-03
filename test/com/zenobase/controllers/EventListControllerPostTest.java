package com.zenobase.controllers;

import static com.zenobase.test.EventAssert.assertThat;
import static com.zenobase.test.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;

import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class EventListControllerPostTest extends EventListControllerTestSupport {

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
		when(queue.dispatch(commandArg.capture())).thenReturn(commandId);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(CREATED).hasContent(EventListController.receipt(commandId));
		assertThat(commandArg.getValue().getEvent())
			.hasField(Event.ID)
			.hasField(Event.TIMESTAMP)
			.hasValue(Event.AUTHOR, user.asIdentity());
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
		when(queue.dispatch(commandArg.capture())).thenReturn(commandId);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(CREATED).hasContent(EventListController.receipt(commandId));
		assertThat(commandArg.getValue().getEvent())
			.hasField(Event.ID)
			.hasValue(Event.TIMESTAMP, now)
			.hasValue(Event.AUTHOR, user.asIdentity())
			.hasValue(Event.TAG, tag);
	}

	@Test
	public void testCreateRandomEvents() {
		String commandId = Generator.id();
		int eventCount = 10;
		ArgumentCaptor<CompoundCommand> commandArg = ArgumentCaptor.forClass(CompoundCommand.class);
		EventListController.RANDOM.setValue(body, eventCount);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(queue.dispatch(commandArg.capture())).thenReturn(commandId);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(OK).hasContent(EventListController.receipt(commandId));
		assertThat(commandArg.getValue().getCommands().size()).as("number of commands").isEqualTo(eventCount);
	}

	@Test
	public void testMissingBucket() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, body);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(Bucket bucket, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.EventListController.post(bucket.getId()), fakeRequest().withJsonBody(body));
	}
}
