package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.Card;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;

public class PaymentControllerHttpGetTest extends PaymentControllerTestSupport {

	private final User user = new User("jdoe");
	private final Card card = new Card("4111 1111 1111 1111", "100", "2050", "01");

	@Test
	public void testGetCard() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		when(payments.findCard(user.getName())).thenReturn(card);
		Result result = call(user.getName());
		assertThat(result).hasStatus(OK).hasContent(card.toJson());
	}

	@Test
	public void testGetCardNone() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(NO_CONTENT).isEmpty();
	}

	@Test
	public void testGetCardUnauthorized() {
		Result result = call(user.getName());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetCardUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(user.getName());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetCardWithInvalidAuthorization() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testGetCardWithScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		when(users.find(user.getName())).thenReturn(user);
		Result result = call(user.getName());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private Result call(String username) {
		return callAction(com.zenobase.controllers.routes.ref.PaymentController.get('@' + username));
	}
}
