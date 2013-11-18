package com.zenobase.controllers;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.Period;
import play.mvc.Result;

import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.common.Callback;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;
import com.zenobase.services.AuthorizationQuery;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class AuthorizationListController extends ControllerSupport {

	private final AuthorizationRepository authorizations;
	private final CommandDispatcher dispatcher;
	private final UserRepository users;

	@Inject
	public AuthorizationListController(AuthorizationContext security, AuthorizationRepository authorizations, CommandDispatcher dispatcher,UserRepository users) {
		super(security);
		this.authorizations = authorizations;
		this.dispatcher = dispatcher;
		this.users = users;
	}

	public Result findAll(int offset, int limit) {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null) {
    		return forbidden();
    	}
		if (!users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		return ok(AuthorizationList.toJson(authorizations.find(offset, limit)));
    }

	public Result findByUser(String userId, Boolean hasClient, int offset, int limit) {
		if (offset < 0 || offset > 1000) {
			return badRequest("expected offset in [0..1000]");
		}
		if (limit < 0 || limit > 100) {
			return badRequest("expected limit in [0..100]");
		}
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
		if (!auth.getPrincipal().equals(principal) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		AuthorizationQuery query = new AuthorizationQuery()
			.principalEqualTo(principal)
			.clientNotNull(hasClient);
		return ok(AuthorizationList.toJson(authorizations.find(query, offset, limit)));
    }

	public Result delete() {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
		return delete(Period.months(1), auth.getPrincipal());
	}

	private Result delete(Period maxAge, final Identity principal) {
		final CompoundCommand command = new CompoundCommand(principal, "expired authorizations", "unexpired authorizations");
		AuthorizationQuery query = new AuthorizationQuery().createdBefore(DateTime.now().minus(maxAge));
		authorizations.find(query, new Callback<Authorization>() {
			@Override
			public void call(Authorization authorization) {
	    		command.add(new DeleteAuthorizationCommand(principal, authorization));
			}
		});
		if (!command.getCommands().isEmpty()) {
			String commandId = dispatcher.dispatch(command);
			response().setHeader(COMMAND_ID, commandId);
		}
		return noContent();
	}
}
