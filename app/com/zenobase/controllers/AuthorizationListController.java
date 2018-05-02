package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;
import com.zenobase.services.AuthorizationQuery;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class AuthorizationListController extends ControllerSupport {

	private final AuthorizationRepository authorizations;
	private final UserRepository users;

	@Inject
	public AuthorizationListController(AuthorizationContext security, AuthorizationRepository authorizations, UserRepository users) {
		super(security);
		this.authorizations = authorizations;
		this.users = users;
	}

	public Result findAll(String q, int offset, int limit) {
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
		AuthorizationQuery query = new AuthorizationQuery();
		if (q != null) {
			query = query.queryString(q);
		}
		return ok(AuthorizationList.toJson(authorizations.find(query, offset, limit)));
    }

	public Result findByUser(String userId, Boolean hasClient, String q, int offset, int limit) {
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
		if (q != null) {
			query = query.queryString(q);
		}
		return ok(AuthorizationList.toJson(authorizations.find(query, offset, limit)));
    }
}
