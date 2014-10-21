package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;

import com.zenobase.commands.CreateCredentialsCommand;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsQuery;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;
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
	public CredentialsListController(AuthorizationContext security, CommandDispatcher dispatcher,
		CredentialsManagerRegistry registry, CredentialsRepository credentials, UserRepository users) {

		super(security);
		this.dispatcher = dispatcher;
		this.registry = registry;
		this.credentials = credentials;
		this.users = users;
	}

	public Result findAll(String q, int offset, int limit) {
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
		if (!users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		CredentialsQuery query = new CredentialsQuery();
		if (q != null) {
			query = query.queryString(q);
		}
		return ok(CredentialsList.toJson(credentials.find(query, offset, limit)));
    }

	public Result findByUser(String userId, String q, int offset, int limit) {
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
		CredentialsQuery query = new CredentialsQuery().principalEqualTo(principal);
		if (q != null) {
			query = query.queryString(q);
		}
		return ok(CredentialsList.toJson(credentials.find(query, offset, limit)));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result post() {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		CreateCredentialsForm form = new CreateCredentialsForm(body());
		if (!form.valid()) {
			return badRequest();
		}
		CredentialsManager manager = registry.find(form.getType());
		if (manager == null) {
			return badRequest("unknown type");
		}
		if (credentials.find(auth.getPrincipal(), form.getType()) != null) {
			return badRequest("already connected");
		}
    	Credentials credentials = manager.newCredentials(auth.getPrincipal());
    	String commandId = dispatcher.dispatch(new CreateCredentialsCommand(auth.getPrincipal(), credentials));
    	response().setHeader(COMMAND_ID, commandId);
    	response().setHeader(LOCATION, com.zenobase.controllers.routes.CredentialsController.get(credentials.getId()).toString());
        return created(credentials.sanitized().toJson());
    }
}
