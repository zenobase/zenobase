package com.zenobase.controllers;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;
import play.mvc.Result;

import com.zenobase.commands.ChangeQuotaCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Payment;
import com.zenobase.models.Plan;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.EventRepository;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class PaymentController extends ControllerSupport {

	private final PaymentGateway payments;
	private final UserRepository users;
	private final EventRepository events;
	private final CommandDispatcher dispatcher;

	@Inject
	public PaymentController(AuthorizationContext security, PaymentGateway payments, UserRepository users, EventRepository events, CommandDispatcher dispatcher) {
		super(security);
		this.payments = payments;
		this.users = users;
		this.events = events;
		this.dispatcher = dispatcher;
	}

	public Result token() {
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
		return ok(Nodes.newObject("value", payments.token(user.getName())));
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
		Payment payment = new Payment(body);
		Plan plan = Plan.getPlan(payment.getPrice());
		if (plan == null) {
			return badRequest("no matching plan");
		}
		if (hasData(user)) {
			payments.subscribe(user.getName(), user.getEmail(), payment, plan);
			dispatcher.dispatch(new ChangeQuotaCommand(auth.getPrincipal(), user.getName(), user.getQuota(), plan.getQuota()));
		} else {
			Logger.warn("Ignoring payment attempt from user without data: {}", user.getName());
		}
		return ok();
	}

	private boolean hasData(User user) {
		return events.size(user.asIdentity()) > 0;
	}

	public Result cancel(String userId) {
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
		payments.unsubscribe(user.getName());
		dispatcher.dispatch(new ChangeQuotaCommand(auth.getPrincipal(), user.getName(), user.getQuota(), null));
		return ok();
	}
}
