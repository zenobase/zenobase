package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;

public class EventListControllerCountTest extends EventListControllerTestSupport {

	@BeforeEach
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testCountEvents() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(events.size()).thenReturn(42L);
		try (Http1ClientResponse result = call(null)) {
			assertThat(result).hasStatus(200).asObjectNode().path("total").isEqualTo(42);
		}
	}

	@Test
	public void testCountUserEvents() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(events.size(user.asIdentity())).thenReturn(7L);
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200).asObjectNode().path("total").isEqualTo(7);
		}
	}

	@Test
	public void testCountUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(users.find("@nobody")).thenReturn(null);
		try (Http1ClientResponse result = call("@nobody")) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testCountUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		try (Http1ClientResponse result = call(null)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testCountForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(null)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String userId) {
		if (userId != null) {
			return client.get("/users/" + userId + "/events/").request();
		}
		return client.get("/events/").request();
	}
}
