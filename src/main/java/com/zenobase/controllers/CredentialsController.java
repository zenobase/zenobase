package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteCredentialsCommand;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsManager;
import com.zenobase.tasks.CredentialsManagerRegistry;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import org.opensearch.client.opensearch._types.OpenSearchException;

public class CredentialsController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final CredentialsManagerRegistry registry;
	private final CredentialsRepository credentials;
	private final UserRepository users;

	@Inject
	public CredentialsController(
		AuthorizationContext security,
		CommandDispatcher dispatcher,
		CredentialsManagerRegistry registry,
		CredentialsRepository credentials,
		UserRepository users
	) {
		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.credentials = credentials;
		this.users = users;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String credentialsId = req.path().pathParameters().get("credentialsId");
		Authorization auth = requireAuthorization(req);
		Credentials credentials = requireFound(this.credentials.find(credentialsId));
		requirePermission(credentials.isPermitted(auth));
		sendOk(res, credentials.sanitized().toJson());
	}

	public void update(ServerRequest req, ServerResponse res) {
		String credentialsId = req.path().pathParameters().get("credentialsId");
		Authorization auth = requireAuthorization(req);
		Credentials credentials = requireFound(this.credentials.find(credentialsId));
		requirePermission(credentials.isPermitted(auth));
		if (!registry.exists(credentials.getType())) {
			throw ResponseStatusException.badRequest("unsupported credentials type: " + credentials.getType());
		}
		CredentialsManager manager = registry.find(credentials.getType());
		ObjectNode body = body(req);
		if (body.size() != 1) {
			throw ResponseStatusException.badRequest("expected a single property");
		}
		ObjectNode config = Credentials.CREDENTIALS.getValue(body);
		if (config == null) {
			throw ResponseStatusException.badRequest("no credentials specified");
		}
		if (credentials.isAuthorized()) {
			throw ResponseStatusException.badRequest("credentials are already authorized");
		}
		Command command = manager.authorize(credentials, config);
		if (command == null) {
			throw ResponseStatusException.badRequest("nothing to do");
		}
		requireNotSuspended(auth);
		try {
			String commandId = dispatcher.dispatch(command);
			setHeader(res, COMMAND_ID, commandId);
			sendNoContent(res);
		} catch (OpenSearchException e) {
			if (e.status() == 409) {
				throw ResponseStatusException.conflict("credentials are stale");
			}
			throw e;
		}
	}

	public void delete(ServerRequest req, ServerResponse res) {
		String credentialsId = req.path().pathParameters().get("credentialsId");
		Authorization auth = requireAuthorization(req);
		Credentials credentials = requireFound(this.credentials.find(credentialsId));
		requirePermission(credentials.isPermitted(auth) || isFirstPartySuperuser(users, auth));
		requireNotSuspended(auth);
		String commandId = dispatcher.dispatch(new DeleteCredentialsCommand(auth.getPrincipal(), credentials));
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}
}
