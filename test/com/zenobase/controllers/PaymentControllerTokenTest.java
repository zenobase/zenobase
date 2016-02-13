package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class PaymentControllerTokenTest extends PaymentControllerTestSupport {

	@Test
	public void test() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		when(payments.token(user.getName())).thenReturn("xxx");
		Result result = call();
		assertThat(result).hasStatus(OK).hasContent(Nodes.newObject("value", "xxx"));
	}

	@Test
	public void testUnauthorized() {
		Result result = call();
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(payments);
	}

	@Test
	public void testUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call();
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(payments);
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call();
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(payments);
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.PaymentController.token());
	}
}
