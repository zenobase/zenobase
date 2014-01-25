package com.zenobase.controllers;

import java.math.BigDecimal;

import javax.inject.Inject;

import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.models.Payment;
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
		Payment payment = payments.findPayment(user.getName());
		return payment != null ? ok(payment.toJson()) : noContent();
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
		if (body == null || body.size() == 0) {
			return badRequest("missing payment data");
		}
		payments.subscribe(user.getName(), user.getEmail(), new Payment(body).withPrice(new BigDecimal("5.00")), PaymentGateway.PLAN_PERSONAL);
		dispatcher.dispatch(new ChangeQuotaCommand(auth.getPrincipal(), user.getName(), user.getQuota(), 3000000));
		return ok();
	}
}
