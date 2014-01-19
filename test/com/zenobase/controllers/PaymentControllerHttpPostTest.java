package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Card;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class PaymentControllerHttpPostTest extends PaymentControllerTestSupport {

	private final int quota = 50000;

	@Test
	public void testPay() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		user.setVerified(true);
		Result result = call(body(quota, card));
		assertThat(result).hasStatus(OK);
		verify(payments).subscribe(user.getName(), user.getEmail(), card, Integer.toString(quota));
		verify(dispatcher).dispatch(Mockito.any(ChangeQuotaCommand.class));
	}

	@Test
	public void testPayWithExistingCard() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		user.setVerified(true);
		Result result = call(body(quota, null));
		assertThat(result).hasStatus(OK);
		verify(payments).subscribe(user.getName(), user.getEmail(), null, Integer.toString(quota));
		verify(dispatcher).dispatch(Mockito.any(ChangeQuotaCommand.class));
	}

	@Test
	public void testPayUnauthorized() {
		Result result = call(body(quota, card));
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testPayUserNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(body(quota, card));
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testPayWithScopedAuthorization() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity(), new Identity(), "xyz"));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(body(quota, card));
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testPayWithUserUnverified() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(body(quota, card));
		assertThat(result).hasStatus(CONFLICT);
		verifyZeroInteractions(payments, dispatcher);
	}

	@Test
	public void testPayWithQuotaMissing() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.find(user.asIdentity())).thenReturn(user);
		user.setVerified(true);
		Result result = call(body(0, card));
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(payments, dispatcher);
	}

	private static ObjectNode body(int quota, Card card) {
		ObjectNode body = Nodes.newObject();
		if (quota > 0) {
			body.put("plan", quota);
		}
		if (card != null) {
			body.put("card", card.toJson());
		}
		return body;
	}

	private Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.PaymentController.pay(), fakeRequest().withJsonBody(body));
	}
}
