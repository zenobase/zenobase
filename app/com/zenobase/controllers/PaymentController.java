package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.models.Card;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class PaymentController extends ControllerSupport {

	private final PaymentGateway payments;
	private final UserRepository users;
	private final CommandDispatcher dispatcher;

	@Inject
	public PaymentController(AuthorizationContext security, PaymentGateway payments, UserRepository users, CommandDispatcher dispatcher) {
		super(security);
		this.payments = payments;
		this.users = users;
		this.dispatcher = dispatcher;
	}

	public Result get(String userId) {
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
		Card card = payments.findCard(user.getName());
		return card != null ? ok(card.toJson()) : noContent();
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
		if (!user.isVerified()) {
			return conflict("user not verified");
		}
		ObjectNode body = body();
		int quota = body.path("plan").intValue();
		if (quota == 0) {
			return badRequest("no plan specified");
		}
		JsonNode card = body.path("card");
		payments.subscribe(user.getName(), user.getEmail(), !card.isMissingNode() ? new Card((ObjectNode) card) : null, Integer.toString(quota));
		dispatcher.dispatch(new ChangeQuotaCommand(auth.getPrincipal(), user.getName(), user.getQuota(), quota));
		return ok();
	}
}
