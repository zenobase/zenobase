package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;
import com.google.common.base.Strings;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;
import com.zenobase.search.QueryConstraint;
import com.zenobase.services.AuthorizationRepository;
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

	public Result find(String query, boolean clientOnly, int offset, int limit) {
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
			? ok(AuthorizationList.toJson(authorizations.find(constraint.getField(), constraint.getValue(), clientOnly, offset, limit)))
			: ok(AuthorizationList.toJson(authorizations.find(offset, limit)));
    }

	private static boolean isConstrainedToPrincipal(QueryConstraint constraint, Identity principal) {
		return constraint != null
			&& Authorization.PRINCIPAL.getName().equals(constraint.getField())
			&& principal.getId().equals(constraint.getValue());
	}
}
