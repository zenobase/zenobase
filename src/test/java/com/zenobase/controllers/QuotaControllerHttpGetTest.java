package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Quota;

public class QuotaControllerHttpGetTest extends QuotaControllerTestSupport {

	@Test
	public void test() {
		Quota expected = new Quota(1000, 50);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(quotas.getQuota(user.asIdentity())).thenReturn(expected);
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(200).hasContent(expected.toJson());
		}
	}

	@Test
	public void testUnauthorized() {
		try (Http1ClientResponse result = call(user.getId())) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call("@nobody")) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testForbidden() {
		Identity someone = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(someone.id())) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testSuperuser() {
		Identity someone = new Identity();
		Quota expected = new Quota(1000, 50);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(quotas.getQuota(someone)).thenReturn(expected);
		try (Http1ClientResponse result = call(someone.id())) {
			assertThat(result).hasStatus(200).hasContent(expected.toJson());
		}
	}

	private Http1ClientResponse call(String userId) {
		return client.get("/users/" + userId + "/quota").request();
	}
}
