package com.zenobase.controllers;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

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
	public AuthorizationController(
			AuthorizationContext security,
			CommandDispatcher dispatcher,
			AuthorizationRepository authorizations,
			UserRepository users) {
		super(security);
		this.dispatcher = dispatcher;
		this.authorizations = authorizations;
		this.users = users;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String authId = req.path().pathParameters().get("authId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Authorization authorization = authorizations.find(authId);
		if (authorization == null) {
			sendNotFound(res);
			return;
		}
		if (!authorization.isPermitted(auth)) {
			sendForbidden(res);
			return;
		}
		sendOk(res, authorization.toJson());
	}

	public void delete(ServerRequest req, ServerResponse res) {
		String authId = req.path().pathParameters().get("authId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Authorization authorization = authorizations.find(authId);
		if (authorization == null) {
			sendNotFound(res);
			return;
		}
		if (!authorization.isPermitted(auth) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		String commandId = dispatcher.dispatch(new DeleteAuthorizationCommand(auth.getPrincipal(), authorization));
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}
}
