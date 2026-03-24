package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class PaymentControllerTokenTest extends PaymentControllerTestSupport {

	@Test
	public void test() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(payments.token(user.getName())).thenReturn("xxx");
		try (Http1ClientResponse result = call()) {
			assertThat(result).hasStatus(200).hasContent(Nodes.newObject("value", "xxx"));
		}
	}

	@Test
	public void testUnauthorized() {
		try (Http1ClientResponse result = call()) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(payments);
		}
	}

	@Test
	public void testUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call()) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(payments);
		}
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call()) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(payments);
		}
	}

	private Http1ClientResponse call() {
		return client.post("/payments/token").request();
	}
}
