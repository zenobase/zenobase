package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class EventListControllerHttpPostTest extends EventListControllerTestSupport {

	private final ObjectNode body = Nodes.newObject();

	@BeforeEach
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testCreateEvent() {
		String commandId = Generator.id();
		ArgumentCaptor<CreateEventCommand> commandArg = ArgumentCaptor.forClass(CreateEventCommand.class);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(commandId);
		try (Http1ClientResponse result = call(bucket, body)) {
			assertThat(result).hasStatus(201).hasHeader(COMMAND_ID, commandId);
			Event event = commandArg.getValue().getEvent();
			assertThat(result).hasContent(event.toJson());
			assertThat(event.getValue(Event.ID)).isNotNull();
			assertThat(event.getValue(Event.TIMESTAMP)).isNotNull();
			assertThat(event.getValue(Event.AUTHOR)).isEqualTo(user.asIdentity());
		}
	}

	@Test
	public void testCreateEvents() {
		String commandId = Generator.id();
		ArgumentCaptor<CreateEventsCommand> commandArg = ArgumentCaptor.forClass(CreateEventsCommand.class);
		ArrayNode eventsNode = body.putArray(EventListController.EVENTS.getName());
		Event.TAG.setValue(eventsNode.addObject(), "a");
		Event.TAG.setValue(eventsNode.addObject(), "b");
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(commandId);
		try (Http1ClientResponse result = call(bucket, body)) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
			assertThat(commandArg.getValue().getEvents().size()).isEqualTo(2);
		}
	}

	@Test
	public void testCreateEventWithData() {
		String commandId = Generator.id();
		DateTime now = DateTime.now(DateTimeZone.UTC);
		String tag = "test";
		ArgumentCaptor<CreateEventCommand> commandArg = ArgumentCaptor.forClass(CreateEventCommand.class);
		Event.TIMESTAMP.setValue(body, now);
		Event.TAG.setValue(body, tag);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(dispatcher.dispatch(commandArg.capture())).thenReturn(commandId);
		try (Http1ClientResponse result = call(bucket, body)) {
			assertThat(result).hasStatus(201).hasHeader(COMMAND_ID, commandId);
			Event event = commandArg.getValue().getEvent();
			assertThat(result).hasContent(event.toJson());
			assertThat(event.getValue(Event.ID)).isNotNull();
			assertThat(event.getValue(Event.TIMESTAMP)).isEqualTo(now);
			assertThat(event.getValue(Event.AUTHOR)).isEqualTo(user.asIdentity());
			assertThat(event.getValue(Event.TAG)).isEqualTo(tag);
		}
	}

	@Test
	public void testCreateEventBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(null);
		try (Http1ClientResponse result = call(bucket, body)) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testCreateEventUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket, body)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testCreateEventForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket, body)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testCreateEventArchived() {
		bucket.setArchived(true);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket, body)) {
			assertThat(result).hasStatus(409);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(Bucket bucket, ObjectNode body) {
		return client.post("/buckets/" + bucket.getId() + "/").submit(body);
	}
}
