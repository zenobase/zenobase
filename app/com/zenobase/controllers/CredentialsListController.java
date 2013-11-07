package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import com.google.common.base.Strings;

import com.zenobase.commands.CreateCredentialsCommand;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.search.QueryConstraint;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsRepository;
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

	public Result find(String query, int offset, int limit) {
		if (limit > 100) {
			return badRequest("limit can't be more than 100");
		}
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		if (auth.getScope() != null) {
    		return forbidden();
		}
		QueryConstraint constraint = null;
		if (!Strings.isNullOrEmpty(query)) {
			try {
				constraint = QueryConstraint.parse(query);
			} catch (IllegalArgumentException e) {
				return badRequest("query is malformed");
			}
		}
		if (!isConstrainedToPrincipal(constraint, auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		return constraint != null
			? ok(CredentialsList.toJson(credentials.find(constraint.getField(), constraint.getValue(), offset, limit)))
			: ok(CredentialsList.toJson(credentials.find(offset, limit)));
    }

	private static boolean isConstrainedToPrincipal(QueryConstraint constraint, Identity principal) {
		return constraint != null
			&& Credentials.PRINCIPAL.getName().equals(constraint.getField())
			&& principal.getId().equals(constraint.getValue());
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
    	dispatcher.dispatch(new CreateCredentialsCommand(auth.getPrincipal(), credentials));
    	response().setHeader(LOCATION, com.zenobase.controllers.routes.CredentialsController.get(credentials.getId()).toString());
        return created(credentials.sanitized().toJson());
    }
}
