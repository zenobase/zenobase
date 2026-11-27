package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.auth.Passkey;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import io.helidon.webclient.http1.Http1ClientResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Passkeys are self-service only: unlike closing an account, superusers and external tokens are rejected. */
public class AccountControllerPasskeyTest extends AccountControllerTestSupport {

	@Test
	public void testListOwnPasskeys() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		when(identityProvider.listPasskeys(user)).thenReturn(
			List.of(new Passkey("pk-1", null, "2026-01-01", null, null))
		);
		try (Http1ClientResponse result = client.get(path()).request()) {
			assertThat(result).hasStatus(200);
		}
	}

	@Test
	public void testListPasskeysOfOtherUserForbiddenForSuperuser() {
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.find(user.getName())).thenReturn(user);
		when(users.isSuperuser(superuser)).thenReturn(true);
		try (Http1ClientResponse result = client.get(path()).request()) {
			assertThat(result).hasStatus(403);
		}
		verifyNoInteractions(identityProvider);
	}

	@Test
	public void testDeleteOwnPasskey() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = client.delete(path() + "/pk-1").request()) {
			assertThat(result).hasStatus(204);
		}
		verify(identityProvider).deletePasskey(user, "pk-1");
	}

	@Test
	public void testDeletePasskeyWithExternalTokenForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity("client"), "external"));
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = client.delete(path() + "/pk-1").request()) {
			assertThat(result).hasStatus(403);
		}
		verifyNoInteractions(identityProvider);
	}

	@Test
	public void testDeleteUnknownPasskey() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		doThrow(new IllegalArgumentException()).when(identityProvider).deletePasskey(user, "pk-1");
		try (Http1ClientResponse result = client.delete(path() + "/pk-1").request()) {
			assertThat(result).hasStatus(404);
		}
	}

	private String path() {
		return "/users/@" + user.getName() + "/passkeys";
	}
}
