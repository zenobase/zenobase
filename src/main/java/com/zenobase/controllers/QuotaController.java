package com.zenobase.controllers;

import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import com.zenobase.commands.Command;
import com.zenobase.commands.SpendQuotaCommand;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.QuotaManager;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class QuotaController extends ControllerSupport {

	private final UserRepository users;
	private final QuotaManager quotas;
	private final CommandDispatcher dispatcher;

	@Inject
	public QuotaController(AuthorizationContext security, UserRepository users, QuotaManager quotas, CommandDispatcher dispatcher) {
		super(security);
		this.users = users;
		this.quotas = quotas;
		this.dispatcher = dispatcher;
	}

	public void get(ServerRequest req, ServerResponse res) {
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
    	Identity principal = new UserLookup(users).getIdentity(userId);
    	if (principal == null) {
    		sendNotFound(res, "user not found");
    		return;
    	}
    	if (!principal.equals(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
    		sendForbidden(res);
    		return;
    	}
		sendOk(res, quotas.getQuota(principal).toJson());
    }

	public void post(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		ObjectNode body = body(req);
		Authorization auth = getCurrentAuthorization(req);
    	if (auth == null) {
    		sendUnauthorized(res);
    		return;
    	}
    	if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
    		sendForbidden(res);
    		return;
    	}
		Identity principal = new UserLookup(users).getIdentity(userId);
    	if (principal == null) {
    		sendNotFound(res, "user not found");
    		return;
    	}
    	int cost = body.path("cost").intValue();
    	if (cost == 0) {
    		sendBadRequest(res, "bad request");
    		return;
    	}
    	Command command = new SpendQuotaCommand(principal, cost);
    	dispatcher.dispatch(command);
		setHeader(res, COMMAND_ID, command.getId());
    	sendNoContent(res);
	}
}
