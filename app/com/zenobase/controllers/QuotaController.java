package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

	public Result get(String userId) {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null) {
    		return forbidden();
    	}
    	Identity principal = new UserLookup(users).getIdentity(userId);
    	if (principal == null) {
    		return notFound("user not found");
    	}
    	if (!principal.equals(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
		return ok(quotas.getQuota(principal).toJson());
    }

	@BodyParser.Of(BodyParser.Json.class)
	public Result post(String userId) {
		ObjectNode body = body();
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
		Identity principal = new UserLookup(users).getIdentity(userId);
    	if (principal == null) {
    		return notFound("user not found");
    	}
    	int cost = body.path("cost").intValue();
    	if (cost == 0) {
    		return badRequest();
    	}
    	Command command = new SpendQuotaCommand(principal, cost);
    	dispatcher.dispatch(command);
		response().setHeader(COMMAND_ID, command.getId());
    	return noContent();
	}
}
