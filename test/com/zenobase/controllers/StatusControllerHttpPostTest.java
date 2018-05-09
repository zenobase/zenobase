package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import play.mvc.Http.Context;
import play.mvc.Result;

import com.zenobase.models.StatusInfo;
import com.zenobase.oauth.Authorization;

public class StatusControllerHttpPostTest extends StatusControllerTestSupport {

	@Test
	public void testEnableReadOnly() {
		when(bus.isReadOnly()).thenReturn(false);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(auth.current(any(Context.class))).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(new StatusInfo(true).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(bus).setReadOnly(true);
	}

	@Test
	public void testDisableReadOnly() {
		when(bus.isReadOnly()).thenReturn(true);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(auth.current(any(Context.class))).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(new StatusInfo(false).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(bus).setReadOnly(false);
	}

	@Test
	public void testServiceUnavailable() {
		when(bus.isReadOnly()).thenReturn(true);
		Result result = call(new StatusInfo(false).toJson());
		assertThat(result).hasStatus(SERVICE_UNAVAILABLE);
		verify(bus, never()).setReadOnly(false);
	}

	@Test
	public void testUnauthorized() {
		when(bus.isReadOnly()).thenReturn(false);
		Result result = call(new StatusInfo(true).toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verify(bus, never()).setReadOnly(true);
	}

	@Test
	public void testForbidden() {
		when(bus.isReadOnly()).thenReturn(false);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(auth.current(any(Context.class))).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(new StatusInfo(true).toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verify(bus, never()).setReadOnly(true);
	}

	private static Result call(JsonNode node) {
		return callAction(com.zenobase.controllers.routes.ref.StatusController.post(), fakeRequest().withJsonBody(node, "POST"));
	}
}
