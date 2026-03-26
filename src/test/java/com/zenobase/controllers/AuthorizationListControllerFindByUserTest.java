package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;
import com.zenobase.services.AuthorizationQuery;

public class AuthorizationListControllerFindByUserTest extends AuthorizationListControllerTestSupport {

	@Test
	public void test() {
		PartialList<Authorization> list = DefaultPartialList.of(new Authorization(user.asIdentity()));
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(authorizations.find(
						new AuthorizationQuery()
								.principalEqualTo(user.asIdentity())
								.clientNotNull(),
						0,
						10))
				.thenReturn(list);
		try (Http1ClientResponse result = call(user.getId(), Boolean.TRUE, null, 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(AuthorizationList.toJson(list));
		}
	}

	@Test
	public void testAsSuperuser() {
		Identity superuser = new Identity();
		PartialList<Authorization> list = DefaultPartialList.of(new Authorization(user.asIdentity()));
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(authorizations.find(
						new AuthorizationQuery()
								.principalEqualTo(user.asIdentity())
								.queryString("scope:*"),
						0,
						10))
				.thenReturn(list);
		try (Http1ClientResponse result = call(user.getId(), null, "scope:*", 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(AuthorizationList.toJson(list));
		}
	}

	@Test
	public void testLimitTooLow() {
		try (Http1ClientResponse result = call(user.getId(), null, null, 0, -1)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testLimitTooHigh() {
		try (Http1ClientResponse result = call(user.getId(), null, null, 0, 1000)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooLow() {
		try (Http1ClientResponse result = call(user.getId(), null, null, -1, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooHigh() {
		try (Http1ClientResponse result = call(user.getId(), null, null, 10000, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testMissingAuthorization() {
		try (Http1ClientResponse result = call(user.getId(), null, null, 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		try (Http1ClientResponse result = call(user.getId(), null, null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call("@none", null, null, 0, 10)) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testNotOwner() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		try (Http1ClientResponse result = call(user.getId(), null, null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String userId, Boolean hasClient, String q, int offset, int limit) {
		var request = client.get("/users/" + userId + "/authorizations/")
				.queryParam("offset", String.valueOf(offset))
				.queryParam("limit", String.valueOf(limit));
		if (hasClient != null) {
			request = request.queryParam("has_client", String.valueOf(hasClient));
		}
		if (q != null) {
			request = request.queryParam("q", q);
		}
		return request.request();
	}
}
