package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zenobase.models.Identity;
import com.zenobase.models.Session;
import com.zenobase.oauth.Authorization;
import io.helidon.webclient.http1.Http1ClientResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SessionsControllerHttpGetByUserTest extends SessionsControllerTestSupport {

	@Test
	public void testNotAuthorized() {
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testNotOwner() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = call('@' + user.getName())) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call("@nobody")) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testSelf() {
		Authorization authorization = new Authorization(user.asIdentity());
		authorization.setSessionId("sid-current");
		when(auth.current(any())).thenReturn(authorization);
		when(users.find(user.asIdentity())).thenReturn(user);
		when(directory.listSessions("auth0|tester")).thenReturn(
			List.of(
				Session.of("sid-current", "UA-1", "1.1.1.1", "2026-04-20T10:00:00Z", "2026-04-20T10:05:00Z"),
				Session.of("sid-other", "UA-2", "2.2.2.2", "2026-04-19T10:00:00Z", null)
			)
		);
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200).asObjectNode().path("sessions").isArray().hasSize(2);
		}
	}

	@Test
	public void testMarksCurrentSession() {
		Authorization authorization = new Authorization(user.asIdentity());
		authorization.setSessionId("sid-current");
		when(auth.current(any())).thenReturn(authorization);
		when(users.find(user.asIdentity())).thenReturn(user);
		when(directory.listSessions("auth0|tester")).thenReturn(
			List.of(
				Session.of("sid-current", "UA-1", "1.1.1.1", "2026-04-20T10:00:00Z", "2026-04-20T10:05:00Z"),
				Session.of("sid-other", "UA-2", "2.2.2.2", "2026-04-19T10:00:00Z", null)
			)
		);
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200);
			var obj = assertThat(result).asObjectNode();
			obj.path("sessions").path(0).path("id").isEqualTo("sid-current");
			obj.path("sessions").path(0).path("current").isEqualTo(true);
			obj.path("sessions").path(1).path("id").isEqualTo("sid-other");
			obj.path("sessions").path(1).path("current").isEqualTo(false);
		}
	}

	@Test
	public void testSuperuserSeesOtherUsersSessions() {
		Identity superuser = new Identity();
		Authorization authorization = new Authorization(superuser);
		authorization.setSessionId("sid-admin");
		when(auth.current(any())).thenReturn(authorization);
		when(users.find(user.asIdentity())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(directory.listSessions("auth0|tester")).thenReturn(
			List.of(Session.of("sid-other", "UA", "1.1.1.1", "2026-04-20T10:00:00Z", null))
		);
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200);
		}
	}

	private Http1ClientResponse call(String userId) {
		return client.get("/users/" + userId + "/sessions/").request();
	}
}
