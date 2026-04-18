package com.zenobase.controllers;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

import com.zenobase.commands.CreateCredentialsCommand;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.CredentialsQuery;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsList;
import com.zenobase.tasks.CredentialsManager;
import com.zenobase.tasks.CredentialsManagerRegistry;

public class CredentialsListController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final CredentialsManagerRegistry registry;
	private final CredentialsRepository credentials;
	private final UserRepository users;

	@Inject
	public CredentialsListController(
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

	public void findAll(ServerRequest req, ServerResponse res) {
		String q = req.query().first("q").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		if (offset < 0 || offset > 1000) {
			sendBadRequest(res, "expected offset in [0..1000]");
			return;
		}
		if (limit < 0 || limit > 100) {
			sendBadRequest(res, "expected limit in [0..100]");
			return;
		}
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		var query = new CredentialsQuery();
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, CredentialsList.toJson(credentials.find(query, offset, limit)));
	}

	public void findByUser(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		String q = req.query().first("q").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		if (offset < 0 || offset > 1000) {
			sendBadRequest(res, "expected offset in [0..1000]");
			return;
		}
		if (limit < 0 || limit > 100) {
			sendBadRequest(res, "expected limit in [0..100]");
			return;
		}
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
		if (!auth.getPrincipal().equals(principal) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		var query = new CredentialsQuery().principalEqualTo(principal).order(Credentials.TYPE.getName(), true);
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, CredentialsList.toJson(credentials.find(query, offset, limit)));
	}

	public void post(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		var form = new CreateCredentialsForm(body(req));
		if (!form.valid()) {
			sendBadRequest(res, "bad request");
			return;
		}
		if (!registry.exists(form.getType())) {
			sendBadRequest(res, "unknown type");
			return;
		}
		CredentialsManager manager = registry.find(form.getType());
		if (credentials.find(auth.getPrincipal(), form.getType()) != null) {
			sendBadRequest(res, "already connected");
			return;
		}
		Credentials credentials = manager.newCredentials(auth.getPrincipal());
		String commandId = dispatcher.dispatch(new CreateCredentialsCommand(auth.getPrincipal(), credentials));
		setHeader(res, COMMAND_ID, commandId);
		setHeader(res, LOCATION, "/credentials/" + credentials.getId());
		sendCreated(res, credentials.sanitized().toJson());
	}
}
