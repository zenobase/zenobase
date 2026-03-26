package com.zenobase.controllers;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

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
	public AuthorizationListController(
			AuthorizationContext security, AuthorizationRepository authorizations, UserRepository users) {
		super(security);
		this.authorizations = authorizations;
		this.users = users;
	}

	public void findAll(ServerRequest req, ServerResponse res) {
		String q = req.query().first("q").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));

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
		var query = new AuthorizationQuery();
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, AuthorizationList.toJson(authorizations.find(query, offset, limit)));
	}

	public void findByUser(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		Boolean hasClient = req.query().first("has_client").isPresent()
				? Boolean.valueOf(req.query().first("has_client").get())
				: null;
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
		var query = new AuthorizationQuery().principalEqualTo(principal).clientNotNull(hasClient);
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, AuthorizationList.toJson(authorizations.find(query, offset, limit)));
	}
}
