package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class PaymentControllerHttpDeleteTest extends PaymentControllerTestSupport {

	@Test
	public void testCancel() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(200);
			verify(payments).unsubscribe(user.getName());
			verify(dispatcher).dispatch(ArgumentMatchers.any(ChangeQuotaCommand.class));
		}
	}

	@Test
	public void testCancelUnauthorized() {
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(payments, dispatcher);
		}
	}

	@Test
	public void testCancelUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(payments, dispatcher);
		}
	}

	@Test
	public void testCancelWithScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		when(users.find(user.getName())).thenReturn(user);
		try (Http1ClientResponse result = call(user.getName())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(payments, dispatcher);
		}
	}

	private Http1ClientResponse call(String userId) {
		return client.delete("/users/@" + userId + "/payment").request();
	}
}
