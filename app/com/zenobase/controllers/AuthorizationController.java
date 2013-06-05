package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;

import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public class AuthorizationController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final AuthorizationRepository authorizations;
	private final UserRepository users;

	@Inject
	public AuthorizationController(AuthorizationContext security, CommandDispatcher dispatcher, AuthorizationRepository authorizations, UserRepository users) {
		super(security);
		this.dispatcher = dispatcher;
		this.authorizations = authorizations;
		this.users = users;
	}

	public Result get(String authId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Authorization authorization = authorizations.find(authId);
		if (authorization == null) {
			return notFound();
		}
		if (!authorization.isPermitted(auth)) {
			return forbidden();
		}
    	return ok(authorization.toJson());
    }

    public Result delete(String authId) {
    	Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Authorization authorization = authorizations.find(authId);
		if (authorization == null) {
			return notFound();
		}
    	if (!authorization.isPermitted(auth) && !users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	String commandId = dispatcher.dispatch(new DeleteAuthorizationCommand(auth.getPrincipal(), authorization));
		response().setHeader(COMMAND_ID, commandId);
    	return noContent();
    }
}
