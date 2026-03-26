package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationControllerHttpGetTest extends AuthorizationControllerTestSupport {

	private Authorization authorization = new Authorization(user.asIdentity(), null, Generator.id());

	@Test
	public void test() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		try (Http1ClientResponse result = call(authorization.getId())) {
			assertThat(result).hasStatus(200).hasContent(authorization.toJson());
		}
	}

	@Test
	public void testNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testUnauthorized() {
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		try (Http1ClientResponse result = call(authorization.getId())) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(authorizations.find(authorization.getId())).thenReturn(authorization);
		try (Http1ClientResponse result = call(authorization.getId())) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String id) {
		return client.get("/authorizations/" + id).request();
	}
}
