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

public class AuthorizationListControllerFindAllTest extends AuthorizationListControllerTestSupport {

	@Test
	public void test() {
		PartialList<Authorization> list = DefaultPartialList.of();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(authorizations.find(new AuthorizationQuery().queryString("scope:*"), 0, 10))
				.thenReturn(list);
		try (Http1ClientResponse result = call("scope:*", 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(AuthorizationList.toJson(list));
		}
	}

	@Test
	public void testWithoutAuthorization() {
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testWithScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testWithNormalUser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String q, int offset, int limit) {
		var request = client.get("/authorizations/")
				.queryParam("offset", String.valueOf(offset))
				.queryParam("limit", String.valueOf(limit));
		if (q != null) {
			request = request.queryParam("q", q);
		}
		return request.request();
	}
}
