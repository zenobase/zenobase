package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;

import com.zenobase.models.StatusInfo;
import com.zenobase.oauth.Authorization;

public class StatusControllerHttpPostTest extends StatusControllerTestSupport {

	@Test
	public void testEnableReadOnly() {
		when(bus.isReadOnly()).thenReturn(false);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		try (Http1ClientResponse result = call(new StatusInfo(true).toJson())) {
			assertThat(result).hasStatus(204);
			verify(bus).setReadOnly(true);
		}
	}

	@Test
	public void testDisableReadOnly() {
		when(bus.isReadOnly()).thenReturn(true);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		try (Http1ClientResponse result = call(new StatusInfo(false).toJson())) {
			assertThat(result).hasStatus(204);
			verify(bus).setReadOnly(false);
		}
	}

	@Test
	public void testUnauthorized() {
		when(bus.isReadOnly()).thenReturn(false);
		try (Http1ClientResponse result = call(new StatusInfo(true).toJson())) {
			assertThat(result).hasStatus(401);
			verify(bus, never()).setReadOnly(true);
		}
	}

	@Test
	public void testForbidden() {
		when(bus.isReadOnly()).thenReturn(false);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(new StatusInfo(true).toJson())) {
			assertThat(result).hasStatus(403);
			verify(bus, never()).setReadOnly(true);
		}
	}

	private Http1ClientResponse call(JsonNode node) {
		return client.post("/status").submit(node);
	}
}
