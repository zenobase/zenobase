package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.models.Payment;
import com.zenobase.models.Plan;
import com.zenobase.oauth.Authorization;

public class PaymentControllerHttpPostTest extends PaymentControllerTestSupport {

	@Test
	public void testPay() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(events.size(user.asIdentity())).thenReturn(1L);
		user.setVerified(true);
		try (Http1ClientResponse result = call(payment)) {
			assertThat(result).hasStatus(200);
			verify(payments)
					.subscribe(
							eq(user.getName()),
							eq(user.getEmail()),
							argThat(p -> p.getPrice().compareTo(payment.getPrice()) == 0),
							eq(Plan.PERSONAL));
			verify(dispatcher).dispatch(ArgumentMatchers.any(ChangeQuotaCommand.class));
		}
	}

	@Test
	public void testPayWithExistingCard() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(events.size(user.asIdentity())).thenReturn(1L);
		user.setVerified(true);
		Payment payment = new Payment(new BigDecimal("5.00"));
		try (Http1ClientResponse result = call(payment)) {
			assertThat(result).hasStatus(200);
			verify(payments)
					.subscribe(
							eq(user.getName()),
							eq(user.getEmail()),
							argThat(p -> p.getPrice().compareTo(payment.getPrice()) == 0),
							eq(Plan.PERSONAL));
			verify(dispatcher).dispatch(ArgumentMatchers.any(ChangeQuotaCommand.class));
		}
	}

	@Test
	public void testPayUnauthorized() {
		try (Http1ClientResponse result = call(payment)) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(payments, dispatcher);
		}
	}

	@Test
	public void testPayUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(payment)) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(payments, dispatcher);
		}
	}

	@Test
	public void testPayWithScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(payment)) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(payments, dispatcher);
		}
	}

	@Test
	public void testPayWithUserUnverified() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(payment)) {
			assertThat(result).hasStatus(409);
			verifyNoInteractions(payments, dispatcher);
		}
	}

	@Test
	public void testPayWithPriceMissing() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		user.setVerified(true);
		try (Http1ClientResponse result = call(new Payment(Nodes.newObject()))) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(payments, dispatcher);
		}
	}

	@Test
	public void testPayWithEmptyAccount() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(events.size(user.asIdentity())).thenReturn(0L);
		user.setVerified(true);
		try (Http1ClientResponse result = call(payment)) {
			assertThat(result).hasStatus(200);
			verifyNoInteractions(payments, dispatcher);
		}
	}

	private Http1ClientResponse call(Payment payment) {
		return client.post("/payments/").submit(payment.toJson());
	}
}
