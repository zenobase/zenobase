package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class AuthorizationListController extends ControllerSupport {

	private final AuthorizationRepository authorizations;
	private final UserRepository users;

	@Inject
	public AuthorizationListController(AuthorizationContext security, AuthorizationRepository authorizations, UserRepository users) {
		super(security);
		this.authorizations = authorizations;
		this.users = users;
	}

	public Result find(String field, String value, boolean clientOnly, int offset, int limit) {
		if (limit > 100) {
			return badRequest("limit can't be more than 100");
		}
		Authorization auth = getCurrentAuthorization();
    	if (auth == null || auth.getScope() != null) {
    		return unauthorized();
    	}
		if (!isConstrainedToPrincipal(field, value, auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		return ok(authorizations.find(field, value, clientOnly, offset, limit).toJson());
    }

	private boolean isConstrainedToPrincipal(String field, String value, Identity principal) {
		return Authorization.PRINCIPAL.getName().equals(field) && principal.getId().equals(value);
	}
}
