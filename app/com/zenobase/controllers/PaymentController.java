package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.models.Card;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class PaymentController extends ControllerSupport {

	private final PaymentGateway payments;
	private final UserRepository users;

	@Inject
	public PaymentController(AuthorizationContext security, PaymentGateway payments, UserRepository users) {
		super(security);
		this.payments = payments;
		this.users = users;
	}

	public Result findCards(String userId) {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null) {
    		return forbidden();
    	}
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			return notFound("user not found");
		}
		if (!auth.getPrincipal().equals(user.asIdentity())) {
			return forbidden();
		}
		ArrayNode cards = Nodes.newArray();
		String token = payments.findPaymentMethodToken(user.getName());
		if (token != null) {
			cards.add(token);
		}
		return ok(cards);
    }

	public Result pay() {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null) {
    		return forbidden();
    	}
		User user = users.find(auth.getPrincipal());
		if (user == null) {
			return notFound("user not found");
		}
		ObjectNode body = body();
		Card card = new Card(body);
		payments.subscribe(user.getName(), user.getEmail(), card, "");

		return ok();
	}
}
