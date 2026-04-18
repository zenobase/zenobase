package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.commands.DeleteEventCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventControllerHttpDeleteTest extends EventControllerTestSupport {

	private final Event event = new Event();
	private final Identity friend = new Identity();

	@BeforeEach
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
		bucket.addRole(friend, Role.CONTRIBUTOR);
		event.setValue(Event.AUTHOR, user.asIdentity());
	}

	@Test
	public void testDeleteEvent() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		String commandId = Generator.id();
		when(dispatcher.dispatch(any(DeleteEventCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testDeleteEventBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(null);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDeleteEventNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(null);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDeleteEventUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDeleteEventForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(friend));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(Bucket bucket, Event event) {
		return client.delete("/buckets/" + bucket.getId() + "/" + event.getId()).request();
	}
}
