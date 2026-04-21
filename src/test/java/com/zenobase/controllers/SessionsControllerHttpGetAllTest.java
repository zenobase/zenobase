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

public class SessionsControllerHttpGetAllTest extends SessionsControllerTestSupport {

	@Test
	public void testNotAuthorized() {
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testSuperuserNoFilterReturnsEmpty() {
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(200).asObjectNode().path("total").isEqualTo(0);
		}
	}

	@Test
	public void testSuperuserFiltersByUser() {
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.find(user.getName())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(directory.listSessions("auth0|tester")).thenReturn(
			List.of(
				Session.of("s1", "UA-1", "1.1.1.1", "2026-04-20T10:00:00Z", null),
				Session.of("s2", "UA-2", "2.2.2.2", "2026-04-19T10:00:00Z", null)
			)
		);
		try (Http1ClientResponse result = call('@' + user.getName(), 0, 10)) {
			assertThat(result).hasStatus(200);
			var obj = assertThat(result).asObjectNode();
			obj.path("total").isEqualTo(2);
			obj.path("sessions").isArray().hasSize(2);
			obj.path("sessions").path(0).path("userId").isEqualTo(user.getId());
			obj.path("sessions").path(0).path("username").isEqualTo(user.getName());
		}
	}

	@Test
	public void testLimitTooLow() {
		asSuperuser();
		try (Http1ClientResponse result = call(null, 0, -1)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testLimitTooHigh() {
		asSuperuser();
		try (Http1ClientResponse result = call(null, 0, 1000)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooLow() {
		asSuperuser();
		try (Http1ClientResponse result = call(null, -1, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooHigh() {
		asSuperuser();
		try (Http1ClientResponse result = call(null, 10000, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	private void asSuperuser() {
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
	}

	private Http1ClientResponse call(String userFilter, int offset, int limit) {
		var req = client
			.get("/sessions/")
			.queryParam("offset", String.valueOf(offset))
			.queryParam("limit", String.valueOf(limit));
		if (userFilter != null) {
			req = req.queryParam("user", userFilter);
		}
		return req.request();
	}
}
