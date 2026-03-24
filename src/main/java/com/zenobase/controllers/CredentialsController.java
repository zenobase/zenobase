package com.zenobase.controllers;

import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;

import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteCredentialsCommand;
import com.zenobase.json.Nodes;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsManager;
import com.zenobase.tasks.CredentialsManagerRegistry;

public class CredentialsController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final CredentialsManagerRegistry registry;
	private final CredentialsRepository credentials;
	private final UserRepository users;

	@Inject
	public CredentialsController(AuthorizationContext security, CommandDispatcher dispatcher,
		CredentialsManagerRegistry registry, CredentialsRepository credentials, UserRepository users) {

		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.credentials = credentials;
		this.users = users;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String credentialsId = req.path().pathParameters().get("credentialsId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Credentials credentials = this.credentials.find(credentialsId);
		if (credentials == null) {
			sendNotFound(res);
			return;
		}
		if (!credentials.isPermitted(auth)) {
			sendForbidden(res);
			return;
		}
    	sendOk(res, credentials.sanitized().toJson());
    }

	public void update(ServerRequest req, ServerResponse res) {
		String credentialsId = req.path().pathParameters().get("credentialsId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Credentials credentials = this.credentials.find(credentialsId);
		if (credentials == null) {
			sendNotFound(res);
			return;
		}
    	if (!credentials.isPermitted(auth)) {
    		sendForbidden(res);
    		return;
    	}
    	CredentialsManager manager = registry.find(credentials.getType());
    	if (manager == null) {
    		sendBadRequest(res, "unsupported credentials type: " + credentials.getType());
    		return;
    	}
    	ObjectNode body = body(req);
    	if (Nodes.size(body) != 1) {
    		sendBadRequest(res, "expected a single property");
    		return;
    	}
    	Command command = null;
    	ObjectNode config = Credentials.CREDENTIALS.getValue(body);
    	if (config == null) {
    		sendBadRequest(res, "no credentials specified");
    		return;
    	}
    	if (credentials.isAuthorized()) {
    		sendBadRequest(res, "credentials are already authorized");
    		return;
    	}
    	command = manager.authorize(credentials, config);
    	if (command == null) {
    		sendBadRequest(res, "nothing to do");
    		return;
    	}
    	try {
    		String commandId = dispatcher.dispatch(command);
    		setHeader(res, COMMAND_ID, commandId);
    		sendNoContent(res);
		} catch (OpenSearchException e) {
			if (e.status() == 409) {
				sendConflict(res, "credentials are stale");
			} else {
				throw e;
			}
		}
    }

    public void delete(ServerRequest req, ServerResponse res) {
		String credentialsId = req.path().pathParameters().get("credentialsId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Credentials credentials = this.credentials.find(credentialsId);
		if (credentials == null) {
			sendNotFound(res);
			return;
		}
    	if (!credentials.isPermitted(auth) && !users.isSuperuser(auth.getPrincipal())) {
    		sendForbidden(res);
    		return;
    	}
    	String commandId = dispatcher.dispatch(new DeleteCredentialsCommand(auth.getPrincipal(), credentials));
		setHeader(res, COMMAND_ID, commandId);
    	sendNoContent(res);
    }
}
