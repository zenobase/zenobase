package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

public class SessionsControllerHttpDeleteTest extends SessionsControllerTestSupport {

	@Test
	public void testDeleteOneNotAuthorized() {
		try (Http1ClientResponse result = client.delete("/users/" + user.getId() + "/sessions/abc").request()) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testDeleteOneSelf() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(directory.revokeSession("auth0|tester", "sid-xyz")).thenReturn(true);
		try (Http1ClientResponse result = client.delete("/users/" + user.getId() + "/sessions/sid-xyz").request()) {
			assertThat(result).hasStatus(204);
		}
		verify(directory).revokeSession("auth0|tester", "sid-xyz");
	}

	@Test
	public void testDeleteOneNotOwner() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = client.delete("/users/@" + user.getName() + "/sessions/sid-xyz").request()) {
			assertThat(result).hasStatus(403);
		}
		verify(directory, never()).revokeSession(any(), any());
	}

	@Test
	public void testDeleteOneSessionNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(directory.revokeSession("auth0|tester", "sid-bogus")).thenReturn(false);
		try (Http1ClientResponse result = client.delete("/users/" + user.getId() + "/sessions/sid-bogus").request()) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testDeleteOneAuth0Down() {
		// Service exceptions propagate to the global error handler, which returns 500 — the
		// controller does NOT mask transient failures as "session not found" (which would be 404).
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(directory.revokeSession("auth0|tester", "sid-xyz")).thenThrow(new RuntimeException("Auth0 down"));
		try (Http1ClientResponse result = client.delete("/users/" + user.getId() + "/sessions/sid-xyz").request()) {
			assertThat(result).hasStatus(500);
		}
	}

	@Test
	public void testDeleteOneSuperuser() {
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(directory.revokeSession("auth0|tester", "sid-xyz")).thenReturn(true);
		try (Http1ClientResponse result = client.delete("/users/" + user.getId() + "/sessions/sid-xyz").request()) {
			assertThat(result).hasStatus(204);
		}
	}

	@Test
	public void testDeleteAllNotAuthorized() {
		try (Http1ClientResponse result = client.delete("/users/" + user.getId() + "/sessions/").request()) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testDeleteAllNotSuperuser() {
		// Even the user themselves cannot bulk-revoke their own sessions via this endpoint.
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = client.delete("/users/" + user.getId() + "/sessions/").request()) {
			assertThat(result).hasStatus(403);
		}
		verify(directory, never()).revokeAllSessions(any());
	}

	@Test
	public void testDeleteAllSuperuser() {
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(directory.revokeAllSessions("auth0|tester")).thenReturn(true);
		try (Http1ClientResponse result = client.delete("/users/" + user.getId() + "/sessions/").request()) {
			assertThat(result).hasStatus(204);
		}
		verify(directory).revokeAllSessions("auth0|tester");
	}

	@Test
	public void testDeleteAllUserNotFound() {
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		try (Http1ClientResponse result = client.delete("/users/@nobody/sessions/").request()) {
			assertThat(result).hasStatus(404);
		}
	}
}
