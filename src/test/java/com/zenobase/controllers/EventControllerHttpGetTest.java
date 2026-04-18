package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventControllerHttpGetTest extends EventControllerTestSupport {

	private final Event event = new Event();

	@BeforeEach
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
		event.setValue(Event.AUTHOR, user.asIdentity());
	}

	@Test
	public void testGetEvent() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(200).hasContent(event.toJson());
		}
	}

	@Test
	public void testGetEventBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(null);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testGetEventNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(null);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testGetEventUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testGetEventForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(bucket.getId(), event.getId())).thenReturn(event);
		try (Http1ClientResponse result = call(bucket, event)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(Bucket bucket, Event event) {
		return client.get("/buckets/" + bucket.getId() + "/" + event.getId()).request();
	}
}
