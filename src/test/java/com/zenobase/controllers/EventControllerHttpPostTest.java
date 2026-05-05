package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.UpdateEventCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;

public class EventControllerHttpPostTest extends EventControllerTestSupport {

	private Bucket bucket = new Bucket();
	private Event from, to;

	@BeforeEach
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
		from = new Event();
		from.setValue(Event.TAG, "foo");
		to = from.copy();
		to.setValue(Event.TAG, "bar");
	}

	@Test
	public void testUpdateEvent() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateEventCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(bucket.getId(), from.getId(), to.toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConflict() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateEventCommand.class))).thenThrow(
			new OpenSearchException(
				ErrorResponse.of(r ->
					r.status(409).error(e2 -> e2.type("version_conflict_engine_exception").reason("version conflict"))
				)
			)
		);
		try (Http1ClientResponse result = call(bucket.getId(), from.getId(), to.toJson())) {
			assertThat(result).hasStatus(409);
		}
	}

	@Test
	public void testUpdateBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(bucket.getId(), from.getId(), to.toJson())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateEventNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket.getId(), from.getId(), to.toJson())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateBucketUnauthorized() {
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket.getId(), from.getId(), to.toJson())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateBucketForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket.getId(), from.getId(), to.toJson())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateEventArchived() {
		bucket.setArchived(true);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket.getId(), from.getId(), to.toJson())) {
			assertThat(result).hasStatus(409);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(String bucketId, String eventId, ObjectNode body) {
		return client.put("/buckets/" + bucketId + "/" + eventId).submit(body);
	}
}
