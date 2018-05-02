package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Result;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class PaymentControllerHttpDeleteTest extends PaymentControllerTestSupport {

	@Test
	public void testCancel() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(OK);
		verify(payments).unsubscribe(user.getName());
		verify(dispatcher).dispatch(Mockito.any(ChangeQuotaCommand.class));
	}

	@Test
	public void testCancelUnauthorized() {
		Result result = call(user.getName());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testCancelUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(user.getName());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testCancelWithScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(payments, dispatcher);
	}

	private Result call(String userId) {
		return callAction(com.zenobase.controllers.routes.ref.PaymentController.cancel('@' + userId));
	}
}
