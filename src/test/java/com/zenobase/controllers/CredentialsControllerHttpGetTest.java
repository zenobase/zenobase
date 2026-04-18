package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Credentials;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

public class CredentialsControllerHttpGetTest extends CredentialsControllerTestSupport {

	private final String type = "test";
	private final Credentials credentials = new Credentials(type, principal);

	@Test
	public void test() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		when(repository.find(credentials.getId())).thenReturn(credentials);
		try (Http1ClientResponse result = call(credentials.getId())) {
			assertThat(result).hasStatus(200).hasContent(credentials.toJson());
		}
	}

	@Test
	public void testUnauthorized() {
		try (Http1ClientResponse result = call(credentials.getId())) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(principal));
		try (Http1ClientResponse result = call(credentials.getId())) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(repository.find(credentials.getId())).thenReturn(credentials);
		try (Http1ClientResponse result = call(credentials.getId())) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String credentialsId) {
		return client.get("/credentials/" + credentialsId).request();
	}
}
