package com.zenobase.controllers;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import play.mvc.BodyParser;
import play.mvc.Result;

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

	public Result get(String credentialsId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Credentials credentials = this.credentials.find(credentialsId);
		if (credentials == null) {
			return notFound();
		}
		if (!credentials.isPermitted(auth)) {
			return forbidden();
		}
    	return ok(credentials.sanitized().toJson());
    }

	@BodyParser.Of(BodyParser.Json.class)
	public Result update(String credentialsId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Credentials credentials = this.credentials.find(credentialsId);
		if (credentials == null) {
			return notFound();
		}
    	if (!credentials.isPermitted(auth)) {
    		return forbidden();
    	}
    	CredentialsManager manager = registry.find(credentials.getType());
    	if (manager == null) {
    		return badRequest("unsupported credentials type: " + credentials.getType());
    	}
    	ObjectNode body = body();
    	if (Nodes.size(body) != 1) {
    		return badRequest("expected a single property");
    	}
    	Command command = null;
    	ObjectNode config = Credentials.CREDENTIALS.getValue(body);
    	if (config == null) {
    		return badRequest("no credentials specified");
    	}
    	if (credentials.isAuthorized()) {
    		return badRequest("credentials are already authorized");
    	}
    	command = manager.authorize(credentials, config);
    	if (command == null) {
    		return badRequest("nothing to do");
    	}
    	try {
    		String commandId = dispatcher.dispatch(command);
    		response().setHeader(COMMAND_ID, commandId);
    		return noContent();
		} catch (VersionConflictEngineException e) {
			return conflict("credentials are stale");
		}
    }

    public Result delete(String credentialsId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
		Credentials credentials = this.credentials.find(credentialsId);
		if (credentials == null) {
			return notFound();
		}
    	if (!credentials.isPermitted(auth) && !users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	String commandId = dispatcher.dispatch(new DeleteCredentialsCommand(auth.getPrincipal(), credentials));
		response().setHeader(COMMAND_ID, commandId);
    	return noContent();
    }
}
