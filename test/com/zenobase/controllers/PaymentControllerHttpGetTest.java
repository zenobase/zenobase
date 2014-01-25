package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class PaymentControllerHttpGetTest extends PaymentControllerTestSupport {

	@Test
	public void testGetPayment() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		when(payments.findPayment(user.getName())).thenReturn(payment);
		Result result = call(user.getName());
		assertThat(result).hasStatus(OK).hasContent(payment.toJson());
	}

	@Test
	public void testGetPaymentNone() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
	}

	@Test
	public void testGetPaymentUnauthorized() {
		Result result = call(user.getName());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetPaymentUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(user.getName());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetPaymentWithInvalidAuthorization() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testGetPaymentWithScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private Result call(String username) {
		return callAction(com.zenobase.controllers.routes.ref.PaymentController.get('@' + username));
	}
}
