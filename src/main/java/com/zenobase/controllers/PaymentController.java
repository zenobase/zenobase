package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

	private final PaymentGateway payments;
	private final UserRepository users;
	private final EventRepository events;
	private final CommandDispatcher dispatcher;

	@Inject
	public PaymentController(
			AuthorizationContext security,
			PaymentGateway payments,
			UserRepository users,
			EventRepository events,
			CommandDispatcher dispatcher) {
		super(security);
		this.payments = payments;
		this.users = users;
		this.events = events;
		this.dispatcher = dispatcher;
	}

	public void token(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		User user = users.find(auth.getPrincipal());
		if (user == null) {
			sendNotFound(res, "user not found");
			return;
		}
		sendOk(res, Nodes.newObject("value", payments.token(user.getName())));
	}

	public void pay(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		User user = users.find(auth.getPrincipal());
		if (user == null) {
			sendNotFound(res, "user not found");
			return;
		}
		if (!user.isVerified()) {
			sendConflict(res, "user not verified");
			return;
		}
		ObjectNode body = body(req);
		if (body == null || body.size() == 0) {
			sendBadRequest(res, "missing payment data");
			return;
		}
		var payment = new Payment(body);
		Plan plan = Plan.getPlan(payment.getPrice());
		if (plan == null) {
			sendBadRequest(res, "no matching plan");
			return;
		}
		if (hasData(user)) {
			payments.subscribe(user.getName(), user.getEmail(), payment, plan);
			dispatcher.dispatch(
					new ChangeQuotaCommand(auth.getPrincipal(), user.getName(), user.getQuota(), plan.getQuota()));
		} else {
			logger.warn("Ignoring payment attempt from user without data: {}", user.getName());
		}
		sendOk(res);
	}

	private boolean hasData(User user) {
		return events.size(user.asIdentity()) > 0;
	}

	public void cancel(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			sendNotFound(res, "user not found");
			return;
		}
		if (!auth.getPrincipal().equals(user.asIdentity())) {
			sendForbidden(res);
			return;
		}
		payments.unsubscribe(user.getName());
		dispatcher.dispatch(new ChangeQuotaCommand(auth.getPrincipal(), user.getName(), user.getQuota(), null));
		sendOk(res);
	}
}
