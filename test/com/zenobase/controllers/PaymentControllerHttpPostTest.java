package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import java.math.BigDecimal;

import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Result;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.models.Payment;
import com.zenobase.models.Plan;
import com.zenobase.oauth.Authorization;

public class PaymentControllerHttpPostTest extends PaymentControllerTestSupport {

	@Test
	public void testPay() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		user.setVerified(true);
		Result result = call(payment);
		assertThat(result).hasStatus(OK);
		verify(payments).subscribe(user.getName(), user.getEmail(), payment, Plan.PERSONAL);
		verify(dispatcher).dispatch(Mockito.any(ChangeQuotaCommand.class));
	}

	@Test
	public void testPayWithExistingCard() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		user.setVerified(true);
		Payment payment = new Payment(new BigDecimal("5.00"));
		Result result = call(payment);
		assertThat(result).hasStatus(OK);
		verify(payments).subscribe(user.getName(), user.getEmail(), payment, Plan.PERSONAL);
		verify(dispatcher).dispatch(Mockito.any(ChangeQuotaCommand.class));
	}

	@Test
	public void testPayUnauthorized() {
		Result result = call(payment);
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testPayUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(payment);
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testPayWithScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(payment);
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testPayWithUserUnverified() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(payment);
		assertThat(result).hasStatus(CONFLICT);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testPayWithPriceMissing() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		user.setVerified(true);
		Result result = call(new Payment(Nodes.newObject()));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(payments, dispatcher);
	}

	private Result call(Payment payment) {
		return callAction(com.zenobase.controllers.routes.ref.PaymentController.pay(), fakeRequest().withJsonBody(payment.toJson()));
	}
}
