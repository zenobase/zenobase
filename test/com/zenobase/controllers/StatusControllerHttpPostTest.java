package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Test;
import play.mvc.Result;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.models.StatusInfo;
import com.zenobase.oauth.Authorization;

public class StatusControllerHttpPostTest extends StatusControllerTestSupport {

	@Test
	public void testEnableReadOnly() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		Result result = call(new StatusInfo(true).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(bus).setReadOnly(true);
	}

	@Test
	public void testDisableReadOnly() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(bus.isReadOnly()).thenReturn(true);
		Result result = call(new StatusInfo(false).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(bus).setReadOnly(false);
	}

	@Test
	public void testUnauthorized() {
		Result result = call(new StatusInfo(true).toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(bus);
	}

	@Test
	public void testForbidden() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(new StatusInfo(true).toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(bus);
	}

	private static Result call(JsonNode node) {
		return callAction(com.zenobase.controllers.routes.ref.StatusController.post(), fakeRequest().withJsonBody(node));
	}
}
